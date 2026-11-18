package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.Feature;
import de.qualityofmine.pocketpals.PocketPals;
import de.qualityofmine.pocketpals.mixin.AbstractContainerMenuAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Gürtel, Köcher und Trankgurt mit je 6 Plätzen. Alle werden auf demselben Platz im Inventar getragen, neben den
 * Rüstungsplätzen, und zwar immer nur einer davon. Der Gürtel trägt Pokébälle mit gefangenem Tier, der Köcher Pfeile und
 * der Trankgurt Tränke.
 */
public final class BeltFeature implements Feature {
	public static final int CONTENT_SIZE = 6;
	/** Positionen im Inventar-Menü. Der Client zeichnet die Hintergründe passend dazu. */
	public static final int EQUIP_X = 77;
	public static final int EQUIP_Y = 44;
	public static final int CONTENT_X = 8;
	public static final int CONTENT_Y = 167;
	public static final int CONTENT_STEP = 18;
	/** In anderen Menüs liegen die 6 Plätze so weit unter der Item-Leiste wie im Inventar, dort steht sie auf der Höhe 142. */
	private static final int CONTENT_BELOW_HOTBAR = CONTENT_Y - 142;
	/** Die Plätze 0 bis 8 der Item-Leiste und 40 für die zweite Hand, erreichbar per Zahlentaste bzw. F im Inventar. */
	private static final int HOTBAR_SIZE = 9;
	private static final int OFFHAND_SLOT = 40;

	private static final AttachmentType<BeltHolder> HOLDER = AttachmentRegistry.<BeltHolder>builder()
		.initializer(BeltHolder::new)
		.persistent(BeltHolder.CODEC)
		.copyOnDeath()
		.buildAndRegister(PocketPals.id("belt"));

	public static BeltHolder holderOf(final Player player) {
		return player.getAttachedOrCreate(HOLDER);
	}

	/** Der angelegte Gürtel, Köcher oder Trankgurt samt Inhalt, leer ohne. */
	public static ItemStack beltOf(final Player player) {
		return holderOf(player).get();
	}

	/** Erst der Platz für Gürtel, Köcher oder Trankgurt, danach die 6 Plätze darin. */
	public static List<Slot> createSlots(final Player player) {
		List<Slot> slots = new ArrayList<>();
		slots.add(new BeltEquipSlot(new BeltEquipContainer(player)));
		BeltContentContainer content = new BeltContentContainer(player);
		for (int i = 0; i < CONTENT_SIZE; i++) {
			slots.add(new BeltContentSlot(player, content, i, CONTENT_X, CONTENT_Y));
		}
		return slots;
	}

