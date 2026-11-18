package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Der Platz im Inventar, auf dem Gürtel, Köcher oder Trankgurt getragen werden. Er nimmt nur diese an. */
public final class BeltEquipSlot extends Slot {
	BeltEquipSlot(final BeltEquipContainer container) {
		super(container, 0, BeltFeature.EQUIP_X, BeltFeature.EQUIP_Y);
	}

	@Override
	public boolean mayPlace(final ItemStack stack) {
		return stack.getItem() instanceof WearableItem;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}
}
