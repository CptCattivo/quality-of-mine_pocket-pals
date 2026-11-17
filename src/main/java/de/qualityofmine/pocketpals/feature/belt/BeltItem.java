package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.feature.pokeball.Pokeballs;
import net.minecraft.world.item.ItemStack;

/** Der Gürtel. Er trägt nur Pokébälle mit gefangenem Tier, einen pro Platz. */
public class BeltItem extends WearableItem {
	public BeltItem(final Properties properties) {
		super(properties);
	}

	@Override
	public boolean accepts(final ItemStack stack) {
		return Pokeballs.isFilled(stack);
	}

	@Override
	public int contentStackSize() {
		return 1;
	}
}
