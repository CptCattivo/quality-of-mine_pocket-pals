package de.qualityofmine.pocketpals.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Zugriff auf das Hinzufügen von Plätzen, damit Gürtel, Köcher und Trankgurt auch in fertig gebauten Menüs erscheinen. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
	@Invoker("addSlot")
	Slot pocketpals$addSlot(Slot slot);
}
