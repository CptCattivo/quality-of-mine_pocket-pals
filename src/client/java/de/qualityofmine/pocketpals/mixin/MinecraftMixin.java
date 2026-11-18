package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltClientFeature;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	/** Die Pause, bis gehaltener Rechtsklick erneut wirkt. Das Spiel setzt sie erst nach unserem Eingriff, deshalb setzen wir sie selbst. */
	private static final int RIGHT_CLICK_DELAY = 4;

	@Shadow
	private int rightClickDelay;

	/**
	 * Alt + Rechtsklick benutzt den gewählten Platz in Gürtel oder Trankgurt, statt das Item in der Hand zu benutzen. Wie beim
	 * normalen Rechtsklick wiederholt sich das beim Halten nur alle 4 Ticks.
	 */
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void pocketpals$useFromBelt(final CallbackInfo ci) {
		if (BeltClientFeature.handleUse()) {
			this.rightClickDelay = RIGHT_CLICK_DELAY;
			ci.cancel();
		}
	}
}
