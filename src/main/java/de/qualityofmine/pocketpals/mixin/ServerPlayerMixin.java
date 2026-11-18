package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
	/**
	 * Jedes Menü, das der Server öffnet, läuft hier durch, bevor sein Inhalt zum Client geht: Truhen, Braustand, Ofen,
	 * aber auch Pferde- und Nautilus-Inventar. Der Client ergänzt die Plätze beim Bau des Bildschirms genauso.
	 */
	@Inject(method = "initMenu", at = @At("HEAD"))
	private void pocketpals$addBeltSlots(final AbstractContainerMenu menu, final CallbackInfo ci) {
		BeltFeature.addContentSlots(menu);
	}
}
