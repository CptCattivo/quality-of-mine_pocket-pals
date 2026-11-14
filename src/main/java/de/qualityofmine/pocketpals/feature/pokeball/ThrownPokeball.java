package de.qualityofmine.pocketpals.feature.pokeball;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Der geworfene Pokéball. Er läuft durch diese Phasen, alles auf dem Server. Die Clients sehen nur die
 * Positionen und die Partikel:
 * <ul>
 * <li>{@code FLYING}: Flug wie beim Ei bis zum ersten Treffer.</li>
 * <li>{@code FALLING}: Leerer Ball hat ein Tier eingefangen und fällt zu Boden.</li>
 * <li>{@code SHAKING}: Der Ball wackelt wie im Spiel dreimal und entscheidet dann über Fang oder Ausbruch.</li>
 * <li>{@code RELEASING}: Ball mit Tier ist aufgetroffen, öffnet sich und lässt das Tier frei.</li>
 * </ul>
 * Das Tier steckt während der Animation schon als Daten im Ball und nicht mehr in der Welt.
 */
public class ThrownPokeball extends ThrowableItemProjectile {
	private static final int SHAKE_TICKS = 20;
	private static final int WOBBLE_TICKS = 8;
	private static final int SETTLE_TICKS = 12;
	private static final int RELEASE_TICKS = 12;
	private static final int MAX_FALL_TICKS = 100;
	private static final double WOBBLE_WIDTH = 0.1;
	private static final float PROVOKE_DAMAGE = 1.0F;
	private static final int CAPTURE_SHAKES = 3;
	/** Ein Tier, das schon einmal gefangen war, geht ohne Wackeln fast sofort wieder in den Ball. */
	private static final int KNOWN_SHAKES = 1;
	/** Merkt sich am Tier, dass es schon einmal im Ball war. Steht in den normalen Tags und bleibt deshalb erhalten. */
	public static final String CAUGHT_TAG = "qualityofmine_caught";
	private static final int RED = 0xFF2A2A;
	private static final int BLUE = 0x33CCFF;

	enum Phase {
		FLYING, FALLING, SHAKING, RELEASING
	}

	private Phase phase = Phase.FLYING;
	private int phaseTicks;
	private boolean success;
	private int shakes;
	/** Das Tier, das gerade eingefangen wird. Beim Freilassen steht es stattdessen im Item. */
	private CaughtAnimal capturing;
	private Vec3 restPosition;

	public ThrownPokeball(final EntityType<? extends ThrownPokeball> type, final Level level) {
		super(type, level);
	}

