package voidsong.surfacepolishing;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.function.Predicate;

public class SurfaceRuleRecords {
    public record PerformantBiomeCondition(SurfaceRules.Context pContext, Predicate<ResourceKey<Biome>> biomeNameTest) implements SurfaceRules.Condition {
        @Override
        public boolean test() {
            return pContext.biome.get().is(biomeNameTest);
        }
    }
}
