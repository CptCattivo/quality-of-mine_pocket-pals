package de.qualityofmine.pocketpals;

import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import de.qualityofmine.pocketpals.feature.handshake.ModHandshakeFeature;
import de.qualityofmine.pocketpals.feature.pokeball.PokeballFeature;

/**
 * Pocket Pals: Bälle zum Fangen von Tieren und Gürtel dafür. Gehört zur Mod-Reihe „Quality of Mine“, die jedes Feature als eigene Mod anbietet.
 * Alle Inhalte stehen im Namensraum {@code qualityofmine}, damit bestehende Welten ihre Daten behalten.
 */
public final class PocketPals implements ModInitializer {
	public static final String MOD_ID = "pocketpals";
	/** Der Namensraum aller Kennungen, Texte und Dateien. Er ist für die ganze Reihe derselbe. */
	public static final String NAMESPACE = "qualityofmine";

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}

	/** Die Version aus fabric.mod.json, also die aus gradle.properties. */
	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString();
	}

	@Override
	public void onInitialize() {
		List<Feature> features = List.of(
			new ModHandshakeFeature(),
			new PokeballFeature(),
			new BeltFeature()
		);
		features.forEach(Feature::initialize);
	}
}
