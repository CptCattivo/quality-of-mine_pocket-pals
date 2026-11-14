package de.qualityofmine.pocketpals.feature.pokeball;

import de.qualityofmine.pocketpals.Feature;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Pokéball zum Fangen und Freilassen von Tieren. Rezept, Modelle und Texte stehen als Dateien in den Ressourcen. */
public final class PokeballFeature implements Feature {
	@Override
	public void initialize() {
		Pokeballs.register();
		ResourceKey<net.minecraft.world.item.CreativeModeTab> tools =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));
		CreativeModeTabEvents.modifyOutputEvent(tools).register(output -> {
			for (BallType type : BallType.values()) {
				output.accept(Pokeballs.empty(type));
			}
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			ItemStack ball = player.getItemInHand(hand);
			// Der Rechtsklick gilt dem Ball, auch bei Haustieren, die sonst sitzen oder aufsteigen würden
			if (player.isSpectator() || !Pokeballs.isEmpty(ball) || !(entity instanceof Animal animal)
				|| !ThrownPokeball.isKnown(animal) || !ThrownPokeball.canCapture(animal, player)) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel server) {
				ThrownPokeball.captureKnown(server, player, ball, animal);
				ball.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		});
	}
}
