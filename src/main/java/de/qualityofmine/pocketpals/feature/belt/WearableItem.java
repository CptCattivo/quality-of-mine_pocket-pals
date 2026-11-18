package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ein Item für den Platz neben der Rüstung: Gürtel, Köcher oder Trankgurt. Alle haben 6 Plätze, legen fest, was hineinpasst,
 * und werden mit Rechtsklick angelegt, solange der Platz frei ist.
 */
public abstract class WearableItem extends Item {
	protected WearableItem(final Properties properties) {
		super(properties);
	}

	/** Passt dieser Gegenstand in einen der 6 Plätze? */
	public abstract boolean accepts(ItemStack stack);

	/** Wie viele Gegenstände auf einem der 6 Plätze liegen dürfen. */
	public abstract int contentStackSize();

	/**
	 * Benutzt den Inhalt eines Platzes, ausgelöst per Taste oder Alt + Rechtsklick, ohne dass das Inventar offen ist.
	 * Ohne eigene Aktion passiert nichts, der Köcher etwa wird nur vom Bogen benutzt.
	 */
	public void useContent(final ServerPlayer player, final BeltHolder holder, final int slot) {
	}

	/** Hat {@link #useContent} eine Aktion? Nur dann gehören Taste und Alt + Rechtsklick dem Item. */
	public boolean canUseContent() {
		return false;
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		BeltHolder holder = BeltFeature.holderOf(player);
		if (holder.wornItem() != null) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		holder.set(stack.copyWithCount(1));
		stack.shrink(1);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}
}
