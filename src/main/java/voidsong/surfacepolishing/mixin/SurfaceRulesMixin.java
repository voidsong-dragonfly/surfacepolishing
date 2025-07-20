package voidsong.surfacepolishing.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


public class SurfaceRulesMixin {
    @Mixin(value = SurfaceRules.StoneDepthCheck.class, priority = 100)
    public record StoneDepthCheck(int offset, boolean addSurfaceDepth, int secondaryDepthRange, CaveSurface surfaceType) {
        @Inject(method= "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;", at = @At("HEAD"), cancellable = true)
        public void apply(SurfaceRules.Context pContext, CallbackInfoReturnable<SurfaceRules.Condition> cir) {
            cir.setReturnValue(new PerformantStoneDepthCheck(pContext, offset, addSurfaceDepth, secondaryDepthRange, this.surfaceType == CaveSurface.CEILING));
        }
    }

    record PerformantStoneDepthCheck(SurfaceRules.Context pContext, int offset, boolean addSurfaceDepth, int secondaryDepthRange, boolean surfaceCeiling) implements SurfaceRules.Condition {
        @Override
        public boolean test() {
            int i = surfaceCeiling ? pContext.stoneDepthBelow : pContext.stoneDepthAbove;
            int j = addSurfaceDepth ? pContext.surfaceDepth : 0;
            int k = secondaryDepthRange == 0
                ? 0
                : (int)Mth.map(pContext.getSurfaceSecondary(), -1.0, 1.0, 0.0, secondaryDepthRange);
            return i <= 1 + offset + j + k;
        }
    }

    @Mixin(value = SurfaceRules.VerticalGradientConditionSource.class, priority = 100)
    public record VerticalGradientConditionSource(ResourceLocation randomName, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove) {
        @Inject(method= "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;", at = @At("HEAD"), cancellable = true)
        public void apply(SurfaceRules.Context pContext, CallbackInfoReturnable<SurfaceRules.Condition> cir) {
            cir.setReturnValue(new PerformantVerticalGradientCondition(pContext, pContext.randomState.getOrCreateRandomFactory(this.randomName()), trueAtAndBelow.resolveY(pContext.context), falseAtAndAbove.resolveY(pContext.context)));
        }
    }

    record PerformantVerticalGradientCondition(SurfaceRules.Context pContext, PositionalRandomFactory randomFactory, int trueAtAndBelow, int falseAtAndAbove) implements SurfaceRules.Condition {
        @Override
        public boolean test() {
            int y = pContext.blockY;
            if (y <= trueAtAndBelow) {
                return true;
            } else if (y >= falseAtAndAbove) {
                return false;
            } else {
                double d0 = Mth.map(y, trueAtAndBelow, falseAtAndAbove, 1.0, 0.0);
                RandomSource randomSource = randomFactory.at(pContext.blockX, y, pContext.blockZ);
                return (double)randomSource.nextFloat() < d0;
            }
        }
    }

    @Mixin(value = SurfaceRules.WaterConditionSource.class, priority = 100)
    public record WaterConditionSource(int offset, int surfaceDepthMultiplier, boolean addStoneDepth) {
        @Inject(method= "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;", at = @At("HEAD"), cancellable = true)
        public void apply(SurfaceRules.Context pContext, CallbackInfoReturnable<SurfaceRules.Condition> cir) {
            cir.setReturnValue(new PerformantWaterConditionSource(pContext, offset, surfaceDepthMultiplier, addStoneDepth));
        }
    }

    record PerformantWaterConditionSource(SurfaceRules.Context pContext, int offset, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.Condition {
        @Override
        public boolean test() {
            return pContext.waterHeight == Integer.MIN_VALUE
                || pContext.blockY + (addStoneDepth ? pContext.stoneDepthAbove : 0)
                >= pContext.waterHeight
                + offset
                + pContext.surfaceDepth * surfaceDepthMultiplier;
        }
    }

    @Mixin(value = SurfaceRules.YConditionSource.class, priority = 100)
    public record YConditionSource(VerticalAnchor anchor, int surfaceDepthMultiplier, boolean addStoneDepth) {
        @Inject(method= "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;", at = @At("HEAD"), cancellable = true)
        public void apply(SurfaceRules.Context pContext, CallbackInfoReturnable<SurfaceRules.Condition> cir) {
            cir.setReturnValue(new PerformantYCondition(pContext, anchor, surfaceDepthMultiplier, addStoneDepth));
        }
    }

    record PerformantYCondition(SurfaceRules.Context pContext, VerticalAnchor anchor, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.Condition {
        @Override
        public boolean test() {
            return pContext.blockY + (addStoneDepth ? pContext.stoneDepthAbove : 0)
                >= anchor.resolveY(pContext.context)
                + pContext.surfaceDepth * surfaceDepthMultiplier;
        }
    }
}
