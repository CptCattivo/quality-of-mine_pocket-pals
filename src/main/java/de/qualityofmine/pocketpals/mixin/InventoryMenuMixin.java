package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin extends AbstractCraftingMenu {
	private InventoryMenuMixin(final MenuType<?> menuType, final int containerId, final int width, final int height) {
		super(menuType, containerId, width, height);
	}

	/** Die Slots kommen ans Ende, damit die festen Slot-Nummern des Spiels gleich bleiben. */
	@Inject(method = "<init>", at = @At("TAIL"))
	private void pocketpals$addBeltSlots(final Inventory inventory, final boolean active, final Player owner, final CallbackInfo ci) {
		BeltFeature.createSlots(owner).forEach(this::addSlot);
	}

	@Inject(method = "canTakeItemForPickAll", at = @At("HEAD"), cancellable = true)
	private void pocketpals$keepBeltOnPickAll(final ItemStack carried, final Slot target, final CallbackInfoReturnable<Boolean> cir) {
		if (BeltFeature.isBeltSlot(target)) {
			cir.setReturnValue(false);
		}
	}
}
