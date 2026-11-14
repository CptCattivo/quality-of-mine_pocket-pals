package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Die Hintergründe für den Gürtel im Inventar: der Platz neben der Rüstung und die 6 Plätze darunter. */
public final class BeltPanel {
	private static final Identifier SLOT_TEXTURE = PocketPals.id("textures/gui/belt_slot.png");
	private static final Identifier PANEL_TEXTURE = PocketPals.id("textures/gui/belt_panel.png");
	private static final int SLOT_SIZE = 18;
	private static final int PANEL_X = 4;
	private static final int PANEL_Y = 163;
	private static final int PANEL_WIDTH = 116;
	private static final int PANEL_HEIGHT = 26;

	private BeltPanel() {
	}

	public static void extract(final GuiGraphicsExtractor graphics, final int leftPos, final int topPos) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, leftPos + BeltFeature.EQUIP_X - 1, topPos + BeltFeature.EQUIP_Y - 1,
			0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
		Player player = Minecraft.getInstance().player;
		// Die 6 Plätze gibt es nur mit angelegtem Gürtel
		if (player != null && !BeltFeature.beltOf(player).isEmpty() && !player.hasInfiniteMaterials()) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, leftPos + PANEL_X, topPos + PANEL_Y,
				0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
		}
	}
}
