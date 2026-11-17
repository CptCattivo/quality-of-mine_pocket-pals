package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltClientFeature;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	/** Mit Alt wählt das Mausrad einen Platz im Gürtel statt in der Item-Leiste. */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void pocketpals$scrollBelt(final long handle, final double xOffset, final double yOffset, final CallbackInfo ci) {
		// Manche Systeme vertauschen die Achsen, solange Alt gedrückt ist
		double scroll = yOffset != 0.0 ? yOffset : xOffset;
		if (BeltClientFeature.handleScroll(scroll)) {
			ci.cancel();
		}
	}
}
