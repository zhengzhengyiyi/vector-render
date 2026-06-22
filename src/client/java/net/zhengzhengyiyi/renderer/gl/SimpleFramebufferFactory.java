package net.zhengzhengyiyi.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.zhengzhengyiyi.renderer.util.ClosableFactory;

@Environment(EnvType.CLIENT)
public record SimpleFramebufferFactory(int width, int height, boolean useDepth, int clearColor) implements ClosableFactory<Framebuffer> {
	@Override
	public Framebuffer create() {
		return new SimpleFramebuffer(this.width, this.height, this.useDepth, false);
	}

	@Override
	public void prepare(Framebuffer framebuffer) {
		framebuffer.beginWrite(true);
		if (this.useDepth) {
			RenderSystem.clear(16640, false);
		} else {
			RenderSystem.clear(16384, false);
		}
	}

	@Override
	public void close(Framebuffer framebuffer) {
		framebuffer.delete();
	}

	@Override
	public boolean equals(ClosableFactory<?> factory) {
		if (!(factory instanceof SimpleFramebufferFactory other)) {
			return false;
		}
		return this.width == other.width && this.height == other.height && this.useDepth == other.useDepth;
	}
}
