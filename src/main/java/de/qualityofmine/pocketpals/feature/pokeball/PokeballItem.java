package de.qualityofmine.pocketpals.feature.pokeball;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Der Pokéball. Leer fängt er ein Tier, mit Tier darin lässt er es wieder frei. Beides geschieht durch Werfen. */
public class PokeballItem extends Item {
	private final BallType type;
	private final boolean filled;

	public PokeballItem(final Properties properties, final BallType type, final boolean filled) {
		super(properties);
		this.type = type;
		this.filled = filled;
	}

	public BallType type() {
		return type;
	}

	/** Ein voller Ball enthält ein Tier und lässt es beim Werfen frei, ein leerer fängt eines. */
	public boolean isFilled() {
		return filled;
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		// Geworfen wird wie beim Ei
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS,
			0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (level instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileFromRotation(ThrownPokeball::new, serverLevel, stack, player, 0.0F, 1.5F, 1.0F);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	/** Mit Tier darin steht es in Klammern hinter dem Namen, zum Beispiel „Pokéball (Schaf)“. */
	@Override
	public Component getName(final ItemStack stack) {
		CaughtAnimal caught = stack.get(Pokeballs.CAUGHT_ANIMAL);
		Component name = Component.translatable(Pokeballs.empty(type).getDescriptionId());
		return caught == null ? name : Component.translatable("item.qualityofmine.pokeball.caught", name, caught.displayName());
	}
}
