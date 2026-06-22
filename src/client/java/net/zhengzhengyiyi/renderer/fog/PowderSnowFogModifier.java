package net.zhengzhengyiyi.renderer.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PowderSnowFogModifier extends FogModifier {
	@Override
	public void applyStartEndModifier(FogData data, Camera camera, ClientWorld world, float viewDistanceBlocks, RenderTickCounter tickCounter) {
		if (camera.getFocusedEntity().isSpectator()) {
			data.environmentalStart = -8.0F;
			data.environmentalEnd = viewDistanceBlocks * 0.5F;
		} else {
			data.environmentalStart = 0.0F;
			data.environmentalEnd = 2.0F;
		}
		data.skyEnd = data.environmentalEnd;
		data.cloudEnd = data.environmentalEnd;
	}

	@Override
	public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
		return submersionType == CameraSubmersionType.POWDER_SNOW;
	}
}
