package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/** Der Köcher. Er trägt nur Pfeile, jeder Platz einen vollen Stapel. Der gewählte Pfeil wird mit Bogen oder Armbrust verschossen. */
public class QuiverItem extends WearableItem {
	private static final int STACK_SIZE = 64;

	public QuiverItem(final Properties properties) {
		super(properties);
	}

	@Override
	public boolean accepts(final ItemStack stack) {
		return stack.is(ItemTags.ARROWS);
	}

	@Override
	public int contentStackSize() {
		return STACK_SIZE;
	}
}
