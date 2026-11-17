package de.qualityofmine.pocketpals;

import de.qualityofmine.pocketpals.Feature;
import de.qualityofmine.pocketpals.feature.belt.BeltClientFeature;
import de.qualityofmine.pocketpals.feature.handshake.ModHandshakeClientFeature;
import de.qualityofmine.pocketpals.feature.pokeball.PokeballClientFeature;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;

public final class PocketPalsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		List<Feature> features = List.of(
			new ModHandshakeClientFeature(),
			new PokeballClientFeature(),
			new BeltClientFeature()
		);
		features.forEach(Feature::initialize);
	}
}
