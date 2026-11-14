package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Die Registrierung des Gürtels. */
public final class Belts {
	public static final Item BELT = register("belt");

	private Belts() {
	}

	private static Item register(final String path) {
		Identifier id = PocketPals.id(path);
		return Registry.register(BuiltInRegistries.ITEM, id,
			new BeltItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1)));
	}

	/** Lädt die Klasse und damit die Registrierung. */
	static void load() {
	}
}
