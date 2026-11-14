package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.feature.pokeball.Pokeballs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Einer der 6 Plätze im Gürtel. Es passen nur Pokébälle mit gefangenem Tier hinein, sonst nichts. */
public final class BeltContentSlot extends Slot {
	private final Player player;

	BeltContentSlot(final Player player, final BeltContentContainer container, final int index) {
		super(container, index, BeltFeature.CONTENT_X + index * BeltFeature.CONTENT_STEP, BeltFeature.CONTENT_Y);
		this.player = player;
	}

	@Override
	public boolean mayPlace(final ItemStack stack) {
		return Pokeballs.isFilled(stack) && isActive();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/** Ohne angelegten Gürtel gibt es keine Plätze. */
	@Override
	public boolean isActive() {
		return !BeltFeature.beltOf(player).isEmpty();
	}
}
