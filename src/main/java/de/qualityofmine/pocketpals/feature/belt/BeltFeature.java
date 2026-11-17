package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.Feature;
import de.qualityofmine.pocketpals.PocketPals;
import de.qualityofmine.pocketpals.feature.pokeball.PokeballItem;
import de.qualityofmine.pocketpals.feature.pokeball.Pokeballs;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Gürtel und Köcher mit je 6 Plätzen. Beide werden auf demselben Platz im Inventar getragen, neben den Rüstungsplätzen,
 * und zwar immer nur einer davon. Der Gürtel trägt Pokébälle mit gefangenem Tier, der Köcher Pfeile.
 */
public final class BeltFeature implements Feature {
	public static final int CONTENT_SIZE = 6;
	/** Positionen im Inventar-Menü. Der Client zeichnet die Hintergründe passend dazu. */
	public static final int EQUIP_X = 77;
	public static final int EQUIP_Y = 44;
	public static final int CONTENT_X = 8;
	public static final int CONTENT_Y = 167;
	public static final int CONTENT_STEP = 18;

	private static final AttachmentType<BeltHolder> HOLDER = AttachmentRegistry.<BeltHolder>builder()
		.initializer(BeltHolder::new)
		.persistent(BeltHolder.CODEC)
		.copyOnDeath()
		.buildAndRegister(PocketPals.id("belt"));

	public static BeltHolder holderOf(final Player player) {
		return player.getAttachedOrCreate(HOLDER);
	}

	/** Der angelegte Gürtel oder Köcher samt Inhalt, leer ohne. */
	public static ItemStack beltOf(final Player player) {
		return holderOf(player).get();
	}

	/** Erst der Platz für Gürtel oder Köcher, danach die 6 Plätze darin. */
	public static List<Slot> createSlots(final Player player) {
		List<Slot> slots = new ArrayList<>();
		slots.add(new BeltEquipSlot(new BeltEquipContainer(player)));
		BeltContentContainer content = new BeltContentContainer(player);
		for (int i = 0; i < CONTENT_SIZE; i++) {
			slots.add(new BeltContentSlot(player, content, i));
		}
		return slots;
	}

	/** Beim Einsammeln gleicher Items per Doppelklick darf der Gürtel nicht geleert werden. */
	public static boolean isBeltSlot(final Slot slot) {
		return slot instanceof BeltEquipSlot || slot instanceof BeltContentSlot;
	}

	/**
	 * Gürtel und Köcher fallen beim Tod mit ihrem Inhalt zu Boden wie die Rüstung, außer die Spielregel für das Behalten
	 * des Inventars gilt.
	 */
	public static void dropOnDeath(final Player player, final ServerLevel level) {
		ItemStack belt = beltOf(player);
		if (belt.isEmpty() || level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		holderOf(player).set(ItemStack.EMPTY);
		player.spawnAtLocation(level, belt);
	}

	/**
	 * Legt einen Gegenstand in Gürtel oder Köcher, sofern er dort hineinpasst: erst auf gleiche Stapel, dann in freie Plätze.
	 * Was keinen Platz findet, bleibt in {@code stack}. Gibt true zurück, wenn etwas hineingelegt wurde.
	 */
	public static boolean insert(final Player player, final ItemStack stack) {
		BeltHolder holder = holderOf(player);
		WearableItem worn = holder.wornItem();
		if (worn == null || stack.isEmpty() || !worn.accepts(stack)) {
			return false;
		}
		int before = stack.getCount();
		int limit = Math.min(worn.contentStackSize(), stack.getMaxStackSize());
		List<ItemStack> contents = holder.contents();
		for (int i = 0; i < CONTENT_SIZE && !stack.isEmpty(); i++) {
			ItemStack target = contents.get(i);
			if (!target.isEmpty() && ItemStack.isSameItemSameComponents(target, stack) && target.getCount() < limit) {
				int moved = Math.min(stack.getCount(), limit - target.getCount());
				target.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int i = 0; i < CONTENT_SIZE && !stack.isEmpty(); i++) {
			if (contents.get(i).isEmpty()) {
				contents.set(i, stack.split(Math.min(stack.getCount(), limit)));
			}
		}
		return stack.getCount() < before;
	}

	/**
	 * Der Pfeil aus dem Köcher, den eine Fernkampfwaffe verschießen soll: der gewählte Platz, sonst der erste passende.
	 * Der zurückgegebene Stapel ist der echte Inhalt des Köchers, verbraucht die Waffe davon, fehlt er dort. Leer ohne
	 * Köcher oder ohne passenden Pfeil, dann sucht das Spiel wie gewohnt in Händen und Inventar.
	 */
	public static ItemStack quiverProjectile(final Player player, final ItemStack weapon) {
		BeltHolder holder = holderOf(player);
		if (!(holder.wornItem() instanceof QuiverItem) || !(weapon.getItem() instanceof ProjectileWeaponItem projectileWeapon)) {
			return ItemStack.EMPTY;
		}
		Predicate<ItemStack> supported = projectileWeapon.getAllSupportedProjectiles();
		List<ItemStack> contents = holder.contents();
		ItemStack selected = contents.get(holder.selected());
		if (!selected.isEmpty() && supported.test(selected)) {
			return selected;
		}
		return contents.stream().filter(stack -> !stack.isEmpty() && supported.test(stack)).findFirst().orElse(ItemStack.EMPTY);
	}

	/**
	 * Wirft den Ball aus einem Platz des Gürtels, ohne dass das Inventar offen sein muss. Der Platz ist danach leer,
	 * ein leerer Ball bleibt wie beim normalen Wurf nach dem Freilassen am Boden liegen.
	 */
	public static void throwBall(final ServerPlayer player, final int slot) {
		if (slot < 0 || slot >= CONTENT_SIZE || player.isSpectator() || !(holderOf(player).wornItem() instanceof BeltItem)) {
			return;
		}
		BeltContentContainer content = new BeltContentContainer(player);
		ItemStack stack = content.getItem(slot);
		if (!Pokeballs.isFilled(stack) || !(stack.getItem() instanceof PokeballItem ball)) {
			return;
		}
		ball.throwBall(player.level(), player, stack);
		content.removeItemNoUpdate(slot);
	}

	@Override
	public void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ThrowBeltBallPayload.TYPE, ThrowBeltBallPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectBeltSlotPayload.TYPE, SelectBeltSlotPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ThrowBeltBallPayload.TYPE,
			(payload, context) -> throwBall(context.player(), payload.slot()));
		ServerPlayNetworking.registerGlobalReceiver(SelectBeltSlotPayload.TYPE,
			(payload, context) -> holderOf(context.player()).select(payload.slot()));
		Belts.load();
		ResourceKey<CreativeModeTab> tools = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));
		CreativeModeTabEvents.modifyOutputEvent(tools).register(output -> {
			output.accept(Belts.BELT);
			output.accept(Belts.QUIVER);
		});
	}
}
