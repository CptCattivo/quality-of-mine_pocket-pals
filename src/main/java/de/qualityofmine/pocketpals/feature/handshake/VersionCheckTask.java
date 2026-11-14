package de.qualityofmine.pocketpals.feature.handshake;

import de.qualityofmine.pocketpals.PocketPals;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;

/** Hält den Verbindungsaufbau an, bis der Client seine Version des Mods gemeldet hat. */
final class VersionCheckTask implements ConfigurationTask {
	static final Type TYPE = new Type(PocketPals.MOD_ID + ":version_check");

	private final String version;

	VersionCheckTask(final String version) {
		this.version = version;
	}

	@Override
	public void start(final Consumer<Packet<?>> connection) {
		connection.accept(ServerConfigurationNetworking.createClientboundPacket(new ModVersionPayload(this.version)));
	}

	@Override
	public Type type() {
		return TYPE;
	}
}
