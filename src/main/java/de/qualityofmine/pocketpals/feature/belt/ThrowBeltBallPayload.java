package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client an Server: Wirf den Ball aus diesem Platz des Gürtels (0 bis 5). */
public record ThrowBeltBallPayload(int slot) implements CustomPacketPayload {
	public static final Type<ThrowBeltBallPayload> TYPE = new Type<>(PocketPals.id("throw_belt_ball"));
	public static final StreamCodec<ByteBuf, ThrowBeltBallPayload> STREAM_CODEC =
		ByteBufCodecs.VAR_INT.map(ThrowBeltBallPayload::new, ThrowBeltBallPayload::slot);

	@Override
	public Type<ThrowBeltBallPayload> type() {
		return TYPE;
	}
}
