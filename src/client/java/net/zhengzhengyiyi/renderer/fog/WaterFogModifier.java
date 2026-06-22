package net.zhengzhengyiyi.renderer.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BiomeTags;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WaterFogModifier extends FogModifier {
	@Override
	public void applyStartEndModifier(FogData data, Camera camera, ClientWorld world, float viewDistanceBlocks, RenderTickCounter tickCounter) {
		data.environmentalStart = -8.0F;
		data.environmentalEnd = 96.0F;
		if (camera.getFocusedEntity() instanceof ClientPlayerEntity player) {
			data.environmentalEnd *= Math.max(0.25F, player.getUnderwaterVisibility());
			if (player.getWorld().getBiome(player.getBlockPos()).isIn(BiomeTags.HAS_CLOSER_WATER_FOG)) {
				data.environmentalEnd *= 0.85F;
			}
		}
		if (data.environmentalEnd > viewDistanceBlocks) {
			data.environmentalEnd = viewDistanceBlocks;
		}
		data.skyEnd = data.environmentalEnd;
		data.cloudEnd = data.environmentalEnd;
	}

	@Override
	public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
		return submersionType == CameraSubmersionType.WATER;
	}
}
