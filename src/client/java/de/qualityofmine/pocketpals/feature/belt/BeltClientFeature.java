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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseEffects;

/**
 * Gürtel, Köcher und Trankgurt im normalen Spiel: Die 6 Plätze erscheinen über den Statusanzeigen, mit gedrückter Alt-Taste
 * wählt das Mausrad einen Platz. Beim Gürtel wirft eine Taste den Ball daraus, beim Trankgurt trinkt man, solange sie gehalten
 * wird, oder wirft einen Wurftrank. Beim Köcher verschießt der Bogen den gewählten Pfeil.
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
	/** Der Platz, aus dem getrunken wird, füllt sich von unten, bis die Flasche leer ist. */
	private static final int DRINK_PROGRESS = 0x80FFFFFF;

	private static KeyMapping useKey;
	/** Restliche Ticks, die der Name noch angezeigt wird. */
	private static int nameTicks;

	@Override
	public void initialize() {
		// Die Kennung stammt noch aus der Zeit, als die Taste nur Bälle warf. So bleibt eine geänderte Belegung erhalten.
		useKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.qualityofmine.throw_belt_ball", InputConstants.KEY_G, KEY_CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(BeltClientFeature::tick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, PocketPals.id("belt"), BeltClientFeature::extract);
	}

	/** Alt zusammen mit dem Mausrad ist für Gürtel, Köcher und Trankgurt reserviert, solange einer getragen wird. */
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

	/** Benutzen per Taste gibt es bei Gürtel und Trankgurt, aus dem Köcher schießt der Bogen. */
	private static boolean canUseContent(final Minecraft minecraft) {
		return isBeltVisible(minecraft) && minecraft.gui.screen() == null && BeltFeature.holderOf(minecraft.player).wornItem().canUseContent();
	}

	/** Alt zusammen mit Rechtsklick benutzt den gewählten Platz. Gibt true zurück, wenn der Klick dem Gürtel gehörte. */
	public static boolean handleUse() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!canUseContent(minecraft) || !isAltDown()) {
			return false;
		}
		useSelected(minecraft);
		return true;
	}

	/** Wird gerade aus dem Trankgurt getrunken? Dann geht der Spieler langsam und kann nicht sprinten, wie beim Trinken aus der Hand. */
	public static boolean isDrinking(final Player player) {
		return BeltFeature.holderOf(player).isDrinking();
	}

	/** Wie stark das Trinken bremst. Das bestimmt der Trank selbst, wie beim Trinken aus der Hand. */
	public static float drinkingSpeedMultiplier(final Player player) {
		BeltHolder holder = BeltFeature.holderOf(player);
		ItemStack stack = holder.contents().get(holder.drinkingSlot());
		return stack.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
	}

	private static void useSelected(final Minecraft minecraft) {
		BeltHolder holder = BeltFeature.holderOf(minecraft.player);
		ClientPlayNetworking.send(new UseBeltSlotPayload(holder.selected()));
		if (holder.wornItem() instanceof BandolierItem) {
			BandolierItem.startDrinking(minecraft.player, holder, holder.selected());
		}
	}

	/** Getrunken wird nur, solange die Taste oder Alt + Rechtsklick gehalten wird. */
	private static boolean isUseHeld(final Minecraft minecraft) {
		return minecraft.gui.screen() == null && (useKey.isDown() || isAltDown() && minecraft.options.keyUse.isDown());
	}

	private static void tick(final Minecraft minecraft) {
		if (nameTicks > 0) {
			nameTicks--;
		}
		while (useKey.consumeClick()) {
			if (canUseContent(minecraft)) {
				useSelected(minecraft);
			}
		}
		if (minecraft.player == null) {
			return;
		}
		BeltHolder holder = BeltFeature.holderOf(minecraft.player);
		if (holder.isDrinking() && !isUseHeld(minecraft)) {
			holder.stopDrinking();
			ClientPlayNetworking.send(StopDrinkingPayload.INSTANCE);
		}
		BandolierItem.tickDrinking(minecraft.player);
	}

	private static void extract(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isBeltVisible(minecraft)) {
			return;
		}
		BeltHolder holder = BeltFeature.holderOf(minecraft.player);
		BeltContentContainer content = new BeltContentContainer(minecraft.player);
		int selected = holder.selected();
		int left = (graphics.guiWidth() - BeltFeature.CONTENT_SIZE * SLOT_SIZE) / 2;
		int top = graphics.guiHeight() - BOTTOM_OFFSET;
		for (int i = 0; i < BeltFeature.CONTENT_SIZE; i++) {
			int x = left + i * SLOT_SIZE;
			graphics.fill(x, top, x + SLOT_SIZE, top + SLOT_SIZE, BACKGROUND);
			ItemStack stack = content.getItem(i);
			if (holder.isDrinking() && holder.drinkingSlot() == i && !stack.isEmpty()) {
				int height = Math.min(SLOT_SIZE, SLOT_SIZE * holder.drinkingTicks() / Math.max(1, stack.getUseDuration(minecraft.player)));
				graphics.fill(x, top + SLOT_SIZE - height, x + SLOT_SIZE, top + SLOT_SIZE, DRINK_PROGRESS);
			}
			graphics.outline(x, top, SLOT_SIZE, SLOT_SIZE, i == selected ? SELECTED_BORDER : BORDER);
			if (!stack.isEmpty()) {
				graphics.item(stack, x + 1, top + 1);
				// Die Anzahl, wichtig für Pfeile und Tränke
				graphics.itemDecorations(minecraft.font, stack, x + 1, top + 1);
			}
		}
		// Der Name des gewählten Tiers, Pfeils oder Tranks erscheint kurz über der Leiste, wenn man den Platz wechselt
		ItemStack current = content.getItem(selected);
		int alpha = Math.min(MAX_ALPHA, nameTicks * MAX_ALPHA / FADE_TICKS);
		if (!current.isEmpty() && alpha > 0) {
			graphics.centeredText(minecraft.font, current.getHoverName(), graphics.guiWidth() / 2, top - NAME_OFFSET, alpha << 24 | WHITE);
		}
	}
}
