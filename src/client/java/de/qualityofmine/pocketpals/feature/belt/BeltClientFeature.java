package de.qualityofmine.pocketpals.feature.belt;

import com.mojang.blaze3d.platform.InputConstants;
import de.qualityofmine.pocketpals.Feature;
import de.qualityofmine.pocketpals.PocketPals;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Gürtel und Köcher im normalen Spiel: Die 6 Plätze erscheinen über den Statusanzeigen, mit gedrückter Alt-Taste wählt das
 * Mausrad einen Platz. Beim Gürtel wirft eine Taste den Ball daraus, beim Köcher verschießt der Bogen den gewählten Pfeil.
 */
public final class BeltClientFeature implements Feature {
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(PocketPals.id("pocketpals"));
	private static final int SLOT_SIZE = 18;
	/** Abstand der Leiste zum unteren Bildschirmrand: über den Anzeigen für Rüstung und Luft. */
	private static final int BOTTOM_OFFSET = 74;
	/** Abstand der Textzeile mit dem Namen über der Leiste. */
	private static final int NAME_OFFSET = 12;
	private static final int WHITE = 0xFFFFFF;
	/** Der Name bleibt so lange stehen wie der Name in der Item-Leiste, und blendet in den letzten Ticks aus. */
	private static final int NAME_TICKS = 40;
	private static final int FADE_TICKS = 10;
	private static final int MAX_ALPHA = 255;
	private static final int BACKGROUND = 0x80000000;
	private static final int BORDER = 0xFF555555;
	private static final int SELECTED_BORDER = 0xFFFFFFFF;

	private static KeyMapping throwKey;
	/** Restliche Ticks, die der Name noch angezeigt wird. */
	private static int nameTicks;

	@Override
	public void initialize() {
		throwKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.qualityofmine.throw_belt_ball", InputConstants.KEY_G, KEY_CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(BeltClientFeature::throwSelected);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, PocketPals.id("belt"), BeltClientFeature::extract);
	}

	/** Alt zusammen mit dem Mausrad ist für den Gürtel reserviert, solange einer getragen wird. */
	public static boolean handleScroll(final double scroll) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isBeltVisible(minecraft) || minecraft.gui.screen() != null || scroll == 0.0 || !isAltDown()) {
			return false;
		}
		BeltHolder holder = BeltFeature.holderOf(minecraft.player);
		holder.select(ScrollWheelHandler.getNextScrollWheelSelection(scroll, holder.selected(), BeltFeature.CONTENT_SIZE));
		// Der Server braucht die Auswahl, damit der Bogen den richtigen Pfeil verschießt
		ClientPlayNetworking.send(new SelectBeltSlotPayload(holder.selected()));
		nameTicks = NAME_TICKS;
		return true;
	}

	private static boolean isAltDown() {
		return InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT);
	}

	private static boolean isBeltVisible(final Minecraft minecraft) {
		Player player = minecraft.player;
		return player != null && !player.isSpectator() && BeltFeature.holderOf(player).wornItem() != null;
	}

	/** Werfen per Taste gibt es nur beim Gürtel, aus dem Köcher schießt der Bogen. */
	private static boolean isBeltWorn(final Minecraft minecraft) {
		return isBeltVisible(minecraft) && BeltFeature.holderOf(minecraft.player).wornItem() instanceof BeltItem;
	}

	/** Alt zusammen mit Rechtsklick wirft sofort den gewählten Ball. Gibt true zurück, wenn der Klick dem Gürtel gehörte. */
	public static boolean handleUse() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isBeltWorn(minecraft) || minecraft.gui.screen() != null || !isAltDown()) {
			return false;
		}
		sendThrow(minecraft);
		return true;
	}

	private static void sendThrow(final Minecraft minecraft) {
		ClientPlayNetworking.send(new ThrowBeltBallPayload(BeltFeature.holderOf(minecraft.player).selected()));
	}

	private static void throwSelected(final Minecraft minecraft) {
		if (nameTicks > 0) {
			nameTicks--;
		}
		while (throwKey.consumeClick()) {
			if (isBeltWorn(minecraft) && minecraft.gui.screen() == null) {
				sendThrow(minecraft);
			}
		}
	}

	private static void extract(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isBeltVisible(minecraft)) {
			return;
		}
		BeltContentContainer content = new BeltContentContainer(minecraft.player);
		int selected = BeltFeature.holderOf(minecraft.player).selected();
		int left = (graphics.guiWidth() - BeltFeature.CONTENT_SIZE * SLOT_SIZE) / 2;
		int top = graphics.guiHeight() - BOTTOM_OFFSET;
		for (int i = 0; i < BeltFeature.CONTENT_SIZE; i++) {
			int x = left + i * SLOT_SIZE;
			graphics.fill(x, top, x + SLOT_SIZE, top + SLOT_SIZE, BACKGROUND);
			graphics.outline(x, top, SLOT_SIZE, SLOT_SIZE, i == selected ? SELECTED_BORDER : BORDER);
			ItemStack stack = content.getItem(i);
			if (!stack.isEmpty()) {
				graphics.item(stack, x + 1, top + 1);
				// Die Anzahl, wichtig für Pfeile im Köcher
				graphics.itemDecorations(minecraft.font, stack, x + 1, top + 1);
			}
		}
		// Der Name des gewählten Tiers oder Pfeils erscheint kurz über der Leiste, wenn man den Platz wechselt
		ItemStack current = content.getItem(selected);
		int alpha = Math.min(MAX_ALPHA, nameTicks * MAX_ALPHA / FADE_TICKS);
		if (!current.isEmpty() && alpha > 0) {
			graphics.centeredText(minecraft.font, current.getHoverName(), graphics.guiWidth() / 2, top - NAME_OFFSET, alpha << 24 | WHITE);
		}
	}
}
