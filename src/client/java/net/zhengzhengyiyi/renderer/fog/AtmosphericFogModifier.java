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
public class AtmosphericFogModifier extends FogModifier {
	@Override
	public void applyStartEndModifier(FogData data, Camera camera, ClientWorld world, float viewDistanceBlocks, RenderTickCounter tickCounter) {
		data.environmentalStart = 0.0F;
		data.environmentalEnd = viewDistanceBlocks;
		data.skyEnd = viewDistanceBlocks;
		data.cloudEnd = viewDistanceBlocks;
	}

	@Override
	public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
		return submersionType == null || submersionType == CameraSubmersionType.NONE;
	}
}
