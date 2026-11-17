package de.qualityofmine.pocketpals.feature.belt;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Der angelegte Gürtel oder Köcher eines Spielers. Er gehört zum Spielstand. Der Inhalt steckt im Item selbst, damit er beim
 * Ablegen und beim Fallenlassen mit ihm zusammenbleibt.
 * <p>
 * Solange das Item getragen wird, liegt der Inhalt als lebende Liste hier. Das Spiel verändert Stapel direkt, etwa wenn der
 * Bogen einen Pfeil verbraucht oder ein Umschalt-Klick Pfeile auf einen Stapel legt. Erst wenn das Item herausgegeben wird,
 * schreibt {@link #get()} den Inhalt zurück hinein.
 */
public final class BeltHolder {
	static final Codec<BeltHolder> CODEC = ItemStack.OPTIONAL_CODEC.xmap(stack -> {
		BeltHolder holder = new BeltHolder();
		holder.set(stack);
		return holder;
	}, BeltHolder::get);

	private ItemStack stack = ItemStack.EMPTY;
	private final NonNullList<ItemStack> contents = NonNullList.withSize(BeltFeature.CONTENT_SIZE, ItemStack.EMPTY);
	/** Der mit Alt und Mausrad gewählte Platz. Er wird nicht gespeichert und vom Client mitgeteilt. */
	private int selected;

	/** Das getragene Item mit aktuellem Inhalt, leer ohne Gürtel oder Köcher. */
	public ItemStack get() {
		if (!stack.isEmpty()) {
			stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
		}
		return stack;
	}

	public void set(final ItemStack worn) {
		this.stack = worn;
		contents.clear();
		if (!worn.isEmpty()) {
			worn.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
		}
	}

	/** Das getragene Item, falls es Gürtel oder Köcher ist, sonst null. Schreibt den Inhalt nicht zurück. */
	public WearableItem wornItem() {
		return !stack.isEmpty() && stack.getItem() instanceof WearableItem wearable ? wearable : null;
	}

	/** Der Inhalt als lebende Liste. Ohne getragenes Item ist er bedeutungslos. */
	NonNullList<ItemStack> contents() {
		return contents;
	}

	public int selected() {
		return selected;
	}

	public void select(final int slot) {
		this.selected = Math.clamp(slot, 0, BeltFeature.CONTENT_SIZE - 1);
	}
}
