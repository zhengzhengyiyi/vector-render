package net.zhengzhengyiyi.client.render;

public final class ColorHelperCompat {
	private ColorHelperCompat() {
	}

	public static float getRedFloat(int color) {
		return (color >> 16 & 0xFF) / 255.0F;
	}

	public static float getGreenFloat(int color) {
		return (color >> 8 & 0xFF) / 255.0F;
	}

	public static float getBlueFloat(int color) {
		return (color & 0xFF) / 255.0F;
	}

	public static float getAlphaFloat(int color) {
		return (color >> 24 & 0xFF) / 255.0F;
	}
}
