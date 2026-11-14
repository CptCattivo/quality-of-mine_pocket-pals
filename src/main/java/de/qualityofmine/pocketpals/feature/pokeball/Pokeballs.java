package de.qualityofmine.pocketpals.feature.pokeball;

import de.qualityofmine.pocketpals.PocketPals;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Alles, was die Bälle im Spiel registrieren: je Stufe ein leerer und ein voller Ball, das Wurfgeschoss und die
 * Datenkomponente fürs gefangene Tier. Der volle Ball ist ein eigenes Item, damit er in keinem Rezept als leerer
 * Ball gilt und kein gefangenes Tier beim Aufwerten verloren geht.
 */
public final class Pokeballs {
	public static final DataComponentType<CaughtAnimal> CAUGHT_ANIMAL = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE, PocketPals.id("caught_animal"),
		DataComponentType.<CaughtAnimal>builder().persistent(CaughtAnimal.CODEC).networkSynchronized(CaughtAnimal.STREAM_CODEC).build());

	private static final Map<BallType, Item> EMPTY = new EnumMap<>(BallType.class);
	private static final Map<BallType, Item> FILLED = new EnumMap<>(BallType.class);

	public static final EntityType<ThrownPokeball> THROWN_POKEBALL = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		PocketPals.id("pokeball"),
		EntityType.Builder.<ThrownPokeball>of(ThrownPokeball::new, MobCategory.MISC)
			.sized(0.25F, 0.25F)
			.clientTrackingRange(4)
			// Jeden Tick senden, damit das Wackeln beim Fangen flüssig aussieht
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, PocketPals.id("pokeball"))));

	static {
		for (BallType type : BallType.values()) {
			EMPTY.put(type, registerItem(type.id(), new Item.Properties().stacksTo(16), type, false));
			// Jeder volle Ball enthält ein anderes Tier und stapelt deshalb nicht
			FILLED.put(type, registerItem(type.id() + "_filled", new Item.Properties().stacksTo(1), type, true));
		}
	}

	private Pokeballs() {
	}

	private static Item registerItem(final String path, final Item.Properties properties, final BallType type, final boolean filled) {
		Identifier id = PocketPals.id(path);
		return Registry.register(BuiltInRegistries.ITEM, id, new PokeballItem(properties.setId(ResourceKey.create(Registries.ITEM, id)), type, filled));
	}

	/** Lädt die Klasse und damit die Registrierungen. */
	public static void register() {
	}

	public static Item empty(final BallType type) {
		return EMPTY.get(type);
	}

	public static Item filledItem(final BallType type) {
		return FILLED.get(type);
	}

	/** Ein Ball mit dem Tier darin. */
	public static ItemStack filled(final BallType type, final CaughtAnimal animal) {
		ItemStack stack = new ItemStack(FILLED.get(type));
		stack.set(CAUGHT_ANIMAL, animal);
		return stack;
	}

	/** Gehört zu den vollen Bällen, also denen mit einem Tier darin. */
	public static boolean isFilled(final ItemStack stack) {
		return stack.getItem() instanceof PokeballItem ball && ball.isFilled();
	}

	/** Gehört zu den leeren Bällen. */
	public static boolean isEmpty(final ItemStack stack) {
		return stack.getItem() instanceof PokeballItem ball && !ball.isFilled();
	}
}
