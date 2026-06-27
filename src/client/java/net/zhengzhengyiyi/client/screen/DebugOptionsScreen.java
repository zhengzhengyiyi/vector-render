package net.zhengzhengyiyi.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.zhengzhengyiyi.client.debug.DebugHudEntries;
import net.zhengzhengyiyi.client.debug.DebugHudEntryVisibility;
import net.zhengzhengyiyi.client.debug.DebugHudProfile;
import net.zhengzhengyiyi.client.mixin.accessor.MinecraftClientAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified debug options screen for configuring debug HUD entries.
 * Ported from 1.21.11. Added scrolling minimal implementation.
 */
public class DebugOptionsScreen extends Screen {
	private static final Text ALWAYS_ON_TEXT = Text.translatable("debug.entry.always_on");
	private static final Text IN_F3_TEXT = Text.translatable("debug.entry.in_f3");
	private static final Text NEVER_TEXT = Text.translatable("debug.entry.never");
	
	public final Screen parent;
	private DebugHudProfile profile;
	private List<EntryWidget> entryWidgets = new ArrayList<>();
	
	// Scrolling state variables
	private double scrollAmount = 0;
	private int maxScroll = 0;

	public DebugOptionsScreen(Screen parent) {
		super(Text.translatable("debug.options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		MinecraftClient client = MinecraftClient.getInstance();
		this.profile = ((MinecraftClientAccessor) client).getDebugHudEntryList();
		
		// Clear existing widgets on resize/init re-runs
		this.entryWidgets.clear();
		this.clearChildren();
		
		int y = 0; // Relative starting position inside the scroll view
		for (Identifier entryId : DebugHudEntries.getEntries().keySet()) {
			EntryWidget widget = new EntryWidget(entryId, y);
			this.entryWidgets.add(widget);
			
			// Buttons are added to child list for interaction, but we manually control positioning during mouse actions
			this.addDrawableChild(widget.alwaysOnButton);
			this.addDrawableChild(widget.inF3Button);
			this.addDrawableChild(widget.neverButton);
			y += 25;
		}
		
		// Calculate maximum allowed scrolling bounds based on content height vs window height
		int contentHeight = y;
		int visibleHeight = this.height - 80; // Margin room for title and Done button
		this.maxScroll = Math.max(0, contentHeight - visibleHeight);
		
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
			.position(this.width / 2 - 100, this.height - 30)
			.size(200, 20)
			.build());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// Update scroll position using mouse wheel delta
		this.scrollAmount = Math.max(0, Math.min(this.maxScroll, this.scrollAmount - (verticalAmount * 12)));
		return true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Draw semi-transparent dark background
		context.fill(0, 0, this.width, this.height, 0x80000000);
		
		// Apply Minecraft scissor test to restrict rendering only within our safe center bounds
		context.enableScissor(0, 40, this.width, this.height - 40);
		
		// Update positions dynamically relative to current scroll delta before rendering
		for (EntryWidget widget : this.entryWidgets) {
			widget.updateScrollPosition((int) this.scrollAmount);
			widget.render(context, mouseX, mouseY, delta);
		}
		
		// Disable scissors to allow out-of-bounds GUI graphics like the global buttons
		context.disableScissor();
		
		// Draw persistent title text header over content layer
		context.drawTextWithShadow(this.textRenderer, this.title, this.width / 2 - this.textRenderer.getWidth(this.title) / 2, 20, 0xFFFFFF);
		
		super.render(context, mouseX, mouseY, delta);
	}

	private class EntryWidget {
		private final Identifier entryId;
		private final int originalY;
		private int currentY;
		private final ButtonWidget alwaysOnButton;
		private final ButtonWidget inF3Button;
		private final ButtonWidget neverButton;

		public EntryWidget(Identifier entryId, int relativeY) {
			this.entryId = entryId;
			// Start with basic 40px top-offset spacing layout baseline
			this.originalY = 40 + relativeY;
			this.currentY = this.originalY;
			
			this.alwaysOnButton = ButtonWidget.builder(ALWAYS_ON_TEXT, button -> setVisibility(DebugHudEntryVisibility.ALWAYS_ON))
				.position(10, currentY)
				.size(60, 20)
				.build();
			
			this.inF3Button = ButtonWidget.builder(IN_F3_TEXT, button -> setVisibility(DebugHudEntryVisibility.IN_OVERLAY))
				.position(75, currentY)
				.size(60, 20)
				.build();
			
			this.neverButton = ButtonWidget.builder(NEVER_TEXT, button -> setVisibility(DebugHudEntryVisibility.NEVER))
				.position(140, currentY)
				.size(60, 20)
				.build();
			
			initButtons();
		}

		public void updateScrollPosition(int scrollOffset) {
			this.currentY = this.originalY - scrollOffset;
			
			// Adjust widget layout positions dynamically to stay sync'd with scroll translation
			this.alwaysOnButton.setY(this.currentY);
			this.inF3Button.setY(this.currentY);
			this.neverButton.setY(this.currentY);
			
			// Deactivate clicking detection completely if button scrolls cleanly out of sight
			boolean visible = this.currentY >= 35 && this.currentY <= (height - 55);
			this.alwaysOnButton.visible = visible;
			this.inF3Button.visible = visible;
			this.neverButton.visible = visible;
		}

		private void initButtons() {
			DebugHudEntryVisibility visibility = profile.getVisibility(entryId);
			alwaysOnButton.active = visibility != DebugHudEntryVisibility.ALWAYS_ON;
			inF3Button.active = visibility != DebugHudEntryVisibility.IN_OVERLAY;
			neverButton.active = visibility != DebugHudEntryVisibility.NEVER;
		}

		private void setVisibility(DebugHudEntryVisibility visibility) {
			profile.setEntryVisibility(entryId, visibility);
			for (EntryWidget widget : entryWidgets) {
				widget.initButtons();
			}
		}

		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			// Skip processing text output entirely if component position falls offscreen bounds
			if (currentY < 30 || currentY > height - 50) return;
			
			Text entryName = Text.translatable("debug.entry." + entryId.getNamespace() + "." + entryId.getPath());
			context.drawTextWithShadow(textRenderer, entryName, 210, currentY + 6, 0xFFFFFF);
		}
	}
}
