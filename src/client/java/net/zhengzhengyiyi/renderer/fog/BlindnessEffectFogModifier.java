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
public class BlindnessEffectFogModifier extends FogModifier {
	@Override
	public void applyStartEndModifier(FogData data, Camera camera, ClientWorld world, float viewDistanceBlocks, RenderTickCounter tickCounter) {
		data.environmentalStart = viewDistanceBlocks * 0.05F;
		data.environmentalEnd = Math.min(viewDistanceBlocks, 192.0F) * 0.5F;
		data.skyEnd = data.environmentalEnd;
		data.cloudEnd = data.environmentalEnd;
	}

	@Override
	public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
		return cameraEntity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.BLINDNESS);
	}
}
