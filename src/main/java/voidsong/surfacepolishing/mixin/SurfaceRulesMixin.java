package voidsong.surfacepolishing.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.spongepowered.asm.mixin.*;
import voidsong.surfacepolishing.IContextExtension;

import java.util.function.Predicate;

public abstract class SurfaceRulesMixin {

    @Mixin(SurfaceRules.BiomeConditionSource.class)
    public static final class BiomeConditionSource {
        @Final
        @Shadow
        Predicate<ResourceKey<Biome>> biomeNameTest;
        /**
         * @author VoidsongDragonfly
         * @reason Replacing Vanilla's use of an per-block biome cache and converting it to a pseudo-2D biome cache.
         * "Surface" biomes (above the min surface level) are cached in eight-block chunks by height to account for sky islands.
         * Depths biomes (cave biomes) are evaluated per-block
         */
        @Overwrite
        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            class PseudoLazyBiomeCondition extends SurfaceRules.LazyCondition {
                protected PseudoLazyBiomeCondition(SurfaceRules.Context pContext) {
                    super(pContext);
                }
                @Override
                public boolean test() {
                    // We check if we're in "surface" biome mode or depths biome mode; depths we return every block
                    if (context.blockY < context.getMinSurfaceLevel()) return pContext.biome.get().is(biomeNameTest);
                    // Otherwise we use normal lazy-cached behavior
                    return super.test();
                }

                @Override
                protected long getContextLastUpdate() {
                    return ((IContextExtension)(Object)context).surfacepolishing$getLastBiomeUpdate();
                }

                @Override
                protected boolean compute() {
                    return pContext.biome.get().is(biomeNameTest);
                }
            }

            return new PseudoLazyBiomeCondition(pContext);
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

            class PerformantStoneDepthCondition implements SurfaceRules.Condition {
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

            return new PerformantStoneDepthCondition();
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

            class PerformantVerticalGradientCondition implements SurfaceRules.Condition {
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

            return new PerformantVerticalGradientCondition();
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
            class PerformantWaterCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    return pContext.waterHeight == Integer.MIN_VALUE
                        || pContext.blockY + (addStoneDepth ? pContext.stoneDepthAbove : 0)
                        >= pContext.waterHeight
                        + offset
                        + pContext.surfaceDepth * surfaceDepthMultiplier;
                }
            }

            return new PerformantWaterCondition();
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
            class PerformantYCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    return pContext.blockY + (addStoneDepth ? pContext.stoneDepthAbove : 0)
                        >= anchor.resolveY(pContext.context)
                        + pContext.surfaceDepth * surfaceDepthMultiplier;
                }
            }

            return new PerformantYCondition();
        }
    }

    @Mixin(SurfaceRules.Context.class)
    @SuppressWarnings("unused")
    protected static final class Context implements IContextExtension {
        @Shadow long lastUpdateXZ;
        @Shadow @Final ChunkAccess chunk;
        @Shadow public int blockX;
        @Shadow public int blockZ;
        @Shadow public int blockY;
        @Unique
        @SuppressWarnings("all")
        int lastUpdatedHeightmap = Integer.MAX_VALUE;
        @Unique
        @SuppressWarnings("all")
        long lastUpdatedBiome = Long.MIN_VALUE;
        @Unique
        @SuppressWarnings("all")
        long lastUpdatedBiomeXZ = Long.MIN_VALUE;


        @Override
        public long surfacepolishing$getLastBiomeUpdate() {
            // If we've updated XZ we have changed column & need to update biome
            if (lastUpdateXZ > lastUpdatedBiomeXZ) {
                lastUpdatedBiomeXZ = lastUpdateXZ;
                lastUpdatedHeightmap = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, blockX, blockZ);
                lastUpdatedBiome++;
            // Otherwise, we only need to update biome every roughly eight blocks by heightmap; this should block out areas by surface/not surface
            } else {
                // We use 12 here because preliminary surface uses eight, and we want to ensure we're quite below it before we check again
                if (blockY < lastUpdatedHeightmap - 12) {
                    lastUpdatedHeightmap = blockY;
                    lastUpdatedBiome++;
                }
            }
            return lastUpdatedBiome;
        }
    }
}