package de.qualityofmine.pocketpals.feature.belt;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Der angelegte Gürtel, Köcher oder Trankgurt eines Spielers. Er gehört zum Spielstand. Der Inhalt steckt im Item selbst,
 * damit er beim Ablegen und beim Fallenlassen mit ihm zusammenbleibt.
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
	private static final int NOT_DRINKING = -1;

	private ItemStack stack = ItemStack.EMPTY;
	private final NonNullList<ItemStack> contents = NonNullList.withSize(BeltFeature.CONTENT_SIZE, ItemStack.EMPTY);
	/** Der mit Alt und Mausrad gewählte Platz. Er wird nicht gespeichert und vom Client mitgeteilt. */
	private int selected;
	/** Der Platz im Trankgurt, aus dem gerade getrunken wird, und wie lange schon. Beides wird nicht gespeichert. */
	private int drinkingSlot = NOT_DRINKING;
	private int drinkingTicks;

	/** Das getragene Item mit aktuellem Inhalt, leer ohne Gürtel, Köcher oder Trankgurt. */
	public ItemStack get() {
		if (!stack.isEmpty()) {
			BeltContents saved = BeltContents.of(contents);
			if (saved.isEmpty()) {
				stack.remove(Belts.CONTENTS);
			} else {
				stack.set(Belts.CONTENTS, saved);
			}
		}
		return stack;
	}

	public void set(final ItemStack worn) {
		this.stack = worn;
		contents.clear();
		if (worn.isEmpty()) {
			return;
		}
		BeltContents saved = worn.get(Belts.CONTENTS);
		if (saved != null) {
			saved.copyInto(contents);
		} else {
			// Gürtel und Köcher aus Version 2.0.0 haben ihren Inhalt noch im Behälter des Spiels
			worn.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
		}
		worn.remove(DataComponents.CONTAINER);
	}

	/** Das getragene Item, falls es Gürtel, Köcher oder Trankgurt ist, sonst null. Schreibt den Inhalt nicht zurück. */
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

	/** Wählt einen Platz. Ein Wechsel bricht das Trinken ab, wie ein Wechsel in der Item-Leiste. */
	public void select(final int slot) {
		this.selected = Math.clamp(slot, 0, BeltFeature.CONTENT_SIZE - 1);
		if (isDrinking() && drinkingSlot != selected) {
			stopDrinking();
		}
	}

	public boolean isDrinking() {
		return drinkingSlot != NOT_DRINKING;
	}

	/** Der Platz, aus dem getrunken wird. Nur gültig, solange {@link #isDrinking()} gilt. */
	public int drinkingSlot() {
		return drinkingSlot;
	}

	/** Wie viele Ticks schon getrunken wird. */
	public int drinkingTicks() {
		return drinkingTicks;
	}

	void startDrinking(final int slot) {
		this.drinkingSlot = slot;
		this.drinkingTicks = 0;
	}

	/** Zählt einen Tick weiter und gibt die neue Dauer zurück. */
	int tickDrinking() {
		return ++drinkingTicks;
	}

	public void stopDrinking() {
		this.drinkingSlot = NOT_DRINKING;
		this.drinkingTicks = 0;
	}
}
