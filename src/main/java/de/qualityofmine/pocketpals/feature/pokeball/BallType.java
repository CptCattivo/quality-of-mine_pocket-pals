package de.qualityofmine.pocketpals.feature.pokeball;

/** Die drei Stufen des Balls. Jede hat eine höhere Grundchance auf den Fang. */
public enum BallType {
	POKEBALL("pokeball", 0.35),
	SUPERBALL("superball", 0.50),
	HYPERBALL("hyperball", 0.70);

	private final String id;
	private final double baseChance;

	BallType(final String id, final double baseChance) {
		this.id = id;
		this.baseChance = baseChance;
	}

	/** Kennung des leeren Balls. Der volle Ball heißt zusätzlich {@code _filled}. */
	public String id() {
		return id;
	}

	public double baseChance() {
		return baseChance;
	}
}
