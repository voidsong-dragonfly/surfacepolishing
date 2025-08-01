package voidsong.surfacepolishing.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import voidsong.surfacepolishing.PerformantSurfaceRules;

@SuppressWarnings("unused")
@Mixin(SurfaceRules.class)
public abstract class TestRulesMixin {
    @Mixin(SurfaceRules.ConditionSource.class)
    public static class ConditionSourceMixin {
        @ModifyArg(method = "bootstrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/SurfaceRules;register(Lnet/minecraft/core/Registry;Ljava/lang/String;Lnet/minecraft/util/KeyDispatchDataCodec;)Lcom/mojang/serialization/MapCodec;"), index = 2)
        private static KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> replaceConditions(Registry<Codec<? extends SurfaceRules.ConditionSource>> registry, String name, KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec) {
            return codec.equals(SurfaceRules.BiomeConditionSource.CODEC) ? PerformantSurfaceRules.PerformantBiomeConditionSource.CODEC :
                codec.equals(SurfaceRules.StoneDepthCheck.CODEC) ? PerformantSurfaceRules.PerformantStoneDepthCheck.CODEC :
                    codec.equals(SurfaceRules.WaterConditionSource.CODEC) ? PerformantSurfaceRules.PerformantWaterConditionSource.CODEC :
                        codec.equals(SurfaceRules.VerticalGradientConditionSource.CODEC) ? PerformantSurfaceRules.PerformantVerticalGradientConditionSource.CODEC :
                            codec.equals(SurfaceRules.YConditionSource.CODEC) ? PerformantSurfaceRules.PerformantYConditionSource.CODEC :
                                codec;
        }

        @Inject(method = "bootstrap", at = @At("HEAD"))
        private static void onBootstrap(Registry<MapCodec<? extends SurfaceRules.ConditionSource>> registry,
                                        CallbackInfoReturnable<Codec<SurfaceRules.ConditionSource>> cir) {
            System.out.println("aaaaaaaaaaaaaaaaaaaaaaaa");
            Minecraft.getInstance().level.getGameTime(); // this should give me a NPE
            SurfaceRules.register(registry, "biome", PerformantSurfaceRules.PerformantBiomeConditionSource.CODEC);
            SurfaceRules.register(registry, "vertical_gradient", PerformantSurfaceRules.PerformantVerticalGradientConditionSource.CODEC);
            SurfaceRules.register(registry, "y_above", PerformantSurfaceRules.PerformantYConditionSource.CODEC);
            SurfaceRules.register(registry, "water", PerformantSurfaceRules.PerformantWaterConditionSource.CODEC);
            SurfaceRules.register(registry, "stone_depth", PerformantSurfaceRules.PerformantStoneDepthCheck.CODEC);
        }
    }
}
