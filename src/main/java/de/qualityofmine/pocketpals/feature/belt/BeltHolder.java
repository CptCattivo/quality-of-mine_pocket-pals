package de.qualityofmine.pocketpals.feature.belt;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;

/**
 * Der angelegte Gürtel eines Spielers. Er gehört zum Spielstand, der Inhalt steckt im Gürtel selbst, damit er beim
 * Ablegen und beim Fallenlassen mit dem Gürtel zusammenbleibt.
 */
public final class BeltHolder {
	static final Codec<BeltHolder> CODEC = ItemStack.OPTIONAL_CODEC.xmap(stack -> {
		BeltHolder holder = new BeltHolder();
		holder.stack = stack;
		return holder;
	}, holder -> holder.stack);

	private ItemStack stack = ItemStack.EMPTY;

	public ItemStack get() {
		return stack;
	}

	public void set(final ItemStack belt) {
		this.stack = belt;
	}
}