	/**
	 * Gibt einem anderen Menü mit Spielerinventar, etwa Truhe, Braustand oder Ofen, die 6 Plätze unter seiner Item-Leiste.
	 * Server und Client rufen das auf, nachdem das Menü fertig gebaut ist. So stehen die Plätze auf beiden Seiten am Ende,
	 * und die festen Slot-Nummern des Menüs bleiben gleich. Das Inventar hat seine Plätze schon, ein zweiter Aufruf ändert nichts.
	 */
	public static void addContentSlots(final AbstractContainerMenu menu) {
		if (menu instanceof InventoryMenu || menu.slots.stream().anyMatch(BeltFeature::isBeltSlot)) {
			return;
		}
		Slot firstHotbarSlot = menu.slots.stream()
			.filter(slot -> slot.container instanceof Inventory && slot.getContainerSlot() == 0)
			.findFirst().orElse(null);
		if (firstHotbarSlot == null) {
			return;
		}
		Player player = ((Inventory) firstHotbarSlot.container).player;
		BeltContentContainer content = new BeltContentContainer(player);
		AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) menu;
		for (int i = 0; i < CONTENT_SIZE; i++) {
			accessor.pocketpals$addSlot(new BeltContentSlot(player, content, i, firstHotbarSlot.x, firstHotbarSlot.y + CONTENT_BELOW_HOTBAR));
		}
	}

	/** Wie viele Plätze das Menü selbst hat, etwa die Truhe. Sie stehen vor dem Spielerinventar. */
	public static int containerSlotCount(final List<Slot> slots) {
		for (int i = 0; i < slots.size(); i++) {
			if (slots.get(i).container instanceof Inventory || isBeltSlot(slots.get(i))) {
				return i;
			}
		}
		return slots.size();
	}

	/** Beim Einsammeln gleicher Items per Doppelklick darf der Gürtel nicht geleert werden. */
	public static boolean isBeltSlot(final Slot slot) {
		return slot instanceof BeltEquipSlot || slot instanceof BeltContentSlot;
	}

	/**
	 * Gürtel, Köcher und Trankgurt fallen beim Tod mit ihrem Inhalt zu Boden wie die Rüstung, außer die Spielregel für das Behalten
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
	 * Legt einen Gegenstand in Gürtel, Köcher oder Trankgurt, sofern er dort hineinpasst: erst auf gleiche Stapel, dann in freie Plätze.
	 * Was keinen Platz findet, bleibt in {@code stack}. Gibt true zurück, wenn etwas hineingelegt wurde.
	 */
	public static boolean insert(final Player player, final ItemStack stack) {
		BeltHolder holder = holderOf(player);
		WearableItem worn = holder.wornItem();
		if (worn == null || stack.isEmpty() || !worn.accepts(stack)) {
			return false;
		}
		int before = stack.getCount();
		int limit = worn.contentStackSize();
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

	/** Benutzt den Inhalt eines Platzes, ohne dass das Inventar offen sein muss. Was dabei passiert, bestimmt das getragene Item. */
	public static void useSlot(final ServerPlayer player, final int slot) {
		BeltHolder holder = holderOf(player);
		WearableItem worn = holder.wornItem();
		if (slot < 0 || slot >= CONTENT_SIZE || player.isSpectator() || worn == null) {
			return;
		}
		worn.useContent(player, holder, slot);
	}

	/**
	 * Zahlentaste oder F über einem Platz im Trankgurt: Ein Stapel über der normalen Größe, etwa 9 Tränke, passt nicht auf
	 * einen Inventarplatz. Dann wandert nur so viel, wie normal auf einen Platz passt, und nur auf einen freien. Gibt true
	 * zurück, wenn der Tausch damit erledigt ist und das Spiel ihn nicht mehr selbst ausführen darf.
	 */
	public static boolean swapOversizedStack(final Slot slot, final Inventory inventory, final int inventorySlot) {
		boolean validTarget = inventorySlot >= 0 && inventorySlot < HOTBAR_SIZE || inventorySlot == OFFHAND_SLOT;
		ItemStack content = slot.getItem();
		if (!(slot instanceof BeltContentSlot) || !validTarget || content.getCount() <= content.getMaxStackSize()) {
			return false;
		}
		if (inventory.getItem(inventorySlot).isEmpty()) {
			inventory.setItem(inventorySlot, slot.remove(content.getMaxStackSize()));
		}
		return true;
	}

	@Override
	public void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(UseBeltSlotPayload.TYPE, UseBeltSlotPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectBeltSlotPayload.TYPE, SelectBeltSlotPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StopDrinkingPayload.TYPE, StopDrinkingPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(UseBeltSlotPayload.TYPE,
			(payload, context) -> useSlot(context.player(), payload.slot()));
		ServerPlayNetworking.registerGlobalReceiver(SelectBeltSlotPayload.TYPE,
			(payload, context) -> holderOf(context.player()).select(payload.slot()));
		ServerPlayNetworking.registerGlobalReceiver(StopDrinkingPayload.TYPE,
			(payload, context) -> holderOf(context.player()).stopDrinking());
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(BandolierItem::tickDrinking));
		Belts.load();
		// Der Inhalt erscheint im Tooltip an derselben Stelle wie bei einer Shulkerkiste
		ItemComponentTooltipProviderRegistry.addAfter(DataComponents.CONTAINER, Belts.CONTENTS);
		ResourceKey<CreativeModeTab> tools = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));
		CreativeModeTabEvents.modifyOutputEvent(tools).register(output -> {
			output.accept(Belts.BELT);
			output.accept(Belts.QUIVER);
			output.accept(Belts.BANDOLIER);
		});
	}
}
