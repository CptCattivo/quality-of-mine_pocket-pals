package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Einer der 6 Plätze in Gürtel, Köcher oder Trankgurt. Was hineinpasst, bestimmt das getragene Item. */
public final class BeltContentSlot extends Slot {
	private final Player player;

	/** {@code x} und {@code y} sind die Position des ersten Platzes, die übrigen folgen rechts daneben. */
	BeltContentSlot(final Player player, final BeltContentContainer container, final int index, final int x, final int y) {
		super(container, index, x + index * BeltFeature.CONTENT_STEP, y);
		this.player = player;
	}

	@Override
	public boolean mayPlace(final ItemStack stack) {
		WearableItem worn = BeltFeature.holderOf(player).wornItem();
		return worn != null && worn.accepts(stack);
	}

	@Override
	public int getMaxStackSize() {
		WearableItem worn = BeltFeature.holderOf(player).wornItem();
		return worn == null ? 1 : worn.contentStackSize();
	}

	/** Das getragene Item bestimmt die Stapelgröße allein, Tränke stapeln sich im Trankgurt also auch über ihre normale Größe. */
	@Override
	public int getMaxStackSize(final ItemStack stack) {
		return getMaxStackSize();
	}

	/** Ohne Gürtel, Köcher oder Trankgurt gibt es keine Plätze. */
	@Override
	public boolean isActive() {
		return BeltFeature.holderOf(player).wornItem() != null;
	}
}
