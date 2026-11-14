package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Der Platz für den Gürtel. Der Halter wird bei jedem Zugriff neu geholt, weil das Inventar-Menü schon entsteht,
 * bevor der Spielstand geladen ist.
 */
final class BeltEquipContainer implements Container {
	private final Player player;

	BeltEquipContainer(final Player player) {
		this.player = player;
	}

	private BeltHolder holder() {
		return BeltFeature.holderOf(player);
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return holder().get().isEmpty();
	}

	@Override
	public ItemStack getItem(final int slot) {
		return holder().get();
	}

	@Override
	public ItemStack removeItem(final int slot, final int count) {
		return holder().get().split(count);
	}

	@Override
	public ItemStack removeItemNoUpdate(final int slot) {
		ItemStack belt = holder().get();
		holder().set(ItemStack.EMPTY);
		return belt;
	}

	@Override
	public void setItem(final int slot, final ItemStack stack) {
		holder().set(stack);
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(final Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		holder().set(ItemStack.EMPTY);
	}
}