	public ThrownPokeball(final Level level, final LivingEntity owner, final ItemStack stack) {
		super(Pokeballs.THROWN_POKEBALL, owner, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return Pokeballs.empty(BallType.POKEBALL);
	}

	private BallType ballType() {
		return getItem().getItem() instanceof PokeballItem ball ? ball.type() : BallType.POKEBALL;
	}

	/** Ein Tier, das schon einmal in einem Ball war und sich ohne Risiko wieder einfangen lässt. */
	public static boolean isKnown(final Animal animal) {
		return animal.entityTags().contains(CAUGHT_TAG);
	}

	/** Fremde Haustiere sind tabu. Ihr Besitzer darf sie aber fangen, zum Beispiel für den Umzug. */
	public static boolean canCapture(final Animal animal, final Entity thrower) {
		if (!animal.isAlive()) {
			return false;
		}
		return !(animal instanceof OwnableEntity pet) || pet.getOwner() == null || pet.getOwner() == thrower;
	}

	/**
	 * Fängt ein bekanntes Tier sofort mit der Animation: Der Ball erscheint am Tier, es verschwindet darin und der Ball
	 * klickt zu. Der Aufrufer nimmt den Ball aus der Hand.
	 */
	public static void captureKnown(final ServerLevel server, final Player player, final ItemStack ball, final Animal animal) {
		ThrownPokeball thrown = new ThrownPokeball(server, player, ball.copyWithCount(1));
		thrown.setPos(animal.position().add(0.0, animal.getBbHeight() / 2.0, 0.0));
		server.addFreshEntity(thrown);
		thrown.startCapture(animal);
	}

	/** Das Tier im Ball: beim Freilassen das aus dem Item, beim Fangen das gerade eingefangene. */
	private CaughtAnimal inside() {
		return capturing != null ? capturing : getItem().get(Pokeballs.CAUGHT_ANIMAL);
	}

	@Override
	public void tick() {
		if (phase == Phase.FLYING || !(level() instanceof ServerLevel server)) {
			super.tick();
			return;
		}
		// Ab jetzt steuert der Server die Bewegung selbst, der Client soll nichts dazurechnen
		baseTick();
		phaseTicks++;
		switch (phase) {
			case FALLING -> fall();
			case SHAKING -> shake(server);
			case RELEASING -> release(server);
			default -> {
			}
		}
	}

	// --- Treffer -------------------------------------------------------------------------------------------------

	@Override
	protected void onHitEntity(final EntityHitResult result) {
		if (level().isClientSide() || phase != Phase.FLYING) {
			return;
		}
		Entity target = result.getEntity();
		if (Pokeballs.isFilled(getItem())) {
			startRelease(result.getLocation());
		} else if (target instanceof Animal animal && canCapture(animal, getOwner())) {
			startCapture(animal);
		} else {
			dropBall();
		}
	}

	@Override
	protected void onHitBlock(final BlockHitResult result) {
		if (level().isClientSide() || phase != Phase.FLYING) {
			return;
		}
		if (Pokeballs.isFilled(getItem())) {
			// Etwas vor die getroffene Fläche, damit das Tier nicht im Block erscheint
			Direction face = result.getDirection();
			startRelease(result.getLocation().add(face.getStepX() * 0.3, face.getStepY() * 0.3, face.getStepZ() * 0.3));
		} else {
			dropBall();
		}
	}

	/** Ein verfehlter Ball geht nicht verloren, sondern liegt wieder zum Aufheben da. */
	private void dropBall() {
		if (level() instanceof ServerLevel server) {
			spawnAtLocation(server, getItem().copy());
		}
		discard();
	}

	// --- Fangen --------------------------------------------------------------------------------------------------

	private void startCapture(final Animal animal) {
		ServerLevel server = (ServerLevel) level();
		if (isKnown(animal)) {
			success = true;
			shakes = KNOWN_SHAKES;
		} else {
			double chance = CatchChance.of(ballType(), animal.getHealth(), animal.getMaxHealth(), animal.isBaby());
			success = random.nextDouble() < chance;
			// Wie im Spiel: Gelingt der Fang, wackelt der Ball dreimal, sonst bricht das Tier unterwegs aus
			shakes = success ? CAPTURE_SHAKES : 1 + random.nextInt(CAPTURE_SHAKES);
		}

		animal.ejectPassengers();
		animal.stopRiding();
		if (animal.isLeashed()) {
			animal.dropLeash();
		}
		if (success) {
			animal.addTag(CAUGHT_TAG);
			if (getOwner() instanceof Player player) {
				tame(animal, player);
			}
		}
		capturing = CaughtAnimal.of(animal);
		Vec3 from = animal.position().add(0.0, animal.getBbHeight() / 2.0, 0.0);
		animal.discard();

		beam(server, from, position(), RED);
		sound(server, SoundEvents.BEACON_ACTIVATE, 0.6F, 2.0F);
		enter(Phase.FALLING);
	}

	/** Ein gefangenes wildes Tier gehört danach dem Fänger. Zahme Tiere bleiben, wie sie sind. */
	private static void tame(final Animal animal, final Player player) {
		if (animal instanceof TamableAnimal pet && !pet.isTame()) {
			pet.tame(player);
		} else if (animal instanceof AbstractHorse horse && !horse.isTamed()) {
			horse.setTamed(true);
			horse.setOwner(player);
		}
	}

	private void enter(final Phase next) {
		phase = next;
		phaseTicks = 0;
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
	}

	private void fall() {
		Vec3 motion = new Vec3(0.0, Math.max(getDeltaMovement().y - 0.04, -1.0), 0.0);
		setDeltaMovement(motion);
		move(MoverType.SELF, motion);
		if (onGround() || phaseTicks > MAX_FALL_TICKS) {
			setDeltaMovement(Vec3.ZERO);
			restPosition = position();
			enter(Phase.SHAKING);
		}
	}

	private void shake(final ServerLevel server) {
		if (restPosition == null) {
			restPosition = position();
		}
		int shake = phaseTicks / SHAKE_TICKS;
		int local = phaseTicks % SHAKE_TICKS;

		if (shake >= shakes) {
			// Kurz stillhalten, dann entscheidet sich der Fang
			setPos(restPosition);
			if (phaseTicks >= shakes * SHAKE_TICKS + SETTLE_TICKS) {
				finishCapture(server);
			}
			return;
		}
		if (local == 1) {
			sound(server, SoundEvents.LODESTONE_COMPASS_LOCK, 0.8F, 1.0F + 0.15F * shake);
		}
		if (local < WOBBLE_TICKS) {
			double angle = Math.toRadians(getYRot());
			double offset = Math.sin(local * Math.PI * 2.0 / WOBBLE_TICKS) * WOBBLE_WIDTH;
			setPos(restPosition.add(Math.cos(angle) * offset, 0.0, Math.sin(angle) * offset));
		} else {
			setPos(restPosition);
		}
	}

	private void finishCapture(final ServerLevel server) {
		Vec3 center = position().add(0.0, 0.1, 0.0);
		if (success) {
			// Der Klick: Der Knopf leuchtet blau auf, dann liegt der volle Ball zum Aufheben da
			sound(server, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
			sound(server, SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F, 1.6F);
			server.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 12, 0.15, 0.15, 0.15, 0.05);
			server.sendParticles(new DustParticleOptions(BLUE, 1.2F), center.x, center.y, center.z, 16, 0.2, 0.2, 0.2, 0.0);
			spawnAtLocation(server, Pokeballs.filled(ballType(), capturing));
		} else {
			// Ausbruch: Der Ball springt auf und das Tier steht wieder da, der Ball ist verbraucht
			sound(server, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.6F);
			server.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 10, 0.15, 0.15, 0.15, 0.03);
			server.sendParticles(new DustParticleOptions(RED, 1.2F), center.x, center.y, center.z, 16, 0.2, 0.2, 0.2, 0.0);
			capturing.release(server, position(), getYRot()).ifPresent(this::provoke);
		}
		discard();
	}

