package net.zhengzhengyiyi.renderer.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class FogModifier {
	public abstract void applyStartEndModifier(FogData data, Camera camera, ClientWorld world, float viewDistanceBlocks, RenderTickCounter tickCounter);

	public boolean isColorSource() {
		return false;
	}

	public int getFogColor(ClientWorld world, Camera camera, int viewDistance, float skyDarkness) {
		return -1;
	}

	public boolean isDarknessModifier() {
		return false;
	}

	public float applyDarknessModifier(LivingEntity entity, float darkness, float tickDelta) {
		return darkness;
	}

	public abstract boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity);
}
