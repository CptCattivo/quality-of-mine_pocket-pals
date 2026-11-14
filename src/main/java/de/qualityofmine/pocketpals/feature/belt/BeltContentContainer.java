package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Die 6 Plätze im Gürtel. Sie lesen und schreiben den Inhalt des angelegten Gürtels, ohne Gürtel sind sie leer. */
final class BeltContentContainer implements Container {
	private final Player player;

	BeltContentContainer(final Player player) {
		this.player = player;
	}

	private NonNullList<ItemStack> read() {
		NonNullList<ItemStack> items = NonNullList.withSize(BeltFeature.CONTENT_SIZE, ItemStack.EMPTY);
		ItemStack belt = BeltFeature.beltOf(player);
		if (!belt.isEmpty()) {
			belt.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
		}
		return items;
	}

	private void write(final NonNullList<ItemStack> items) {
		ItemStack belt = BeltFeature.beltOf(player);
		if (!belt.isEmpty()) {
			belt.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
		}
	}

	@Override
	public int getContainerSize() {
		return BeltFeature.CONTENT_SIZE;
	}

	@Override
	public boolean isEmpty() {
		return read().stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(final int slot) {
		return read().get(slot);
	}

	@Override
	public ItemStack removeItem(final int slot, final int count) {
		NonNullList<ItemStack> items = read();
		ItemStack removed = ContainerHelper.removeItem(items, slot, count);
		write(items);
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(final int slot) {
		NonNullList<ItemStack> items = read();
		ItemStack removed = ContainerHelper.takeItem(items, slot);
		write(items);
		return removed;
	}

	@Override
	public void setItem(final int slot, final ItemStack stack) {
		NonNullList<ItemStack> items = read();
		items.set(slot, stack);
		write(items);
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
		write(NonNullList.withSize(BeltFeature.CONTENT_SIZE, ItemStack.EMPTY));
	}
}
