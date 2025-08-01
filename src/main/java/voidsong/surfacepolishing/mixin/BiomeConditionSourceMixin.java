package voidsong.surfacepolishing.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import voidsong.surfacepolishing.SurfaceRuleRecords;

import java.util.function.Predicate;

//@Mixin(value = SurfaceRules.BiomeConditionSource.class, priority = 100)
public final class BiomeConditionSourceMixin {

    //@Final
    //@Shadow
    Predicate<ResourceKey<Biome>> biomeNameTest;

    //@Inject(method= "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;", at = @At("HEAD"), cancellable = true)
    public void apply(SurfaceRules.Context pContext, CallbackInfoReturnable<SurfaceRules.Condition> cir) {
        cir.setReturnValue(new SurfaceRuleRecords.PerformantBiomeCondition(pContext, biomeNameTest));
    }
}
