package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Der Gürtel. Mit Rechtsklick legt man ihn an, solange noch keiner getragen wird. Man kann ihn auch im Inventar auf
 * seinen Platz legen.
 */
public class BeltItem extends Item {
	public BeltItem(final Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		BeltHolder holder = BeltFeature.holderOf(player);
		if (!holder.get().isEmpty()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		holder.set(stack.copyWithCount(1));
		stack.shrink(1);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}
}
