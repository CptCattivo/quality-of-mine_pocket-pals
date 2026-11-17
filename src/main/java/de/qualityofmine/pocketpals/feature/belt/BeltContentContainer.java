package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Die 6 Plätze in Gürtel oder Köcher. Sie arbeiten direkt auf dem Inhalt des Halters, ohne getragenes Item sind sie leer. */
final class BeltContentContainer implements Container {
	private final Player player;

	BeltContentContainer(final Player player) {
		this.player = player;
	}

	private BeltHolder holder() {
		return BeltFeature.holderOf(player);
	}

	private boolean isWorn() {
		return holder().wornItem() != null;
	}

	@Override
	public int getContainerSize() {
		return BeltFeature.CONTENT_SIZE;
	}

	@Override
	public boolean isEmpty() {
		return !isWorn() || holder().contents().stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(final int slot) {
		return isWorn() ? holder().contents().get(slot) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(final int slot, final int count) {
		return isWorn() ? ContainerHelper.removeItem(holder().contents(), slot, count) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(final int slot) {
		return isWorn() ? ContainerHelper.takeItem(holder().contents(), slot) : ItemStack.EMPTY;
	}

	@Override
	public void setItem(final int slot, final ItemStack stack) {
		if (isWorn()) {
			holder().contents().set(slot, stack);
		}
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
		holder().contents().clear();
	}
}
