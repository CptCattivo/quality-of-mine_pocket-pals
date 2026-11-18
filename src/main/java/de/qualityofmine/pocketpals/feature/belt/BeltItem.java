package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.feature.pokeball.PokeballItem;
import de.qualityofmine.pocketpals.feature.pokeball.Pokeballs;
import net.minecraft.server.level.ServerPlayer;
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

	/** Wirft den Ball aus dem Platz. Der Platz ist danach leer, ein leerer Ball bleibt wie beim normalen Wurf nach dem Freilassen am Boden liegen. */
	@Override
	public void useContent(final ServerPlayer player, final BeltHolder holder, final int slot) {
		ItemStack stack = holder.contents().get(slot);
		if (!Pokeballs.isFilled(stack) || !(stack.getItem() instanceof PokeballItem ball)) {
			return;
		}
		ball.throwBall(player.level(), player, stack);
		holder.contents().set(slot, ItemStack.EMPTY);
	}

	@Override
	public boolean canUseContent() {
		return true;
	}
}
