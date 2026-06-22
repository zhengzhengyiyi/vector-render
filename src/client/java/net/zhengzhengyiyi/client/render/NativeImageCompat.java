package net.zhengzhengyiyi.client.render;

import net.minecraft.client.texture.NativeImage;
import net.zhengzhengyiyi.client.mixin.accessor.NativeImageAccessor;

public final class NativeImageCompat {
	private NativeImageCompat() {
	}

	public static long getPointer(NativeImage image) {
		return ((NativeImageAccessor) (Object) image).renderer$getPointer();
	}
}
