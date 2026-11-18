package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltClientFeature;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
	/** Beim Trinken aus dem Trankgurt geht der Spieler so langsam wie beim Trinken aus der Hand. */
	@Inject(method = "modifyInput", at = @At("RETURN"), cancellable = true)
	private void pocketpals$slowWhileDrinking(final Vec2 input, final CallbackInfoReturnable<Vec2> cir) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (BeltClientFeature.isDrinking(player) && !player.isPassenger()) {
			cir.setReturnValue(cir.getReturnValue().scale(BeltClientFeature.drinkingSpeedMultiplier(player)));
		}
	}

	/** Beim Trinken aus dem Trankgurt kann der Spieler nicht sprinten, wie beim Trinken aus der Hand. */
	@Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
	private void pocketpals$noSprintWhileDrinking(final CallbackInfoReturnable<Boolean> cir) {
		if (BeltClientFeature.isDrinking((LocalPlayer) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
