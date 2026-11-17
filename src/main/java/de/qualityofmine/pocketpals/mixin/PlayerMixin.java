package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerMixin {
	/** Gürtel und Köcher fallen beim Tod wie die Rüstung zu Boden. */
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void pocketpals$dropBelt(final ServerLevel level, final CallbackInfo ci) {
		BeltFeature.dropOnDeath((Player) (Object) this, level);
	}

	/** Bogen und Armbrust nehmen zuerst den gewählten Pfeil aus dem Köcher, erst danach sucht das Spiel wie gewohnt. */
	@Inject(method = "getProjectile", at = @At("HEAD"), cancellable = true)
	private void pocketpals$useQuiver(final ItemStack weapon, final CallbackInfoReturnable<ItemStack> cir) {
		ItemStack arrow = BeltFeature.quiverProjectile((Player) (Object) this, weapon);
		if (!arrow.isEmpty()) {
			cir.setReturnValue(arrow);
		}
	}
}
