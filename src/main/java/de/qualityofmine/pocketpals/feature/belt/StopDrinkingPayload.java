package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client an Server: Die Taste ist losgelassen, das Trinken aus dem Trankgurt bricht ab. */
public record StopDrinkingPayload() implements CustomPacketPayload {
	public static final StopDrinkingPayload INSTANCE = new StopDrinkingPayload();
	public static final Type<StopDrinkingPayload> TYPE = new Type<>(PocketPals.id("stop_drinking"));
	public static final StreamCodec<ByteBuf, StopDrinkingPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<StopDrinkingPayload> type() {
		return TYPE;
	}
}
