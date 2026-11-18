package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
abstract class InventoryMixin {
	@Shadow
	@Final
	public Player player;

	/** So viele Gegenstände waren vor dem Aufheben im Stapel, um zu erkennen, ob etwas übernommen wurde. */
	@Unique
	private int pocketpals$countBefore;

	/**
	 * Volle Bälle, Pfeile und Tränke kommen beim Aufheben zuerst in Gürtel, Köcher oder Trankgurt. Was dort keinen Platz findet, geht wie
	 * gewohnt ins normale Inventar.
	 */
	@Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void pocketpals$fillBeltFirst(final int slot, final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
		pocketpals$countBefore = stack.getCount();
		// Ein Platz ungleich -1 ist eine gezielte Vorgabe, die bleibt unberührt
		if (slot == -1 && !this.player.level().isClientSide() && BeltFeature.insert(this.player, stack) && stack.isEmpty()) {
			cir.setReturnValue(true);
		}
	}

	/** Hat der Gürtel einen Teil übernommen und das Inventar den Rest nicht, gilt das Aufheben trotzdem als erfolgreich. */
	@Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
	private void pocketpals$reportPartialPickup(final int slot, final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && stack.getCount() < pocketpals$countBefore) {
			cir.setReturnValue(true);
		}
	}
}
