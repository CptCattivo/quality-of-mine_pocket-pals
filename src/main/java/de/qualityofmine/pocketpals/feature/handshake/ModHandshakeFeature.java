package de.qualityofmine.pocketpals.feature.handshake;

import de.qualityofmine.pocketpals.Feature;
import de.qualityofmine.pocketpals.PocketPals;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.chat.Component;

/**
 * Der Mod ändert Menüs und Inventar. Ein Client ohne den Mod oder mit einer anderen Version würde sich
 * mit dem Server nicht verstehen und abstürzen. Deshalb lässt der Server nur Spieler mit derselben Version
 * ein. Die Meldung ist bewusst kein Übersetzungsschlüssel, denn ein Client ohne den Mod kennt ihn nicht.
 */
public final class ModHandshakeFeature implements Feature {
	static final int MAX_VERSION_LENGTH = 64;

	static Component missingMessage(final String serverVersion) {
		return Component.literal("Dieser Server benötigt die Mod Pocket Pals " + serverVersion
			+ ".\nThis server requires the Pocket Pals mod " + serverVersion + ".");
	}

	static Component wrongVersionMessage(final String serverVersion, final String clientVersion) {
		return Component.literal("Dieser Server benötigt Pocket Pals " + serverVersion + ", installiert ist " + clientVersion
			+ ".\nThis server requires Pocket Pals " + serverVersion + ", but " + clientVersion + " is installed.");
	}

	@Override
	public void initialize() {
		String version = PocketPals.version();
		PayloadTypeRegistry.clientboundConfiguration().register(ModVersionPayload.TYPE, ModVersionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundConfiguration().register(ModVersionReplyPayload.TYPE, ModVersionReplyPayload.STREAM_CODEC);

		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (ServerConfigurationNetworking.canSend(handler, ModVersionPayload.TYPE)) {
				handler.addTask(new VersionCheckTask(version));
			} else {
				handler.disconnect(missingMessage(version));
			}
		});
		ServerConfigurationNetworking.registerGlobalReceiver(ModVersionReplyPayload.TYPE, (payload, context) -> {
			if (version.equals(payload.version())) {
				context.packetListener().completeTask(VersionCheckTask.TYPE);
			} else {
				context.packetListener().disconnect(wrongVersionMessage(version, payload.version()));
			}
		});
	}
}
