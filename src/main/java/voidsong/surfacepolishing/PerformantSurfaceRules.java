package voidsong.surfacepolishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class PerformantSurfaceRules {
    public static final class PerformantBiomeConditionSource implements SurfaceRules.ConditionSource {
        public static final KeyDispatchDataCodec<PerformantBiomeConditionSource> CODEC = KeyDispatchDataCodec.of(
            ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biome_is").xmap(PerformantBiomeConditionSource::new, (biomeConditionSource) -> biomeConditionSource.biomes)
        );
        private final List<ResourceKey<Biome>> biomes;
        final Predicate<ResourceKey<Biome>> biomeNameTest;

        PerformantBiomeConditionSource(List<ResourceKey<Biome>> biomes) {
            this.biomes = biomes;
            Set<ResourceKey<Biome>> var10001 = Set.copyOf(biomes);
            Objects.requireNonNull(var10001);
            this.biomeNameTest = var10001::contains;
        }

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            class PerformantBiomeCondition implements SurfaceRules.Condition {
                @Override
                public boolean test() {
                    return pContext.biome.get().is(biomeNameTest);
                }
            }
            return new PerformantBiomeCondition();
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object instanceof PerformantBiomeConditionSource performantBiomeConditionSource) {
                return this.biomes.equals(performantBiomeConditionSource.biomes);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return this.biomes.hashCode();
        }

        public String toString() {
            return "PerformantBiomeConditionSource[biomes=" + this.biomes + "]";
        }
    }

    public record PerformantStoneDepthCheck(int offset, boolean addSurfaceDepth, int secondaryDepthRange, CaveSurface surfaceType) implements SurfaceRules.ConditionSource {
        public static final KeyDispatchDataCodec<PerformantStoneDepthCheck> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                Codec.INT.fieldOf("offset").forGetter(PerformantStoneDepthCheck::offset),
                Codec.BOOL.fieldOf("add_surface_depth").forGetter(PerformantStoneDepthCheck::addSurfaceDepth),
                Codec.INT.fieldOf("secondary_depth_range").forGetter(PerformantStoneDepthCheck::secondaryDepthRange),
                CaveSurface.CODEC.fieldOf("surface_type").forGetter(PerformantStoneDepthCheck::surfaceType))
                .apply(instance, PerformantStoneDepthCheck::new)
        ));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

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
                        : (int) Mth.map(pContext.getSurfaceSecondary(), -1.0, 1.0, 0.0, secondaryDepthRange);
                    return i <= 1 + offset + j + k;
                }
            }

            return new StoneDepthCondition();
        }
    }

    public record PerformantVerticalGradientConditionSource(ResourceLocation randomName, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove) implements SurfaceRules.ConditionSource {
        public static final KeyDispatchDataCodec<PerformantVerticalGradientConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((instance) ->
             instance.group(
                 ResourceLocation.CODEC.fieldOf("random_name").forGetter(PerformantVerticalGradientConditionSource::randomName),
                 VerticalAnchor.CODEC.fieldOf("true_at_and_below").forGetter(PerformantVerticalGradientConditionSource::trueAtAndBelow),
                 VerticalAnchor.CODEC.fieldOf("false_at_and_above").forGetter(PerformantVerticalGradientConditionSource::falseAtAndAbove))
                 .apply(instance, PerformantVerticalGradientConditionSource::new)
        ));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
            // Copied Vanilla variables
            final int i = trueAtAndBelow.resolveY(pContext.context);
            final int j = falseAtAndAbove.resolveY(pContext.context);
            final PositionalRandomFactory positionalrandomfactory = pContext.randomState.getOrCreateRandomFactory(randomName);

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

    public record PerformantWaterConditionSource(int offset, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.ConditionSource {
        public static final KeyDispatchDataCodec<PerformantWaterConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                Codec.INT.fieldOf("offset").forGetter(PerformantWaterConditionSource::offset),
                Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(PerformantWaterConditionSource::surfaceDepthMultiplier),
                Codec.BOOL.fieldOf("add_stone_depth").forGetter(PerformantWaterConditionSource::addStoneDepth))
                .apply(instance, PerformantWaterConditionSource::new)
        ));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

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

    public record PerformantYConditionSource(VerticalAnchor anchor, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.ConditionSource {
        public static final KeyDispatchDataCodec<PerformantYConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                VerticalAnchor.CODEC.fieldOf("anchor").forGetter(PerformantYConditionSource::anchor),
                Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(PerformantYConditionSource::surfaceDepthMultiplier),
                Codec.BOOL.fieldOf("add_stone_depth").forGetter(PerformantYConditionSource::addStoneDepth))
                .apply(instance, PerformantYConditionSource::new)
        ));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

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
