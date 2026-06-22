package net.zhengzhengyiyi.renderer.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DarknessEffectFogModifier extends FogModifier {
	@Override
	public boolean isDarknessModifier() {
		return true;
	}

	@Override
	public float applyDarknessModifier(LivingEntity entity, float darkness, float tickDelta) {
		var effect = entity.getStatusEffect(StatusEffects.DARKNESS);
		if (effect == null) {
			return darkness;
		}
		if (effect.isDurationBelow(19)) {
			return 1.0F - effect.getDuration() / 20.0F;
		}
		return 0.0F;
	}

	@Override
	public void applyStartEndModifier(FogData data, Camera camera, ClientWorld world, float viewDistanceBlocks, RenderTickCounter tickCounter) {
	}

	@Override
	public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
		return cameraEntity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.DARKNESS);
	}
}
