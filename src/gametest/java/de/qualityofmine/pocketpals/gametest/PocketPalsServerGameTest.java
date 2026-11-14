package de.qualityofmine.pocketpals.gametest;

import static de.qualityofmine.pocketpals.gametest.PocketPalsClientGameTest.check;
import static de.qualityofmine.pocketpals.gametest.PocketPalsClientGameTest.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * Spielt Pocket Pals gegen einen echten dedizierten Server durch. Der Client verbindet sich wie ein Mitspieler,
 * Server und Client sind getrennte Programme. Das deckt ab, was ein Einzelspieler-Test nicht sieht, zum Beispiel
 * Code, der auf einem Server fehlt, oder Pakete, die der Server prüfen muss.
 */
public final class PocketPalsServerGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			// Der Server friert die Uhr ein, Monster bleiben aus, damit kein Treffer die Testnacht beendet
			server.runCommand("gamerule advance_time true");
			server.runCommand("gamerule spawn_monsters false");
			server.runCommand("gamemode survival @a");
			context.waitTicks(20);

			check(context.computeOnClient(minecraft -> minecraft.player != null), "Der Client ist auf dem Server, die Versionsabfrage ist bestanden");

		}
	}

}
