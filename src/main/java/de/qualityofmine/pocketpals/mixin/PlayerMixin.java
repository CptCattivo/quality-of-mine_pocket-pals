package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerMixin {
	/** Der Gürtel fällt beim Tod wie die Rüstung zu Boden. */
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void pocketpals$dropBelt(final ServerLevel level, final CallbackInfo ci) {
		BeltFeature.dropOnDeath((Player) (Object) this, level);
	}
}
