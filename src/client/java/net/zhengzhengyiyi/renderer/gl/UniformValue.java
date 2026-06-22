package net.zhengzhengyiyi.renderer.gl;

import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.Std140Builder;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Minimal uniform value codec until full post-processing backport lands. */
@Environment(EnvType.CLIENT)
public interface UniformValue {
	Codec<UniformValue> CODEC = Codec.unit(new UniformValue() {
		@Override
		public void write(Std140Builder builder) {
		}

		@Override
		public void addSize(Std140SizeCalculator calculator) {
		}
	});

	void write(Std140Builder builder);

	void addSize(Std140SizeCalculator calculator);
}