	/** Wer ausbricht, wurde getroffen: Das Tier flüchtet, und manche greifen den Werfer an. */
	private void provoke(final Entity animal) {
		provoke(animal, getOwner(), this);
	}

	/**
	 * Behandelt das Tier wie nach einem Treffer. Ohne echten Schaden löst das Spiel weder Flucht noch Angriff aus,
	 * deshalb bekommt es bis zu 1 Leben Schaden, bleibt aber immer am Leben.
	 */
	public static void provoke(final Entity animal, final Entity attacker, final Entity source) {
		float damage = animal instanceof LivingEntity living ? Math.min(PROVOKE_DAMAGE, living.getHealth() - 0.5F) : 0.0F;
		animal.hurt(animal.damageSources().thrown(source, attacker), Math.max(damage, 0.0F));
	}

	// --- Freilassen ----------------------------------------------------------------------------------------------

	private void startRelease(final Vec3 at) {
		setPos(at);
		enter(Phase.RELEASING);
		sound((ServerLevel) level(), SoundEvents.BEACON_DEACTIVATE, 0.6F, 2.0F);
	}

	private void release(final ServerLevel server) {
		Vec3 center = position();
		// Ein Wirbel aus Rot und Weiß steigt aus dem Ball auf, wie der Lichtstrahl im Anime
		double angle = phaseTicks * 0.9;
		for (int arm = 0; arm < 2; arm++) {
			double phaseAngle = angle + arm * Math.PI;
			double radius = 0.35 * (1.0 - (double) phaseTicks / RELEASE_TICKS * 0.5);
			ParticleOptions particle = arm == 0 ? new DustParticleOptions(RED, 1.0F) : ParticleTypes.END_ROD;
			server.sendParticles(particle, center.x + Math.cos(phaseAngle) * radius, center.y + phaseTicks * 0.05,
				center.z + Math.sin(phaseAngle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
		}
		if (phaseTicks < RELEASE_TICKS) {
			return;
		}
		sound(server, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.8F);
		server.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.3, center.z, 20, 0.3, 0.3, 0.3, 0.05);
		server.sendParticles(new DustParticleOptions(RED, 1.5F), center.x, center.y + 0.3, center.z, 20, 0.3, 0.3, 0.3, 0.0);
		CaughtAnimal animal = inside();
		if (animal == null || animal.release(server, center, getYRot()).isEmpty()) {
			// Das Tier gibt es nicht mehr, etwa weil ein Mod fehlt. Der Ball bleibt trotzdem erhalten.
			spawnAtLocation(server, getItem().copy());
		} else {
			spawnAtLocation(server, new ItemStack(Pokeballs.empty(ballType())));
		}
		discard();
	}

	// --- Hilfen --------------------------------------------------------------------------------------------------

	private void sound(final ServerLevel server, final SoundEvent event, final float volume, final float pitch) {
		server.playSound(null, getX(), getY(), getZ(), event, SoundSource.NEUTRAL, volume, pitch);
	}

	/** Eine Spur aus Partikeln vom Tier zum Ball. */
	private static void beam(final ServerLevel server, final Vec3 from, final Vec3 to, final int color) {
		int steps = 12;
		for (int step = 0; step <= steps; step++) {
			Vec3 point = from.lerp(to, (double) step / steps);
			server.sendParticles(new DustParticleOptions(color, 1.2F), point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.0);
		}
		server.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 8, 0.2, 0.3, 0.2, 0.05);
	}

	// --- Speichern -----------------------------------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Phase", phase.name());
		output.putInt("PhaseTicks", phaseTicks);
		output.putBoolean("Success", success);
		output.putInt("Shakes", shakes);
		if (capturing != null) {
			output.store("Capturing", CaughtAnimal.CODEC, capturing);
		}
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		try {
			phase = Phase.valueOf(input.getStringOr("Phase", Phase.FLYING.name()));
		} catch (IllegalArgumentException exception) {
			phase = Phase.FLYING;
		}
		phaseTicks = input.getIntOr("PhaseTicks", 0);
		success = input.getBooleanOr("Success", false);
		shakes = input.getIntOr("Shakes", 0);
		capturing = input.read("Capturing", CaughtAnimal.CODEC).orElse(null);
		if (phase != Phase.FLYING) {
			setNoGravity(true);
		}
	}
}
