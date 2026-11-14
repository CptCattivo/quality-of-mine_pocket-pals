package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.Feature;
import de.qualityofmine.pocketpals.PocketPals;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Der Gürtel mit 6 Plätzen für Pokébälle mit gefangenem Tier. Er wird auf einem eigenen Platz im Inventar getragen,
 * neben den Rüstungsplätzen.
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

	/** Der angelegte Gürtel, leer ohne Gürtel. */
	public static ItemStack beltOf(final Player player) {
		return holderOf(player).get();
	}

	/** Erst der Platz für den Gürtel, danach die 6 Plätze darin. */
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
	 * Der Gürtel fällt beim Tod mit seinem Inhalt zu Boden wie die Rüstung, außer die Spielregel für das Behalten
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

	@Override
	public void initialize() {
		Belts.load();
		ResourceKey<CreativeModeTab> tools = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));
		CreativeModeTabEvents.modifyOutputEvent(tools).register(output -> output.accept(Belts.BELT));
	}
}
