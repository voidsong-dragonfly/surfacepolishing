package voidsong.surfacepolishing.common;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

import javax.annotation.Nonnull;

public class PerformantBiomeConditionSource implements SurfaceRules.ConditionSource {
    public static final KeyDispatchDataCodec<PerformantBiomeConditionSource> CODEC = KeyDispatchDataCodec.of(
        RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biome_is").xmap(PerformantBiomeConditionSource::makeBiomeConditionSource, biomeSource -> biomeSource.biomeSet)
    );
    public final HolderSet<Biome> biomeSet;

    public PerformantBiomeConditionSource(HolderSet<Biome> biomes) {
        this.biomeSet = biomes;
    }

    @Override
    @Nonnull
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    public SurfaceRules.Condition apply(final SurfaceRules.Context pContext) {
        class BiomeCondition implements SurfaceRules.Condition {

            @Override
            public boolean test() {
                return (pContext.getMinSurfaceLevel()-8) > pContext.blockY ? biomeSet.contains(pContext.biome.get()) : biomeSet.contains(((IContextExtension)(Object)pContext).surfacepolishing$getXZCachedBiome().get());
            }
        }

        return new BiomeCondition();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else {
            return other instanceof PerformantBiomeConditionSource source && this.biomeSet.equals(source.biomeSet);
        }
    }

    @Override
    public int hashCode() {
        return this.biomeSet.hashCode();
    }

    private static PerformantBiomeConditionSource makeBiomeConditionSource(HolderSet<Biome> biomes) {
        return new PerformantBiomeConditionSource(biomes);
    }

    @Override
    @Nonnull
    public String toString() {
        return "PerformantBiomeConditionSource[biomes=" + this.biomeSet + "]";
    }
}
