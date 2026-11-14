package de.qualityofmine.pocketpals.feature.pokeball;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CatchChanceTest {
	private static final double TOLERANCE = 1e-9;

	@Test
	void healthyAdultHasBaseChance() {
		assertEquals(BallType.POKEBALL.baseChance(), CatchChance.of(BallType.POKEBALL, 10.0F, 10.0F, false), TOLERANCE);
	}

	@Test
	void chanceRisesAsHealthDrops() {
		double full = CatchChance.of(BallType.POKEBALL, 10.0F, 10.0F, false);
		double half = CatchChance.of(BallType.POKEBALL, 5.0F, 10.0F, false);
		double nearlyDead = CatchChance.of(BallType.POKEBALL, 1.0F, 10.0F, false);
		assertTrue(full < half && half < nearlyDead);
		assertEquals(BallType.POKEBALL.baseChance() + CatchChance.MAX_HEALTH_BONUS * 0.5, half, TOLERANCE);
	}

	@Test
	void babiesAreEasier() {
		assertEquals(CatchChance.of(BallType.POKEBALL, 10.0F, 10.0F, false) + CatchChance.BABY_BONUS, CatchChance.of(BallType.POKEBALL, 10.0F, 10.0F, true), TOLERANCE);
	}

	@Test
	void chanceStaysWithinBounds() {
		assertEquals(CatchChance.MAX, CatchChance.of(BallType.POKEBALL, 0.0F, 10.0F, true), TOLERANCE);
		assertEquals(BallType.POKEBALL.baseChance(), CatchChance.of(BallType.POKEBALL, 50.0F, 10.0F, false), TOLERANCE, "Mehr Leben als maximal zählt als volles Leben");
	}

	@Test
	void betterBallsCatchMoreOften() {
		double pokeball = CatchChance.of(BallType.POKEBALL, 10.0F, 10.0F, false);
		double superball = CatchChance.of(BallType.SUPERBALL, 10.0F, 10.0F, false);
		double hyperball = CatchChance.of(BallType.HYPERBALL, 10.0F, 10.0F, false);
		assertTrue(pokeball < superball && superball < hyperball);
		assertEquals(0.50, superball, TOLERANCE);
		assertEquals(0.70, hyperball, TOLERANCE);
	}

	@Test
	void zeroMaxHealthDoesNotBreak() {
		assertEquals(BallType.POKEBALL.baseChance(), CatchChance.of(BallType.POKEBALL, 0.0F, 0.0F, false), TOLERANCE);
	}
}
