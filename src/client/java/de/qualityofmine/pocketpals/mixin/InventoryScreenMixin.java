package de.qualityofmine.pocketpals.mixin;

import de.qualityofmine.pocketpals.feature.belt.BeltPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
	private InventoryScreenMixin(final InventoryMenu menu, final RecipeBookComponent<?> recipeBook, final Inventory inventory, final Component title) {
		super(menu, recipeBook, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void pocketpals$extractBeltPanel(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a, final CallbackInfo ci) {
		BeltPanel.extract(graphics, this.leftPos, this.topPos);
	}
}
