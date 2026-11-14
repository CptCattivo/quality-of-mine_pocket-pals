package de.qualityofmine.pocketpals.feature.handshake;

import de.qualityofmine.pocketpals.PocketPals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server an Client beim Verbindungsaufbau: Diese Version des Mods läuft auf dem Server. */
public record ModVersionPayload(String version) implements CustomPacketPayload {
	public static final Type<ModVersionPayload> TYPE = new Type<>(PocketPals.id("pocketpals_version"));
	public static final StreamCodec<ByteBuf, ModVersionPayload> STREAM_CODEC =
		ByteBufCodecs.stringUtf8(ModHandshakeFeature.MAX_VERSION_LENGTH).map(ModVersionPayload::new, ModVersionPayload::version);

	@Override
	public Type<ModVersionPayload> type() {
		return TYPE;
	}
}
