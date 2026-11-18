package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client an Server: Benutze den Inhalt dieses Platzes (0 bis 5), also Ball werfen, Trank werfen oder anfangen zu trinken. */
public record UseBeltSlotPayload(int slot) implements CustomPacketPayload {
	public static final Type<UseBeltSlotPayload> TYPE = new Type<>(PocketPals.id("use_belt_slot"));
	public static final StreamCodec<ByteBuf, UseBeltSlotPayload> STREAM_CODEC =
		ByteBufCodecs.VAR_INT.map(UseBeltSlotPayload::new, UseBeltSlotPayload::slot);

	@Override
	public Type<UseBeltSlotPayload> type() {
		return TYPE;
	}
}
