package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltClientFeature;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	/** Alt + Rechtsklick wirft den gewählten Ball aus dem Gürtel, statt das Item in der Hand zu benutzen. */
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void pocketpals$throwFromBelt(final CallbackInfo ci) {
		if (BeltClientFeature.handleUse()) {
			ci.cancel();
		}
	}
}
