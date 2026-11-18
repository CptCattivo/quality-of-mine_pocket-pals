package de.qualityofmine.pocketpals.feature.pokeball;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Das Tier im Pokéball. Es steckt als komplette Entity-Daten im Item, also mit Leben, Alter, Name, Sattel und allem
 * anderen, und kommt beim Freilassen genau so wieder heraus.
 */
public record CaughtAnimal(Identifier type, CompoundTag data) {
	public static final Codec<CaughtAnimal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.fieldOf("type").forGetter(CaughtAnimal::type),
		CompoundTag.CODEC.fieldOf("data").forGetter(CaughtAnimal::data)
	).apply(instance, CaughtAnimal::new));

	public static final StreamCodec<ByteBuf, CaughtAnimal> STREAM_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, CaughtAnimal::type,
		ByteBufCodecs.COMPOUND_TAG, CaughtAnimal::data,
		CaughtAnimal::new
	);

	/** Nimmt das Tier auf. Das Tier selbst bleibt unverändert in der Welt und muss vom Aufrufer entfernt werden. */
	public static CaughtAnimal of(final Mob animal) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, animal.registryAccess());
		animal.saveWithoutId(output);
		return new CaughtAnimal(EntityType.getKey(animal.getType()), output.buildResult());
	}

	/** Der Name des Tiers, zum Beispiel „Schaf“. */
	public Component displayName() {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(type)
			.map(EntityType::getDescription)
			.orElseGet(() -> Component.literal(type.toString()));
	}

	/** Setzt das Tier an die Position in die Welt. Leer, falls die Daten nicht mehr zu einem Tier passen. */
	public Optional<Entity> release(final ServerLevel level, final Vec3 position, final float yRot) {
		Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(type);
		if (entityType.isEmpty()) {
			return Optional.empty();
		}
		// Neue UUID, damit das Tier nie mit seinem Original kollidiert, falls dieses noch nicht ganz entfernt ist
		CompoundTag copy = data.copy();
		copy.putIntArray("UUID", UUIDUtil.uuidToIntArray(UUID.randomUUID()));
		Optional<Entity> entity = EntityType.create(entityType.get(),
			TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), copy), level, EntitySpawnReason.TRIGGERED);
		entity.ifPresent(created -> {
			created.snapTo(position.x, position.y, position.z, yRot, 0.0F);
			level.addFreshEntity(created);
		});
		return entity;
	}
}
