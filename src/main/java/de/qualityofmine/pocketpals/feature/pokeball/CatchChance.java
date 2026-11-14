package de.qualityofmine.pocketpals.feature.pokeball;

/**
 * Die Fangchance eines Balls. Wie im Spiel gilt: Je schwächer das Tier, desto leichter ist es zu fangen.
 * Wer ein Tier vorher verletzt, erhöht also seine Chance. Bessere Bälle starten mit einer höheren Grundchance.
 */
public final class CatchChance {
	static final double MAX_HEALTH_BONUS = 0.5;
	static final double BABY_BONUS = 0.1;
	static final double MIN = 0.05;
	static final double MAX = 0.95;

	private CatchChance() {
	}

	/** Wahrscheinlichkeit zwischen 0,05 und 0,95, dass der Fang gelingt. */
	public static double of(final BallType ball, final float health, final float maxHealth, final boolean baby) {
		double missingHealth = maxHealth <= 0.0F ? 0.0 : 1.0 - Math.clamp(health / maxHealth, 0.0F, 1.0F);
		double chance = ball.baseChance() + MAX_HEALTH_BONUS * missingHealth + (baby ? BABY_BONUS : 0.0);
		return Math.clamp(chance, MIN, MAX);
	}
}
