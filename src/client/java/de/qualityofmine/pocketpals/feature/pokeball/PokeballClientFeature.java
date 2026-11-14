package de.qualityofmine.pocketpals.feature.pokeball;

import de.qualityofmine.pocketpals.Feature;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Der geworfene Ball wird wie ein Ei als sein Item gezeichnet, mit dem Modell von leerem oder vollem Ball. */
public final class PokeballClientFeature implements Feature {
	@Override
	public void initialize() {
		EntityRendererRegistry.register(Pokeballs.THROWN_POKEBALL, ThrownItemRenderer::new);
	}
}
