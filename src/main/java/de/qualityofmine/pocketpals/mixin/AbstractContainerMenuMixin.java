package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
	@Shadow
	@Final
	public NonNullList<Slot> slots;

	/** Hat der Gürtel einen Teil übernommen, gilt das Verschieben als erfolgreich, auch wenn für den Rest kein Platz war. */
	@Unique
	private boolean pocketpals$movedIntoBelt;

	/**
	 * Umschalt-Klick aus einer Truhe, einem Ofen und so weiter ins Spielerinventar legt Pfeile, volle Bälle und Tränke zuerst
	 * in Köcher, Gürtel oder Trankgurt. Im eigenen Inventar gilt das nicht, sonst kämen Gegenstände aus dem Köcher sofort wieder hinein.
	 */
	@Inject(method = "moveItemStackTo", at = @At("HEAD"), cancellable = true)
	private void pocketpals$fillBeltFirst(final ItemStack stack, final int start, final int end, final boolean reverse,
		final CallbackInfoReturnable<Boolean> cir) {
		pocketpals$movedIntoBelt = false;
		Object menu = this;
		if (menu instanceof InventoryMenu || start < 0 || start >= end || end > this.slots.size()
			|| !(this.slots.get(start).container instanceof Inventory inventory)) {
			return;
		}
		pocketpals$movedIntoBelt = BeltFeature.insert(inventory.player, stack);
		if (stack.isEmpty()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "moveItemStackTo", at = @At("RETURN"), cancellable = true)
	private void pocketpals$reportPartialMove(final ItemStack stack, final int start, final int end, final boolean reverse,
		final CallbackInfoReturnable<Boolean> cir) {
		if (pocketpals$movedIntoBelt && !cir.getReturnValueZ()) {
			cir.setReturnValue(true);
		}
	}

	/** Zahlentaste über einem Platz im Trankgurt: Ein Stapel von 8 Tränken darf nicht als Ganzes auf einen Inventarplatz. */
	@Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
	private void pocketpals$swapOversizedStack(final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player,
		final CallbackInfo ci) {
		if (containerInput == ContainerInput.SWAP && slotIndex >= 0 && slotIndex < this.slots.size()
			&& BeltFeature.swapOversizedStack(this.slots.get(slotIndex), player.getInventory(), buttonNum)) {
			ci.cancel();
		}
	}
}
