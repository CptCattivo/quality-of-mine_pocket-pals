package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import de.qualityofmine.pocketpals.feature.belt.BeltPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Die 6 Plätze von Gürtel, Köcher und Trankgurt in Truhe, Braustand, Ofen und allen anderen Menüs mit Spielerinventar. */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
	@Shadow
	@Final
	protected T menu;

	@Shadow
	protected Slot hoveredSlot;

	private AbstractContainerScreenMixin(final Component title) {
		super(title);
	}

	/**
	 * Der Server ergänzt die Plätze beim Öffnen des Menüs, der Client hier beim Bau des Bildschirms, bevor der Inhalt
	 * ankommt. Das Kreativ-Inventar ist ein reines Client-Menü und bekommt keine.
	 */
	@Inject(method = "<init>(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;II)V",
		at = @At("TAIL"))
	private void pocketpals$addBeltSlots(final CallbackInfo ci) {
		Object screen = this;
		if (!(screen instanceof CreativeModeInventoryScreen)) {
			BeltFeature.addContentSlots(this.menu);
		}
	}

	/** Der Hintergrund der Plätze, vor der Hervorhebung und den Items. Das Inventar zeichnet seinen eigenen. */
	@Inject(method = "extractContents", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void pocketpals$extractBeltPanel(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a, final CallbackInfo ci) {
		if (this.menu instanceof InventoryMenu) {
			return;
		}
		this.menu.slots.stream()
			.filter(slot -> BeltFeature.isBeltSlot(slot) && slot.isActive())
			.findFirst()
			.ifPresent(first -> BeltPanel.extractContentPanel(graphics, first.x, first.y));
	}

	/** Die Plätze liegen unter dem Bild des Menüs. Ein Klick darauf zählt sonst als Klick daneben und wirft den Gegenstand weg. */
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void pocketpals$beltIsInside(final double mouseX, final double mouseY, final int xo, final int yo, final CallbackInfoReturnable<Boolean> cir) {
		if (this.hoveredSlot != null && BeltFeature.isBeltSlot(this.hoveredSlot)) {
			cir.setReturnValue(false);
		}
	}
}
