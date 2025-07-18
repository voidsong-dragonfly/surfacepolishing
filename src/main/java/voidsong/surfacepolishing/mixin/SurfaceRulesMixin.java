package voidsong.surfacepolishing.mixin;


import com.google.common.base.Suppliers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidsong.surfacepolishing.common.IContextExtension;
import voidsong.surfacepolishing.common.PerformantBiomeConditionSource;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
@Mixin(SurfaceRules.class)
public abstract class SurfaceRulesMixin {

    @Mixin(SurfaceRules.ConditionSource.class)
    public interface ConditionSource extends Function<SurfaceRules.Context, SurfaceRules.Condition> {
        /**
         * @author VoidsongDragonfly
         * @reason Replacing Vanilla's use of {@link SurfaceRules.LazyYCondition LazyYCondition} that causes performance detriments due to unused caching behavior, and the use of a {@link net.minecraft.core.HolderSet HolderSet<Biome>} for storage & comparison
         */
        @ModifyArg(method = "bootstrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/SurfaceRules;register(Lnet/minecraft/core/Registry;Ljava/lang/String;Lnet/minecraft/util/KeyDispatchDataCodec;)Lcom/mojang/serialization/MapCodec;"), index = 2)
        private static KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> replaceBiomeRule(KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec) {
            return codec.equals(SurfaceRules.BiomeConditionSource.CODEC) ? PerformantBiomeConditionSource.CODEC : codec;
        }
    }

    @Mixin(SurfaceRules.Context.class)
    protected static final class Context implements IContextExtension {
        @Unique
        @SuppressWarnings("all")
        Supplier<Holder<Biome>> cachedXZBiome;
        @Unique
        @SuppressWarnings("all")
        final BlockPos.MutableBlockPos xzPos = new BlockPos.MutableBlockPos();

        @Inject(method="updateXZ", at=@At("RETURN"))
        private void updateXZ(int blockX, int blockZ, CallbackInfo ci) {
            SurfaceRules.Context self = (SurfaceRules.Context) (Object) this;
            this.cachedXZBiome = Suppliers.memoize(() -> self.biomeGetter.apply(xzPos.set(blockX, 1000000, blockZ)));
        }

        @Override
        public Supplier<Holder<Biome>> surfacepolishing$getXZCachedBiome() {
            return cachedXZBiome;
        }
    }

    @Mixin(SurfaceRules.StoneDepthCheck.class)
    public record StoneDepthCheck(int offset, boolean addSurfaceDepth, int secondaryDepthRange, CaveSurface surfaceType) {
        /**
         * @author VoidsongDragonfly
         * @reason Replacing Vanilla's use of {@link SurfaceRules.LazyYCondition LazyYCondition} that causes performance detriments due to unused caching behavior
         */
        @Overwrite
        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            // Copied Vanilla variables
            final boolean flag = this.surfaceType == CaveSurface.CEILING;

            class StoneDepthCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    int i = flag ? pContext.stoneDepthBelow : pContext.stoneDepthAbove;
                    int j = addSurfaceDepth ? pContext.surfaceDepth : 0;
                    int k = secondaryDepthRange == 0
                        ? 0
                        : (int)Mth.map(pContext.getSurfaceSecondary(), -1.0, 1.0, 0.0, secondaryDepthRange);
                    return i <= 1 + offset + j + k;
                }
            }

            return new StoneDepthCondition();
        }
    }

    @Mixin(SurfaceRules.VerticalGradientConditionSource.class)
    public record VerticalGradientConditionSource(ResourceLocation randomName, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove) {
        /**
         * @author VoidsongDragonfly
         * @reason Replacing Vanilla's use of {@link SurfaceRules.LazyYCondition LazyYCondition} that causes performance detriments due to unused caching behavior
         */
        @Overwrite
        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            // Copied Vanilla variables
            final int i = this.trueAtAndBelow().resolveY(pContext.context);
            final int j = this.falseAtAndAbove().resolveY(pContext.context);
            final PositionalRandomFactory positionalrandomfactory = pContext.randomState.getOrCreateRandomFactory(this.randomName());

            class VerticalGradientCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    int k = pContext.blockY;
                    if (k <= i) {
                        return true;
                    } else if (k >= j) {
                        return false;
                    } else {
                        double d0 = Mth.map(k, i, j, 1.0, 0.0);
                        RandomSource randomsource = positionalrandomfactory.at(pContext.blockX, k, pContext.blockZ);
                        return (double)randomsource.nextFloat() < d0;
                    }
                }
            }

            return new VerticalGradientCondition();
        }
    }

    @Mixin(SurfaceRules.WaterConditionSource.class)
    public record WaterConditionSource(int offset, int surfaceDepthMultiplier, boolean addStoneDepth) {
        /**
         * @author VoidsongDragonfly
         * @reason Replacing Vanilla's use of {@link SurfaceRules.LazyYCondition LazyYCondition} that causes performance detriments due to unused caching behavior
         */
        @Overwrite
        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            class WaterCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    return pContext.waterHeight == Integer.MIN_VALUE
                        || pContext.blockY + (addStoneDepth ? pContext.stoneDepthAbove : 0)
                        >= pContext.waterHeight
                        + offset
                        + pContext.surfaceDepth * surfaceDepthMultiplier;
                }
            }

            return new WaterCondition();
        }

    }

    @Mixin(SurfaceRules.YConditionSource.class)
    public record YConditionSource(VerticalAnchor anchor, int surfaceDepthMultiplier, boolean addStoneDepth) {
        /**
         * @author VoidsongDragonfly
         * @reason Replacing Vanilla's use of {@link SurfaceRules.LazyYCondition LazyYCondition} that causes performance detriments due to unused caching behavior
         */
        @Overwrite
        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            class YCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    return pContext.blockY + (addStoneDepth ? pContext.stoneDepthAbove : 0)
                        >= anchor.resolveY(pContext.context)
                        + pContext.surfaceDepth * surfaceDepthMultiplier;
                }
            }

            return new YCondition();
        }
    }
}
