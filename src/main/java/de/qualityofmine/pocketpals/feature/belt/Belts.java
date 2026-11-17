package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Die Registrierung von Gürtel und Köcher. */
public final class Belts {
	public static final Item BELT = register("belt", BeltItem::new);
	public static final Item QUIVER = register("quiver", QuiverItem::new);

	private Belts() {
	}

	private static Item register(final String path, final Function<Item.Properties, Item> factory) {
		Identifier id = PocketPals.id(path);
		return Registry.register(BuiltInRegistries.ITEM, id,
			factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1)));
	}

	/** Lädt die Klasse und damit die Registrierung. */
	static void load() {
	}
}
