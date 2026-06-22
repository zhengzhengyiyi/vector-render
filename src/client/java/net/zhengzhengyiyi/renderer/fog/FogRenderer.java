package net.zhengzhengyiyi.renderer.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class FogRenderer implements AutoCloseable {
	private static final List<FogModifier> FOG_MODIFIERS = Lists.newArrayList(
		new LavaFogModifier(),
		new PowderSnowFogModifier(),
		new BlindnessEffectFogModifier(),
		new DarknessEffectFogModifier(),
		new WaterFogModifier(),
		new AtmosphericFogModifier()
	);
	private static final RenderTickCounter TICK_COUNTER = new RenderTickCounter(20.0F, 0L, millis -> millis);
	private static boolean fogEnabled = true;
	private FogData lastFogData = new FogData();
	private Vector4f lastFogColor = new Vector4f();

	public FogRenderer() {
	}

	@Override
	public void close() {
	}

	public static boolean toggleFog() {
		return fogEnabled = !fogEnabled;
	}

	public FogData getLastFogData() {
		return this.lastFogData;
	}

	public Vector4f getLastFogColor() {
		return this.lastFogColor;
	}

	public Vector4f applyFog(
		Camera camera,
		int viewDistanceChunks,
		float tickDelta,
		float skyDarkness,
		ClientWorld world,
		boolean thickFog
	) {
		BackgroundRenderer.render(camera, tickDelta, world, viewDistanceChunks, skyDarkness);
		BackgroundRenderer.applyFogColor();

		float viewDistanceBlocks = viewDistanceChunks * 16.0F;
		CameraSubmersionType submersion = camera.getSubmersionType();
		Entity entity = camera.getFocusedEntity();
		FogData fogData = new FogData();
		TICK_COUNTER.tickDelta = tickDelta;

		for (FogModifier modifier : FOG_MODIFIERS) {
			if (modifier.shouldApply(submersion, entity)) {
				modifier.applyStartEndModifier(fogData, camera, world, viewDistanceBlocks, TICK_COUNTER);
				break;
			}
		}

		float chunkFogWidth = MathHelper.clamp(viewDistanceBlocks / 10.0F, 4.0F, 64.0F);
		fogData.renderDistanceStart = viewDistanceBlocks - chunkFogWidth;
		fogData.renderDistanceEnd = viewDistanceBlocks;

		if (thickFog) {
			fogData.environmentalStart = viewDistanceBlocks * 0.05F;
			fogData.environmentalEnd = Math.min(viewDistanceBlocks, 192.0F) * 0.5F;
		}

		this.lastFogData = fogData;
		float[] fogColor = RenderSystem.getShaderFogColor();
		this.lastFogColor = new Vector4f(fogColor[0], fogColor[1], fogColor[2], 1.0F);
		applyLegacyFog(fogData);
		return this.lastFogColor;
	}

	private void applyLegacyFog(FogData fogData) {
		float start = Math.max(fogData.environmentalStart, fogData.renderDistanceStart);
		float end = Math.min(
			fogData.environmentalEnd > 0.0F ? fogData.environmentalEnd : Float.MAX_VALUE,
			fogData.renderDistanceEnd
		);
		RenderSystem.setShaderFogStart(start);
		RenderSystem.setShaderFogEnd(end);
		RenderSystem.setShaderFogShape(FogShape.CYLINDER);
	}
}
