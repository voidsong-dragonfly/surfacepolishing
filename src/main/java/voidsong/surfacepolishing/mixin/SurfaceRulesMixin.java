package voidsong.surfacepolishing.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Predicate;

@SuppressWarnings("unused")
@Mixin(SurfaceRules.class)
public abstract class SurfaceRulesMixin {

    @Mixin(SurfaceRules.BiomeConditionSource.class)
    public static final class BiomeConditionSource{
        @Final
        @Shadow
        Predicate<ResourceKey<Biome>> biomeNameTest;
        /**
         * @author VoidsongDragonfly
         * @reason Replacing Vanilla's use of {@link SurfaceRules.LazyYCondition LazyYCondition} that causes performance detriments due to unused caching behavior
         */
        @Overwrite
        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            class BiomeCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    return pContext.biome.get().is(biomeNameTest);
                }
            }

            return new BiomeCondition();
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
