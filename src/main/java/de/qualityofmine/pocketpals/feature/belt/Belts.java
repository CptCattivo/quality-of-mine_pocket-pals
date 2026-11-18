package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Die Registrierung von Gürtel, Köcher und Trankgurt samt der Komponente für ihren Inhalt. */
public final class Belts {
	public static final DataComponentType<BeltContents> CONTENTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		PocketPals.id("belt_contents"),
		DataComponentType.<BeltContents>builder().persistent(BeltContents.CODEC).networkSynchronized(BeltContents.STREAM_CODEC).cacheEncoding().build());
	public static final Item BELT = register("belt", BeltItem::new);
	public static final Item QUIVER = register("quiver", QuiverItem::new);
	public static final Item BANDOLIER = register("bandolier", BandolierItem::new);

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
