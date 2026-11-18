package de.qualityofmine.pocketpals.feature.belt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * Der Inhalt von Gürtel, Köcher oder Trankgurt, gespeichert im Item selbst.
 * <p>
 * Der Behälter des Spiels ({@code minecraft:container}) verwirft beim Laden jeden Stapel, der größer ist als die normale
 * Stapelgröße. Im Trankgurt liegen aber bis zu 9 Tränke auf einem Platz, normal stapeln sie gar nicht. Deshalb gibt es
 * diese eigene Komponente, die Stapel unverändert speichert.
 */
public record BeltContents(List<Entry> entries) implements TooltipProvider {
	public static final BeltContents EMPTY = new BeltContents(List.of());
	static final Codec<BeltContents> CODEC = Entry.CODEC.listOf(0, BeltFeature.CONTENT_SIZE).xmap(BeltContents::new, BeltContents::entries);
	static final StreamCodec<RegistryFriendlyByteBuf, BeltContents> STREAM_CODEC =
		Entry.STREAM_CODEC.apply(ByteBufCodecs.list(BeltFeature.CONTENT_SIZE)).map(BeltContents::new, BeltContents::entries);
	/** So viele Plätze nennt der Tooltip einzeln, wie beim Behälter des Spiels. */
	private static final int TOOLTIP_LINES = 5;

	/** Ein belegter Platz mit seinem Stapel. */
	public record Entry(int slot, ItemStackTemplate item) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.intRange(0, BeltFeature.CONTENT_SIZE - 1).fieldOf("slot").forGetter(Entry::slot),
				ItemStackTemplate.CODEC.fieldOf("item").forGetter(Entry::item))
			.apply(i, Entry::new));
		static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Entry::slot,
			ItemStackTemplate.STREAM_CODEC, Entry::item,
			Entry::new);

		/** Baut den Stapel ohne die Prüfung der Stapelgröße, die {@link ItemStackTemplate#create()} macht. */
		ItemStack toStack() {
			return new ItemStack(item.item(), item.count(), item.components());
		}
	}

	public BeltContents {
		entries = List.copyOf(entries);
	}

	public static BeltContents of(final List<ItemStack> stacks) {
		List<Entry> entries = new ArrayList<>();
		for (int i = 0; i < stacks.size(); i++) {
			if (!stacks.get(i).isEmpty()) {
				entries.add(new Entry(i, ItemStackTemplate.fromNonEmptyStack(stacks.get(i))));
			}
		}
		return entries.isEmpty() ? EMPTY : new BeltContents(entries);
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/** Schreibt den Inhalt in eine Liste mit den 6 Plätzen. Plätze außerhalb der Liste werden übergangen. */
	public void copyInto(final NonNullList<ItemStack> target) {
		for (Entry entry : entries) {
			if (entry.slot() < target.size()) {
				target.set(entry.slot(), entry.toStack());
			}
		}
	}

	/** Wie beim Behälter des Spiels: Anzahl und Name der ersten Plätze, danach die Zahl der übrigen. */
	@Override
	public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag,
		final DataComponentGetter components) {
		entries.stream().limit(TOOLTIP_LINES).forEach(entry -> consumer.accept(
			Component.translatable("item.container.item_count", entry.item().count(), entry.toStack().getHoverName())));
		if (entries.size() > TOOLTIP_LINES) {
			consumer.accept(Component.translatable("item.container.more_items", entries.size() - TOOLTIP_LINES).withStyle(ChatFormatting.ITALIC));
		}
	}
}
