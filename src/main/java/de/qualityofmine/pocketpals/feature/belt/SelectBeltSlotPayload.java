package de.qualityofmine.pocketpals.feature.belt;

import de.qualityofmine.pocketpals.PocketPals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client an Server: Dieser Platz in Gürtel oder Köcher ist gewählt (0 bis 5). Der Bogen verschießt den Pfeil daraus. */
public record SelectBeltSlotPayload(int slot) implements CustomPacketPayload {
	public static final Type<SelectBeltSlotPayload> TYPE = new Type<>(PocketPals.id("select_belt_slot"));
	public static final StreamCodec<ByteBuf, SelectBeltSlotPayload> STREAM_CODEC =
		ByteBufCodecs.VAR_INT.map(SelectBeltSlotPayload::new, SelectBeltSlotPayload::slot);

	@Override
	public Type<SelectBeltSlotPayload> type() {
		return TYPE;
	}
}
