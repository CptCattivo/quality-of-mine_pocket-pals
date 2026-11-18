package de.qualityofmine.pocketpals.feature.pokeball;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Welche Lebewesen sich mit einem Ball fangen lassen. Die Regel steht nur hier, Werfen und Rechtsklick fragen beide hier nach.
 * <p>
 * Fangbar sind diese Zweige des Spiels:
 * <ul>
 * <li>Landtiere samt Nautilus und Axolotl ({@link Animal})</li>
 * <li>Delfine und Tintenfische ({@link AgeableWaterCreature})</li>
 * <li>Fische und Kaulquappen ({@link WaterAnimal})</li>
 * <li>Fledermäuse ({@link AmbientCreature})</li>
 * <li>Eisen-, Schnee- und Kupfergolem ({@link AbstractGolem})</li>
 * <li>der Hilfsgeist ({@link Allay})</li>
 * </ul>
 * Feinde sind nie fangbar, auch wenn das Spiel sie in einem dieser Zweige führt, etwa Hoglin (Tier) und Shulker (Golem).
 * Dorfbewohner und Händler gehören zu keinem Zweig und bleiben bewusst außen vor.
 */
public final class Catchables {
	private Catchables() {
	}

	public static boolean isCatchable(final Entity entity) {
		// Der Zombie-Nautilus ist ein Feind, ist im Spiel aber nicht als solcher markiert
		if (entity instanceof Enemy || entity instanceof ZombieNautilus) {
			return false;
		}
		return entity instanceof Animal || entity instanceof AgeableWaterCreature || entity instanceof WaterAnimal
			|| entity instanceof AmbientCreature || entity instanceof AbstractGolem || entity instanceof Allay;
	}
}
