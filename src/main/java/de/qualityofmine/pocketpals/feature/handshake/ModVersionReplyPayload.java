package de.qualityofmine.pocketpals.feature.handshake;

import de.qualityofmine.pocketpals.PocketPals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client an Server beim Verbindungsaufbau: Diese Version des Mods ist auf dem Client installiert. */
public record ModVersionReplyPayload(String version) implements CustomPacketPayload {
	public static final Type<ModVersionReplyPayload> TYPE = new Type<>(PocketPals.id("pocketpals_version_reply"));
	public static final StreamCodec<ByteBuf, ModVersionReplyPayload> STREAM_CODEC =
		ByteBufCodecs.stringUtf8(ModHandshakeFeature.MAX_VERSION_LENGTH).map(ModVersionReplyPayload::new, ModVersionReplyPayload::version);

	@Override
	public Type<ModVersionReplyPayload> type() {
		return TYPE;
	}
}
