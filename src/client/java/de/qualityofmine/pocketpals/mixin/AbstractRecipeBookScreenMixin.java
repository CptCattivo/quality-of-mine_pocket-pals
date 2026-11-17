package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
abstract class AbstractRecipeBookScreenMixin<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	private AbstractRecipeBookScreenMixin(final T menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title);
	}

	/**
	 * Das Inventar überschreibt die Prüfung über das Rezeptbuch und ruft die der Oberklasse nicht auf, deshalb sitzt der Eingriff hier.
	 * Die Gürtelplätze liegen unter dem Bild des Inventars. Ein Klick darauf zählt sonst als Klick daneben und wirft den Gegenstand weg.
	 */
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void pocketpals$beltIsInside(final double mouseX, final double mouseY, final int xo, final int yo, final CallbackInfoReturnable<Boolean> cir) {
		if (this.hoveredSlot != null && BeltFeature.isBeltSlot(this.hoveredSlot)) {
			cir.setReturnValue(false);
		}
	}
}
