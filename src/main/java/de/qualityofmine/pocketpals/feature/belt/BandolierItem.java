package de.qualityofmine.pocketpals.feature.belt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;

/**
 * Der Trankgurt. Er trägt Flaschen: Tränke, Wurf- und Verweiltränke, Wasser-, Honig- und unheilvolle Flaschen. Gleiche
 * Flaschen stapeln sich darin zu 8, außerhalb bleibt es bei der normalen Stapelgröße.
 * <p>
 * Aus dem gewählten Platz wird getrunken, solange die Taste gehalten wird, und zwar so lange wie aus der Hand. Wurf- und
 * Verweiltränke fliegen sofort.
 */
public class BandolierItem extends WearableItem {
	private static final int STACK_SIZE = 8;
	/** Partikel pro Schluck, wie beim Trinken aus der Hand. */
	private static final int SIP_PARTICLES = 5;
	private static final int FINISH_PARTICLES = 16;
	/** Wurfwinkel und Kraft wie beim Werfen aus der Hand. */
	private static final float THROW_PITCH_OFFSET = -20.0F;
	private static final float THROW_POWER = 0.5F;
	private static final float THROW_UNCERTAINTY = 1.0F;
	private static final float THROW_VOLUME = 0.5F;

	public BandolierItem(final Properties properties) {
		super(properties);
	}

	@Override
	public boolean accepts(final ItemStack stack) {
		return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)
			|| stack.is(Items.HONEY_BOTTLE) || stack.is(Items.OMINOUS_BOTTLE);
	}

	@Override
	public int contentStackSize() {
		return STACK_SIZE;
	}

	/** Kann der Spieler das trinken? Wurftränke und leere Plätze nicht, Honig auch mit vollem Hunger. */
	public static boolean isDrinkable(final Player player, final ItemStack stack) {
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		return consumable != null && consumable.canConsume(player, stack);
	}

	/** Wirft einen Wurf- oder Verweiltrank sofort, bei allem anderen beginnt das Trinken. */
	@Override
	public void useContent(final ServerPlayer player, final BeltHolder holder, final int slot) {
		ItemStack stack = holder.contents().get(slot);
		if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
			throwPotion(player, stack);
		} else {
			startDrinking(player, holder, slot);
		}
	}

	@Override
	public boolean canUseContent() {
		return true;
	}

	/**
	 * Beginnt das Trinken aus einem Platz, sofern dort etwas Trinkbares liegt. Läuft es schon, bleibt es dabei, denn Taste und
	 * Rechtsklick melden sich beim Halten wiederholt. Der Client ruft das ebenfalls auf, für Geräusch, Fortschritt und Verlangsamung.
	 */
	public static void startDrinking(final Player player, final BeltHolder holder, final int slot) {
		boolean alreadyDrinking = holder.isDrinking() && holder.drinkingSlot() == slot;
		if (!alreadyDrinking && isDrinkable(player, holder.contents().get(slot))) {
			holder.startDrinking(slot);
		}
	}

	private static void throwPotion(final ServerPlayer player, final ItemStack stack) {
		ServerLevel level = player.level();
		boolean splash = stack.is(Items.SPLASH_POTION);
		SoundEvent sound = splash ? SoundEvents.SPLASH_POTION_THROW : SoundEvents.LINGERING_POTION_THROW;
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, THROW_VOLUME,
			0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		ItemStack thrown = stack.copyWithCount(1);
		if (splash) {
			Projectile.spawnProjectileFromRotation(ThrownSplashPotion::new, level, thrown, player, THROW_PITCH_OFFSET, THROW_POWER, THROW_UNCERTAINTY);
		} else {
			Projectile.spawnProjectileFromRotation(ThrownLingeringPotion::new, level, thrown, player, THROW_PITCH_OFFSET, THROW_POWER, THROW_UNCERTAINTY);
		}
		player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		stack.consume(1, player);
	}

	/**
	 * Ein Tick Trinken, auf Server und Client. Beide machen Geräusch und Partikel, der Server für die anderen Spieler, der
	 * Client für den Trinkenden selbst, wie beim Trinken aus der Hand. Nur der Server verbraucht die Flasche. Solange die
	 * Taste gehalten wird, geht es mit der nächsten Flasche weiter.
	 */
	public static void tickDrinking(final Player player) {
		BeltHolder holder = BeltFeature.holderOf(player);
		if (!holder.isDrinking()) {
			return;
		}
		ItemStack stack = holder.contents().get(holder.drinkingSlot());
		if (!(holder.wornItem() instanceof BandolierItem) || !player.isAlive() || player.isSpectator() || !isDrinkable(player, stack)) {
			holder.stopDrinking();
			return;
		}
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		int duration = stack.getUseDuration(player);
		int ticks = holder.tickDrinking();
		if (consumable.shouldEmitParticlesAndSounds(duration - ticks)) {
			consumable.emitParticlesAndSounds(player.getRandom(), player, stack, SIP_PARTICLES);
		}
		if (ticks < duration) {
			return;
		}
		if (player.level() instanceof ServerLevel level) {
			drink(player, level, stack);
		} else {
			consumable.emitParticlesAndSounds(player.getRandom(), player, stack, FINISH_PARTICLES);
		}
		holder.startDrinking(holder.drinkingSlot());
	}

	/** Trinkt eine Flasche aus dem Stapel. Die leere Flasche kommt ins Inventar oder fällt zu Boden, wenn es voll ist. */
	private static void drink(final Player player, final ServerLevel level, final ItemStack stack) {
		if (player.hasInfiniteMaterials()) {
			stack.copyWithCount(1).finishUsingItem(level, player);
			return;
		}
		ItemStack remainder = stack.split(1).finishUsingItem(level, player);
		if (!remainder.isEmpty()) {
			player.getInventory().placeItemBackInInventory(remainder, Prediction.SERVER_ONLY);
		}
	}
}
