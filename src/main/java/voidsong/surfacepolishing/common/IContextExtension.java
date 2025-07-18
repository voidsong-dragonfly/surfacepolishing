package voidsong.surfacepolishing.common;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Supplier;

public interface IContextExtension {
    Supplier<Holder<Biome>> surfacepolishing$getXZCachedBiome();
}
