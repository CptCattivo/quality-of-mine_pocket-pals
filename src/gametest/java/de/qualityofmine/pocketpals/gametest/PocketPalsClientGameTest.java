package de.qualityofmine.pocketpals.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import de.qualityofmine.pocketpals.feature.belt.BeltClientFeature;
import de.qualityofmine.pocketpals.feature.belt.BeltFeature;
import de.qualityofmine.pocketpals.feature.belt.Belts;
import de.qualityofmine.pocketpals.feature.pokeball.BallType;
import de.qualityofmine.pocketpals.feature.pokeball.CaughtAnimal;
import de.qualityofmine.pocketpals.feature.pokeball.Pokeballs;
import de.qualityofmine.pocketpals.feature.pokeball.ThrownPokeball;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Spielt Pocket Pals im echten Spiel durch: Klicks und Tasten gehen über die normale Eingabe des Clients.
 * Start mit {@code gradlew runClientGameTest}, Screenshots landen unter {@code build/run/clientGameTest/screenshots}.
 */
public final class PocketPalsClientGameTest implements FabricClientGameTest {
		private static final Logger LOGGER = LogUtils.getLogger();
	private static final int HOTBAR_START = 36;
	private static final int TRASH_START = 46;
	private static final int TRASH_BIN = 49;
	/** Ein Punkt auf dem Panel neben dem Symbol. */
	private static final int PANEL_EDGE_X = 152;
	private static final int PANEL_EDGE_Y = 186;
	/** Mitte des Buttons „Nacht überspringen“, gemessen vom unteren Bildschirmrand. */
	private static final int SKIP_BUTTON_BOTTOM_OFFSET = 54;
	private static final int FIRST_EXTRA_SLOT = 9;
	private static final int LAST_EXTRA_SLOT = 10;
	private static final int MAX_TICKS_AFTER_SKIP = 100;
	private static final int IMAGE_WIDTH = 176;
	private static final int IMAGE_HEIGHT = 166;
	private static final double WALK_SPEED = 0.1;
	private static final double SPEED_TOLERANCE = 0.0001;
	/** Im Test laufen Client und Server im Gleichschritt. Eine kurze Restnacht reicht und spart Minuten. */
	private static final int LATE_NIGHT = 20000;
	/** Rund 3360 Ticks bis zum Wecken bei 200 Ticks pro Kartoffel, also knapp 17 Stück. */
	private static final int MIN_SMELTED_ITEMS = 15;
	private static final int EARLIEST_WAKE_UP = 23000;
	private static final int BREEDING_TIMEOUT_TICKS = 1200;
	private static final int REST_TIMEOUT_TICKS = 700;
	private static final int LURE_TICKS = 200;
	/** Länger als die Laufzeit des Effekts der Verzauberung, ohne Auffrischen wäre er dann weg. */
	private static final int NIGHT_VISION_REFRESH_CHECK = 450;

	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule advance_time true");
			server.runCommand("gamerule spawn_mobs true");
			server.runCommand("gamerule spawn_monsters false");
			server.runCommand("gamerule show_advancement_messages false");
			// Sonst heilt der volle Hungerbalken schneller als die Verzauberung
			server.runCommand("gamerule natural_health_regeneration false");
			server.runCommand("gamemode survival @a");
			context.waitTicks(20);

			testPokeball(context, server);
			testBallUpgrades(server);
			testRecaptureAndFlee(context, server);
			testWaterCreatures(context, server);
			testBelt(server);
			testBeltClick(context, server);
			testQuiver(context, server);
			testBandolier(context, server);
			testBeltInMenus(context, server);
			testBeltDeath(context, server);
		}
	}

	/** Wirft einen Pokéball vom Spieler aus auf eine Stelle. */
	private static void throwBall(final TestServerContext server, final ItemStack stack, final double targetX, final double targetY, final double targetZ) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ThrownPokeball ball = new ThrownPokeball(player.level(), player, stack);
			ball.setPos(player.getX(), player.getEyeY(), player.getZ());
			// Etwas höher gezielt, weil der Ball im Flug sinkt
			ball.shoot(targetX - player.getX(), targetY + 0.05 - player.getEyeY(), targetZ - player.getZ(), 1.5F, 0.0F);
			player.level().addFreshEntity(ball);
		});
	}

	private static AABB around(final ServerPlayer player) {
		return player.getBoundingBox().inflate(20.0);
	}

	private static boolean ballInFlight(final TestServerContext server) {
		return server.computeOnServer(s -> !player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty());
	}

	private static List<ItemEntity> ballItems(final TestServerContext server) {
		return server.computeOnServer(s -> player(s).level().getEntitiesOfClass(ItemEntity.class, around(player(s)),
			item -> item.getItem().getItem() instanceof de.qualityofmine.pocketpals.feature.pokeball.PokeballItem));
	}

	/** Fangen mit Animation, Namen im Item, Freilassen mit dem Tier im selben Zustand. */
	private static void testPokeball(final ClientGameTestContext context, final TestServerContext server) {
		flattenArea(server);
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:sheep]");
		server.waitFor(s -> player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).isEmpty(), 100);

		ItemStack filled = null;
		for (int attempt = 1; attempt <= 12 && filled == null; attempt++) {
			double[] target = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				Animal sheep = EntityTypes.SHEEP.create(player.level(), EntitySpawnReason.COMMAND);
				sheep.snapTo(player.getX() + 5, player.getY(), player.getZ(), 0.0F, 0.0F);
				player.level().addFreshEntity(sheep);
				// Fast tot, damit die Chance hoch ist und der Test selten wiederholen muss
				sheep.setHealth(1.0F);
				sheep.setNoAi(true);
				return new double[] {sheep.getX(), sheep.getY() + sheep.getBbHeight() / 2.0, sheep.getZ()};
			});
			context.waitTicks(2);
			throwBall(server, new ItemStack(Pokeballs.empty(BallType.POKEBALL)), target[0], target[1], target[2]);
			context.waitTicks(10);
			check(ballInFlight(server), "Der Ball ist nach dem Treffer noch da und zeigt die Fang-Animation");
			check(server.computeOnServer(s -> player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).isEmpty()),
				"Das getroffene Schaf ist im Ball verschwunden");
			server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 300);

			List<ItemEntity> items = ballItems(server);
			if (items.isEmpty()) {
				LOGGER.info("[QoM-Test] Versuch {}: Das Schaf ist ausgebrochen", attempt);
				check(server.computeOnServer(s -> player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).size() == 1),
					"Nach dem Ausbruch steht das Schaf wieder da");
				server.runCommand("kill @e[type=minecraft:sheep]");
				server.waitFor(s -> player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).isEmpty(), 100);
			} else {
				filled = server.computeOnServer(s -> items.getFirst().getItem().copy());
				LOGGER.info("[QoM-Test] Versuch {}: Schaf gefangen", attempt);
			}
		}
		check(filled != null, "Ein Schaf mit 1 Leben ist nach höchstens 12 Versuchen gefangen");
		ItemStack ball = filled;
		CaughtAnimal caught = ball.get(Pokeballs.CAUGHT_ANIMAL);
		check(caught != null && caught.type().getPath().equals("sheep"), "Der Ball enthält ein Schaf");
		check(ball.getMaxStackSize() == 1, "Ein voller Ball stapelt nicht");
		String name = context.computeOnClient(m -> ball.getHoverName().getString());
		LOGGER.info("[QoM-Test] Name des vollen Balls: {}", name);
		check(name.startsWith("Pokéball (") && name.endsWith(")"), "Das Tier steht in Klammern hinter dem Namen");

		// Aufheben und Freilassen: Der volle Ball landet auf dem Boden, das Schaf erscheint mit seinem alten Zustand
		server.runCommand("kill @e[type=minecraft:item]");
		throwBall(server, ball, server.computeOnServer(s -> player(s).getX() + 5), server.computeOnServer(s -> player(s).getY()), server.computeOnServer(s -> player(s).getZ()));
		server.waitFor(s -> !player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).isEmpty(), 200);
		float health = server.computeOnServer(s -> player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).getFirst().getHealth());
		check(health == 1.0F, "Das freigelassene Schaf hat noch sein altes Leben");
		server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 100);
		List<ItemEntity> back = ballItems(server);
		check(back.size() == 1 && Pokeballs.isEmpty(back.getFirst().getItem()), "Nach dem Freilassen liegt der leere Ball zum Aufheben da");

		// Ein wilder Wolf ist nach dem Fang gezähmt und gehört dem Fänger
		server.runCommand("kill @e[type=minecraft:sheep]");
		server.runCommand("kill @e[type=minecraft:item]");
		ItemStack wolfBall = null;
		for (int attempt = 1; attempt <= 12 && wolfBall == null; attempt++) {
			double[] target = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				Animal wolf = EntityTypes.WOLF.create(player.level(), EntitySpawnReason.COMMAND);
				wolf.snapTo(player.getX() + 5, player.getY(), player.getZ(), 0.0F, 0.0F);
				player.level().addFreshEntity(wolf);
				wolf.setHealth(1.0F);
				wolf.setNoAi(true);
				return new double[] {wolf.getX(), wolf.getY() + wolf.getBbHeight() / 2.0, wolf.getZ()};
			});
			context.waitTicks(2);
			throwBall(server, new ItemStack(Pokeballs.empty(BallType.POKEBALL)), target[0], target[1], target[2]);
			context.waitTicks(10);
			server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 300);
			List<ItemEntity> items = ballItems(server);
			if (items.isEmpty()) {
				server.runCommand("kill @e[type=minecraft:wolf]");
				server.waitFor(s -> player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).isEmpty(), 100);
			} else {
				wolfBall = server.computeOnServer(s -> items.getFirst().getItem().copy());
			}
		}
		check(wolfBall != null, "Ein Wolf mit 1 Leben ist nach höchstens 12 Versuchen gefangen");
		ItemStack wolfStack = wolfBall;
		server.runCommand("kill @e[type=minecraft:item]");
		throwBall(server, wolfStack, server.computeOnServer(s -> player(s).getX() + 5), server.computeOnServer(s -> player(s).getY()), server.computeOnServer(s -> player(s).getZ()));
		server.waitFor(s -> !player(s).level().getEntitiesOfClass(Animal.class, around(player(s))).isEmpty(), 200);
		check(server.computeOnServer(s -> player(s).level().getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class, around(player(s))).getFirst()
			.getOwner() == player(s)), "Der freigelassene Wolf ist gezähmt und gehört dem Fänger");
		server.runCommand("kill @e[type=minecraft:wolf]");
		server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 100);

		// Daneben geworfen: Der leere Ball geht nicht verloren
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("kill @e[type=minecraft:sheep]");
		throwBall(server, new ItemStack(Pokeballs.empty(BallType.POKEBALL)), server.computeOnServer(s -> player(s).getX() + 5), server.computeOnServer(s -> player(s).getY()), server.computeOnServer(s -> player(s).getZ()));
		server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 100);
		check(ballItems(server).size() == 1, "Ein Fehlwurf auf den Boden lässt den leeren Ball liegen");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	/** Prüft, was die Werkbank aus dem Raster macht. Leer, wenn es kein Rezept gibt. */
	private static ItemStack craft(final TestServerContext server, final List<ItemStack> grid, final int width, final int height) {
		return server.computeOnServer(s -> {
			CraftingInput input = CraftingInput.of(width, height, grid);
			return s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, s.overworld())
				.map(holder -> holder.value().assemble(input))
				.orElse(ItemStack.EMPTY);
		});
	}

	private static List<ItemStack> ring(final ItemStack outer, final ItemStack center) {
		List<ItemStack> grid = new java.util.ArrayList<>();
		for (int i = 0; i < 9; i++) {
			grid.add(i == 4 ? center : outer);
		}
		return grid;
	}

	private static ItemStack filledSheepBall(final BallType type) {
		return Pokeballs.filled(type, new CaughtAnimal(net.minecraft.resources.Identifier.withDefaultNamespace("sheep"), new net.minecraft.nbt.CompoundTag()));
	}

	private static void testBallUpgrades(final TestServerContext server) {
		ItemStack pokeball = new ItemStack(Pokeballs.empty(BallType.POKEBALL));
		ItemStack superball = new ItemStack(Pokeballs.empty(BallType.SUPERBALL));
		check(craft(server, ring(new ItemStack(Items.LAPIS_LAZULI), pokeball), 3, 3).is(Pokeballs.empty(BallType.SUPERBALL)),
			"Pokéball mit Lapislazuli ringsherum wird zum Superball");
		check(craft(server, ring(new ItemStack(Items.GOLD_INGOT), superball), 3, 3).is(Pokeballs.empty(BallType.HYPERBALL)),
			"Superball mit Gold ringsherum wird zum Hyperball");
		check(craft(server, ring(new ItemStack(Items.GOLD_INGOT), pokeball), 3, 3).isEmpty(), "Aus dem Pokéball wird mit Gold nichts");
		check(craft(server, ring(new ItemStack(Items.LAPIS_LAZULI), filledSheepBall(BallType.POKEBALL)), 3, 3).isEmpty(),
			"Ein voller Ball lässt sich nicht aufwerten, sonst ginge das Tier verloren");
		check(craft(server, List.of(new ItemStack(Items.LEATHER), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.LEATHER)), 3, 1).is(Belts.BELT),
			"Leder, Eisen, Leder ergibt einen Gürtel");
	}

	private static List<ItemEntity> itemsOfClass(final TestServerContext server, final java.util.function.Predicate<ItemStack> filter) {
		return server.computeOnServer(s -> player(s).level().getEntitiesOfClass(ItemEntity.class, around(player(s)), item -> filter.test(item.getItem())));
	}

	/** Ein gefangenes Tier lässt sich per Rechtsklick ohne Risiko wieder einfangen, ein Fehlschlag verscheucht es. */
	private static void testRecaptureAndFlee(final ClientGameTestContext context, final TestServerContext server) {
		flattenArea(server);
		server.runCommand("kill @e[type=minecraft:sheep]");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
		server.runCommand("item replace entity @a weapon.mainhand with qualityofmine:pokeball 3");
		context.waitTicks(2);

		for (int round = 1; round <= 3; round++) {
			Animal sheep = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				Animal created = EntityTypes.SHEEP.create(player.level(), EntitySpawnReason.COMMAND);
				created.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0.0F, 0.0F);
				created.addTag(ThrownPokeball.CAUGHT_TAG);
				player.level().addFreshEntity(created);
				return created;
			});
			InteractionResult result = server.computeOnServer(s -> UseEntityCallback.EVENT.invoker()
				.interact(player(s), player(s).level(), InteractionHand.MAIN_HAND, sheep, null));
			check(result == InteractionResult.SUCCESS, "Rechtsklick mit dem Ball auf ein bekanntes Tier wird vom Ball übernommen (Runde " + round + ")");
			context.waitTicks(5);
			check(server.computeOnServer(s -> !sheep.isAlive()), "Das Tier verschwindet im Ball (Runde " + round + ")");
			server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 200);
			check(itemsOfClass(server, Pokeballs::isFilled).size() == round, "Der volle Ball liegt da (Runde " + round + ")");
		}
		check(server.computeOnServer(s -> player(s).getMainHandItem().getCount()) == 0, "Jeder Fang hat einen Ball verbraucht");

		// Ein unbekanntes Tier lässt der Rechtsklick in Ruhe, dafür muss man werfen
		Animal wild = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			Animal created = EntityTypes.SHEEP.create(player.level(), EntitySpawnReason.COMMAND);
			created.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0.0F, 0.0F);
			player.level().addFreshEntity(created);
			return created;
		});
		server.runCommand("item replace entity @a weapon.mainhand with qualityofmine:pokeball 1");
		check(server.computeOnServer(s -> UseEntityCallback.EVENT.invoker()
			.interact(player(s), player(s).level(), InteractionHand.MAIN_HAND, wild, null)) == InteractionResult.PASS,
			"Ein Tier, das noch nie gefangen war, lässt sich nicht per Rechtsklick einfangen");

		// Ausbruch: Das Tier gilt als getroffen und flüchtet
		float health = server.computeOnServer(s -> wild.getHealth());
		server.runOnServer(s -> ThrownPokeball.provoke(wild, player(s), player(s)));
		check(server.computeOnServer(s -> wild.getLastHurtByMob() == player(s)), "Das ausgebrochene Tier weiß, wer es getroffen hat");
		float after = server.computeOnServer(s -> wild.getHealth());
		check(after > 0.0F && after >= health - 1.0F, "Der Treffer verletzt das Tier höchstens leicht und tötet es nicht");
		// Ob es flieht, zeigt die KI: Beim Ausbruch geht das Tier in den Panik-Modus. Der Endabstand wäre vom Zufall abhängig.
		boolean panicked = false;
		for (int tick = 0; tick < 40 && !panicked; tick++) {
			context.waitTick();
			panicked = server.computeOnServer(s -> isPanicking(wild));
		}
		check(panicked, "Das Tier ist in Panik geraten und flieht");
		server.runCommand("kill @e[type=minecraft:sheep]");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	private static List<ItemEntity> filledBallItems(final TestServerContext server) {
		return itemsOfClass(server, Pokeballs::isFilled);
	}

	/** Welche Arten fangbar sind, und ein echter Fang samt Freilassen eines Fischs. */
	private static void testWaterCreatures(final ClientGameTestContext context, final TestServerContext server) {
		flattenArea(server);
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:item]");
		Map<EntityType<?>, Boolean> expected = new java.util.LinkedHashMap<>();
		expected.put(EntityTypes.COD, true);
		expected.put(EntityTypes.SALMON, true);
		expected.put(EntityTypes.PUFFERFISH, true);
		expected.put(EntityTypes.TROPICAL_FISH, true);
		expected.put(EntityTypes.TADPOLE, true);
		expected.put(EntityTypes.SQUID, true);
		expected.put(EntityTypes.GLOW_SQUID, true);
		expected.put(EntityTypes.DOLPHIN, true);
		expected.put(EntityTypes.AXOLOTL, true);
		expected.put(EntityTypes.NAUTILUS, true);
		expected.put(EntityTypes.BAT, true);
		expected.put(EntityTypes.ALLAY, true);
		expected.put(EntityTypes.IRON_GOLEM, true);
		expected.put(EntityTypes.SNOW_GOLEM, true);
		expected.put(EntityTypes.COPPER_GOLEM, true);
		expected.put(EntityTypes.ZOMBIE_NAUTILUS, false);
		expected.put(EntityTypes.HOGLIN, false);
		expected.put(EntityTypes.SHULKER, false);
		expected.put(EntityTypes.ZOMBIE, false);
		expected.put(EntityTypes.VILLAGER, false);
		expected.put(EntityTypes.WANDERING_TRADER, false);
		for (Map.Entry<EntityType<?>, Boolean> entry : expected.entrySet()) {
			boolean catchable = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				net.minecraft.world.entity.Entity entity = entry.getKey().create(player.level(), EntitySpawnReason.COMMAND);
				return ThrownPokeball.canCapture(entity, player);
			});
			check(catchable == entry.getValue(), EntityType.getKey(entry.getKey()).getPath() + (entry.getValue() ? " ist fangbar" : " ist nicht fangbar"));
		}

		// Ein Kabeljau mit 1 Leben wird gefangen und kommt beim Freilassen wieder heraus
		ItemStack filled = null;
		for (int attempt = 1; attempt <= 12 && filled == null; attempt++) {
			double[] target = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				Mob cod = EntityTypes.COD.create(player.level(), EntitySpawnReason.COMMAND);
				cod.snapTo(player.getX() + 4, player.getY(), player.getZ(), 0.0F, 0.0F);
				cod.setNoAi(true);
				player.level().addFreshEntity(cod);
				cod.setHealth(1.0F);
				return new double[] {cod.getX(), cod.getY() + cod.getBbHeight() / 2.0, cod.getZ()};
			});
			context.waitTicks(2);
			throwBall(server, new ItemStack(Pokeballs.empty(BallType.HYPERBALL)), target[0], target[1], target[2]);
			server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 300);
			List<ItemEntity> items = filledBallItems(server);
			if (items.isEmpty()) {
				server.runCommand("kill @e[type=minecraft:cod]");
			} else {
				filled = server.computeOnServer(s -> items.getFirst().getItem().copy());
			}
		}
		check(filled != null, "Ein Kabeljau mit 1 Leben ist nach höchstens 12 Versuchen gefangen");
		ItemStack codBall = filled;
		check(codBall.get(Pokeballs.CAUGHT_ANIMAL).type().getPath().equals("cod"), "Der Ball enthält den Kabeljau");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("kill @e[type=minecraft:cod]");
		server.waitFor(s -> player(s).level().getEntitiesOfClass(net.minecraft.world.entity.animal.fish.Cod.class, around(player(s))).isEmpty(), 100);
		throwBall(server, codBall, server.computeOnServer(s -> player(s).getX() + 4), server.computeOnServer(s -> player(s).getY()), server.computeOnServer(s -> player(s).getZ()));
		server.waitFor(s -> !player(s).level().getEntitiesOfClass(net.minecraft.world.entity.animal.fish.Cod.class, around(player(s))).isEmpty(), 200);
		check(server.computeOnServer(s -> player(s).level().getEntitiesOfClass(net.minecraft.world.entity.animal.fish.Cod.class, around(player(s))).size()) == 1,
			"Der freigelassene Kabeljau ist wieder in der Welt");
		server.runCommand("kill @e[type=minecraft:cod]");
		server.waitFor(s -> player(s).level().getEntitiesOfClass(ThrownPokeball.class, around(player(s))).isEmpty(), 100);
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	private static Slot beltSlot(final MinecraftServer server, final int beltIndex) {
		return player(server).inventoryMenu.slots.stream().filter(BeltFeature::isBeltSlot).toList().get(beltIndex);
	}

	/** Der Gürtel hat einen eigenen Platz und 6 Plätze, in die nur volle Bälle passen. */
	private static void testBelt(final TestServerContext server) {
		server.runCommand("clear @a");
		ItemStack filled = filledSheepBall(BallType.SUPERBALL);
		ItemStack empty = new ItemStack(Pokeballs.empty(BallType.POKEBALL));
		ItemStack belt = new ItemStack(Belts.BELT);

		check(server.computeOnServer(s -> player(s).inventoryMenu.slots.stream().filter(BeltFeature::isBeltSlot).count()) == 7,
			"Das Inventar hat einen Gürtelplatz und 6 Plätze im Gürtel");
		check(server.computeOnServer(s -> !beltSlot(s, 1).isActive() && !beltSlot(s, 1).mayPlace(filled)), "Ohne Gürtel gibt es keine Plätze");
		check(server.computeOnServer(s -> !beltSlot(s, 0).mayPlace(filled) && !beltSlot(s, 0).mayPlace(new ItemStack(Items.LEATHER)) && beltSlot(s, 0).mayPlace(belt)),
			"Auf den Gürtelplatz passt nur der Gürtel");

		server.runOnServer(s -> beltSlot(s, 0).set(belt.copy()));
		check(server.computeOnServer(s -> beltSlot(s, 1).isActive() && beltSlot(s, 6).isActive()), "Mit Gürtel sind alle 6 Plätze da");
		check(server.computeOnServer(s -> beltSlot(s, 1).mayPlace(filled)), "Ein voller Ball passt in den Gürtel");
		check(server.computeOnServer(s -> !beltSlot(s, 1).mayPlace(empty) && !beltSlot(s, 1).mayPlace(new ItemStack(Items.DIRT))),
			"Ein leerer Ball und andere Gegenstände passen nicht hinein");

		server.runOnServer(s -> {
			beltSlot(s, 1).set(filled.copy());
			beltSlot(s, 6).set(filled.copy());
		});
		check(server.computeOnServer(s -> Pokeballs.isFilled(beltSlot(s, 1).getItem()) && Pokeballs.isFilled(beltSlot(s, 6).getItem()) && beltSlot(s, 3).getItem().isEmpty()),
			"Der Inhalt steht in den gewählten Plätzen");
		// Der Inhalt steckt im Gürtel, also bleibt er beim Ablegen mit ihm zusammen
		ItemStack taken = server.computeOnServer(s -> beltSlot(s, 0).remove(1));
		check(taken.is(Belts.BELT) && taken.get(Belts.CONTENTS).entries().size() == 2,
			"Der Gürtel nimmt seinen Inhalt mit, wenn man ihn ablegt");
		check(server.computeOnServer(s -> beltSlot(s, 1).getItem().isEmpty() && !beltSlot(s, 1).isActive()), "Ohne Gürtel sind die Plätze wieder leer");
		server.runOnServer(s -> beltSlot(s, 0).set(taken));
		check(server.computeOnServer(s -> Pokeballs.isFilled(beltSlot(s, 1).getItem())), "Wieder angelegt ist der Inhalt zurück");

		// Wurf direkt aus dem Gürtel, ohne das Inventar zu öffnen
		server.runOnServer(s -> BeltFeature.useSlot(player(s), 2));
		check(server.computeOnServer(s -> Pokeballs.isFilled(beltSlot(s, 1).getItem())), "Ein leerer Gürtelplatz wirft nichts");
		check(!ballInFlight(server), "Aus einem leeren Platz fliegt kein Ball");
		server.runOnServer(s -> BeltFeature.useSlot(player(s), 0));
		check(server.computeOnServer(s -> beltSlot(s, 1).getItem().isEmpty() && Pokeballs.isFilled(beltSlot(s, 6).getItem())) && ballInFlight(server),
			"Der Wurf aus dem Gürtel leert nur diesen Platz und lässt den Ball fliegen");

		// Aufgehobene volle Bälle füllen zuerst den Gürtel, erst danach das Inventar
		server.runCommand("clear @a minecraft:superball_filled");
		server.runOnServer(s -> player(s).getInventory().add(filled.copy()));
		check(server.computeOnServer(s -> Pokeballs.isFilled(beltSlot(s, 1).getItem()) && player(s).getInventory().countItem(filled.getItem()) == 0),
			"Ein aufgehobener voller Ball landet im Gürtel");
		server.runOnServer(s -> {
			// Zwei Plätze sind schon belegt, vier Bälle füllen den Rest, der fünfte hat keinen Platz mehr
			for (int i = 0; i < BeltFeature.CONTENT_SIZE - 1; i++) {
				player(s).getInventory().add(filled.copy());
			}
		});
		check(server.computeOnServer(s -> player(s).getInventory().countItem(filled.getItem()) == 1
			&& IntStream.rangeClosed(1, BeltFeature.CONTENT_SIZE).allMatch(i -> Pokeballs.isFilled(beltSlot(s, i).getItem()))),
			"Bei vollem Gürtel kommt der Ball ins normale Inventar");
		server.runOnServer(s -> player(s).getInventory().add(new ItemStack(Pokeballs.empty(BallType.POKEBALL))));
		check(server.computeOnServer(s -> player(s).getInventory().countItem(Pokeballs.empty(BallType.POKEBALL)) == 1),
			"Ein leerer Ball bleibt im Inventar");
		server.runOnServer(s -> beltSlot(s, 0).set(ItemStack.EMPTY));
	}

	/** Klick im geöffneten Inventar auf einen Ball im Gürtel: Er wandert an den Mauszeiger und wird nicht weggeworfen. */
	private static void testBeltClick(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("clear @a");
		ItemStack filled = filledSheepBall(BallType.POKEBALL);
		server.runOnServer(s -> {
			beltSlot(s, 0).set(new ItemStack(Belts.BELT));
			beltSlot(s, 1).set(filled.copy());
		});
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
		context.waitTicks(5);
		double[] position = context.computeOnClient(minecraft -> {
			Slot slot = minecraft.player.inventoryMenu.slots.stream().filter(BeltFeature::isBeltSlot).skip(1).findFirst().orElseThrow();
			int scale = minecraft.getWindow().getGuiScale();
			int left = (minecraft.getWindow().getGuiScaledWidth() - IMAGE_WIDTH) / 2;
			int top = (minecraft.getWindow().getGuiScaledHeight() - IMAGE_HEIGHT) / 2;
			return new double[]{(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
		});
		context.getInput().setCursorPos(position[0], position[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(5);
		check(context.computeOnClient(minecraft -> Pokeballs.isFilled(minecraft.player.inventoryMenu.getCarried())),
			"Ein Klick auf den Ball im Gürtel nimmt ihn an den Mauszeiger");
		check(itemsOfClass(server, stack -> Pokeballs.isFilled(stack)).isEmpty(), "Der Ball im Gürtel wird nicht weggeworfen");
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(5);
		check(server.computeOnServer(s -> Pokeballs.isFilled(beltSlot(s, 1).getItem())) && itemsOfClass(server, stack -> Pokeballs.isFilled(stack)).isEmpty(),
			"Ein zweiter Klick legt den Ball zurück in den Gürtel");
		context.setScreen(() -> null);
		server.runOnServer(s -> beltSlot(s, 0).set(ItemStack.EMPTY));
	}

	private static ItemStack quiverItem(final TestServerContext server, final int index) {
		return server.computeOnServer(s -> beltSlot(s, index + 1).getItem().copy());
	}

	/** Schießt mit einem voll gespannten Bogen, wie beim Loslassen der rechten Maustaste. */
	private static void shootBow(final TestServerContext server, final boolean infinity) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ItemStack bow = new ItemStack(Items.BOW);
			if (infinity) {
				bow.enchant(s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY), 1);
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, bow);
			bow.getItem().releaseUsing(bow, player.level(), player, bow.getUseDuration(player) - 20);
		});
	}

	private static int arrowsInFlight(final TestServerContext server, final Class<? extends AbstractArrow> type) {
		return server.computeOnServer(s -> player(s).level().getEntitiesOfClass(type, around(player(s))).size());
	}

	/** Der Köcher: Rezept, nur Pfeile, zuerst beim Aufheben, Auswahl per Alt und Mausrad, Verschießen und Unendlichkeit. */
	private static void testQuiver(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:item]");
		check(craft(server, List.of(new ItemStack(Items.STRING), new ItemStack(Items.RABBIT_HIDE), new ItemStack(Items.LEATHER)), 1, 3).is(Belts.QUIVER),
			"Faden, Kaninchenfell, Leder untereinander ergibt einen Köcher");
		check(server.computeOnServer(s -> beltSlot(s, 0).mayPlace(new ItemStack(Belts.QUIVER))), "Der Köcher passt auf den Platz des Gürtels");
		server.runOnServer(s -> beltSlot(s, 0).set(new ItemStack(Belts.QUIVER)));
		check(server.computeOnServer(s -> beltSlot(s, 1).mayPlace(new ItemStack(Items.ARROW)) && beltSlot(s, 1).mayPlace(new ItemStack(Items.SPECTRAL_ARROW))
			&& !beltSlot(s, 1).mayPlace(filledSheepBall(BallType.POKEBALL)) && !beltSlot(s, 1).mayPlace(new ItemStack(Items.DIRT))),
			"In den Köcher passen nur Pfeile");
		check(server.computeOnServer(s -> beltSlot(s, 1).getMaxStackSize(new ItemStack(Items.ARROW))) == 64, "Ein Köcherplatz fasst einen vollen Stapel");

		// Aufheben: Pfeile landen zuerst im Köcher
		ItemStack healing = PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.HEALING).copyWithCount(3);
		server.runOnServer(s -> {
			player(s).getInventory().add(new ItemStack(Items.ARROW, 20));
			player(s).getInventory().add(new ItemStack(Items.SPECTRAL_ARROW, 5));
			player(s).getInventory().add(healing.copy());
		});
		check(quiverItem(server, 0).is(Items.ARROW) && quiverItem(server, 0).getCount() == 20 && quiverItem(server, 1).is(Items.SPECTRAL_ARROW)
			&& quiverItem(server, 2).is(Items.TIPPED_ARROW) && server.computeOnServer(s -> player(s).getInventory().countItem(Items.ARROW)) == 0,
			"Aufgehobene Pfeile landen im Köcher und nicht im Inventar");
		server.runOnServer(s -> {
			ItemEntity arrows = new ItemEntity(player(s).level(), player(s).getX(), player(s).getY(), player(s).getZ(), new ItemStack(Items.ARROW, 10));
			player(s).level().addFreshEntity(arrows);
		});
		server.waitFor(s -> beltSlot(s, 1).getItem().getCount() == 30, 100);
		check(quiverItem(server, 0).getCount() == 30, "Pfeile vom Boden kommen auf den gleichen Stapel im Köcher");
		context.waitTicks(5);
		context.takeScreenshot("qom_quiver_hud");
		context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
		context.waitTicks(5);
		context.takeScreenshot("qom_quiver_inventory");
		context.setScreen(() -> null);

		// Umschalt-Klick aus einer Truhe
		SimpleContainer chest = new SimpleContainer(27);
		chest.setItem(0, new ItemStack(Items.ARROW, 16));
		server.runOnServer(s -> player(s).openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest), Component.literal("Test"))));
		context.waitTicks(5);
		server.runOnServer(s -> player(s).containerMenu.quickMoveStack(player(s), 0));
		check(quiverItem(server, 0).getCount() == 46 && chest.getItem(0).isEmpty(), "Umschalt-Klick aus der Truhe legt Pfeile in den Köcher");
		server.runOnServer(s -> player(s).closeContainer());
		context.waitTicks(5);

		// Auswahl über die echte Eingabe: Alt gedrückt halten und scrollen
		context.getInput().holdKey(InputConstants.KEY_LALT);
		context.getInput().scroll(-1);
		context.waitTicks(5);
		context.getInput().releaseKey(InputConstants.KEY_LALT);
		check(server.computeOnServer(s -> BeltFeature.holderOf(player(s)).selected()) == 1, "Alt und Mausrad wählen den nächsten Platz, auch auf dem Server");
		check(server.computeOnServer(s -> player(s).getInventory().getSelectedSlot()) == 0, "Die Item-Leiste bleibt dabei stehen");

		// Der Bogen verschießt den gewählten Spektralpfeil aus dem Köcher
		// Ein Pfeil direkt auf einem Inventarplatz, für die Prüfung ohne Köcher weiter unten
		server.runOnServer(s -> player(s).getInventory().setItem(9, new ItemStack(Items.ARROW, 1)));
		check(server.computeOnServer(s -> player(s).getProjectile(new ItemStack(Items.BOW)).is(Items.SPECTRAL_ARROW)), "Der Bogen nimmt den gewählten Pfeil");
		shootBow(server, false);
		check(quiverItem(server, 1).getCount() == 4 && arrowsInFlight(server, SpectralArrow.class) == 1, "Der Schuss verbraucht den Spektralpfeil aus dem Köcher");
		check(quiverItem(server, 0).getCount() == 46, "Die normalen Pfeile im Köcher bleiben unberührt");

		// Unendlichkeit verbraucht auch Trankpfeile nicht
		server.runOnServer(s -> BeltFeature.holderOf(player(s)).select(2));
		shootBow(server, true);
		check(quiverItem(server, 2).getCount() == 3, "Mit Unendlichkeit bleibt auch ein Trankpfeil erhalten");

		// Ist der gewählte Platz leer, kommt der erste passende Pfeil aus dem Köcher
		server.runOnServer(s -> BeltFeature.holderOf(player(s)).select(5));
		check(server.computeOnServer(s -> player(s).getProjectile(new ItemStack(Items.BOW)).is(Items.ARROW)), "Ein leerer Platz nimmt den ersten Pfeil im Köcher");

		// Abgelegt nimmt der Köcher seinen Inhalt mit, der Gürtel nimmt keine Pfeile
		ItemStack taken = server.computeOnServer(s -> beltSlot(s, 0).remove(1));
		check(taken.is(Belts.QUIVER) && taken.get(Belts.CONTENTS).entries().size() == 3,
			"Der Köcher nimmt seine Pfeile mit, wenn man ihn ablegt");
		check(server.computeOnServer(s -> player(s).getProjectile(new ItemStack(Items.BOW)).is(Items.ARROW)) && server.computeOnServer(s -> player(s).getInventory().countItem(Items.ARROW)) == 1,
			"Ohne Köcher nimmt der Bogen wieder Pfeile aus dem Inventar");
		server.runOnServer(s -> beltSlot(s, 0).set(new ItemStack(Belts.BELT)));
		check(server.computeOnServer(s -> !beltSlot(s, 1).mayPlace(new ItemStack(Items.ARROW))), "In den Gürtel passen keine Pfeile");
		server.runOnServer(s -> beltSlot(s, 0).set(ItemStack.EMPTY));
		server.runCommand("kill @e[type=#minecraft:arrows]");
		server.runCommand("clear @a");
	}

	private static void selectOnBothSides(final ClientGameTestContext context, final TestServerContext server, final int slot) {
		server.runOnServer(s -> BeltFeature.holderOf(player(s)).select(slot));
		context.runOnClient(minecraft -> BeltFeature.holderOf(minecraft.player).select(slot));
	}

	private static void click(final TestServerContext server, final int slot, final int button, final ContainerInput input) {
		server.runOnServer(s -> player(s).inventoryMenu.clicked(slot, button, input, player(s)));
	}

	/**
	 * Der Trankgurt: Rezept, nur Flaschen, Stapel zu 8, Speichern, Aufheben, Trinken mit gehaltener Taste, Werfen und das
	 * Herausnehmen von Stapeln, die außerhalb nicht stapeln.
	 */
	private static void testBandolier(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("effect clear @a");
		ItemStack leather = new ItemStack(Items.LEATHER);
		ItemStack rod = new ItemStack(Items.BLAZE_ROD);
		check(craft(server, List.of(leather, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, rod, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, leather), 3, 3)
			.is(Belts.BANDOLIER), "Leder, Lohenrute, Leder diagonal ergibt einen Trankgurt");

		ItemStack swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
		ItemStack healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
		ItemStack splash = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
		ItemStack lingering = PotionContents.createItemStack(Items.LINGERING_POTION, Potions.HEALING);
		ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
		server.runOnServer(s -> beltSlot(s, 0).set(new ItemStack(Belts.BANDOLIER)));
		check(server.computeOnServer(s -> Stream.of(swiftness, splash, lingering, water, new ItemStack(Items.HONEY_BOTTLE), new ItemStack(Items.OMINOUS_BOTTLE))
			.allMatch(beltSlot(s, 1)::mayPlace)), "In den Trankgurt passen Tränke, Wurf- und Verweiltränke, Wasser-, Honig- und unheilvolle Flaschen");
		check(server.computeOnServer(s -> Stream.of(new ItemStack(Items.ARROW), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.DIRT), filledSheepBall(BallType.POKEBALL))
			.noneMatch(beltSlot(s, 1)::mayPlace)), "Pfeile, leere Flaschen und anderes passen nicht hinein");
		check(server.computeOnServer(s -> beltSlot(s, 1).getMaxStackSize(swiftness)) == 8, "Ein Platz im Trankgurt fasst 8 Tränke, obwohl sie sonst nicht stapeln");

		// Aufheben: gleiche Tränke stapeln sich zu 8, andere bekommen einen eigenen Platz
		server.runOnServer(s -> {
			for (int i = 0; i < 10; i++) {
				player(s).getInventory().add(swiftness.copy());
			}
			player(s).getInventory().add(healing.copy());
			player(s).getInventory().add(splash.copy());
		});
		check(quiverItem(server, 0).getCount() == 8 && quiverItem(server, 1).getCount() == 2 && ItemStack.isSameItemSameComponents(quiverItem(server, 1), swiftness)
			&& ItemStack.isSameItemSameComponents(quiverItem(server, 2), healing) && quiverItem(server, 3).is(Items.SPLASH_POTION)
			&& server.computeOnServer(s -> player(s).getInventory().countItem(Items.POTION)) == 0,
			"Aufgehobene Tränke landen im Trankgurt, gleiche stapeln sich zu 8");

		// Speichern: Der Stapel von 8 übersteht Ablegen und das Schreiben ins Format des Spielstands
		ItemStack taken = server.computeOnServer(s -> beltSlot(s, 0).remove(1));
		check(taken.is(Belts.BANDOLIER) && taken.get(Belts.CONTENTS).entries().getFirst().item().count() == 8,
			"Der abgelegte Trankgurt nimmt den Stapel von 8 Tränken mit");
		ItemStack reloaded = server.computeOnServer(s -> {
			RegistryOps<Tag> ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
			Tag saved = ItemStack.CODEC.encodeStart(ops, taken).getOrThrow();
			return ItemStack.CODEC.parse(ops, saved).getOrThrow();
		});
		check(ItemStack.validateStrict(reloaded).isSuccess(), "Das Spiel hält den gespeicherten Trankgurt für gültig");
		server.runOnServer(s -> beltSlot(s, 0).set(reloaded));
		check(quiverItem(server, 0).getCount() == 8 && quiverItem(server, 3).is(Items.SPLASH_POTION),
			"Nach Speichern und Laden sind alle Tränke wieder da, auch der Stapel von 8");

		// Ein Köcher aus Version 2.0.0 speicherte im Behälter des Spiels, sein Inhalt bleibt beim Anlegen erhalten
		ItemStack oldQuiver = new ItemStack(Belts.QUIVER);
		oldQuiver.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.ARROW, 12))));
		ItemStack bandolier = server.computeOnServer(s -> beltSlot(s, 0).remove(1));
		ItemStack migrated = server.computeOnServer(s -> {
			beltSlot(s, 0).set(oldQuiver);
			return beltSlot(s, 1).getItem().copy();
		});
		check(migrated.is(Items.ARROW) && migrated.getCount() == 12, "Ein Köcher aus Version 2.0.0 behält seine Pfeile");
		ItemStack newQuiver = server.computeOnServer(s -> beltSlot(s, 0).remove(1));
		check(!newQuiver.has(DataComponents.CONTAINER) && newQuiver.get(Belts.CONTENTS).entries().size() == 1,
			"Abgelegt steckt sein Inhalt in der neuen Komponente");
		server.runOnServer(s -> beltSlot(s, 0).set(bandolier));

		// Trinken mit gehaltener Taste dauert so lange wie aus der Hand
		context.waitTicks(5);
		selectOnBothSides(context, server, 0);
		context.getInput().holdKey(InputConstants.KEY_G);
		context.waitTicks(10);
		check(context.computeOnClient(minecraft -> BeltClientFeature.isDrinking(minecraft.player)), "Mit gehaltener Taste trinkt der Spieler auf dem Client");
		check(server.computeOnServer(s -> BeltFeature.holderOf(player(s)).isDrinking()), "Auch der Server weiß, dass getrunken wird");
		check(quiverItem(server, 0).getCount() == 8 && server.computeOnServer(s -> !player(s).hasEffect(MobEffects.SPEED)),
			"Nach einer halben Sekunde ist der Trank noch nicht getrunken");
		context.takeScreenshot("qom_bandolier_drinking");
		server.waitFor(s -> beltSlot(s, 1).getItem().getCount() == 7, 60);
		context.getInput().releaseKey(InputConstants.KEY_G);
		context.waitTicks(5);
		check(server.computeOnServer(s -> player(s).hasEffect(MobEffects.SPEED)), "Der getrunkene Trank wirkt");
		check(server.computeOnServer(s -> player(s).getInventory().countItem(Items.GLASS_BOTTLE)) == 1, "Die leere Flasche kommt ins Inventar");
		check(server.computeOnServer(s -> !BeltFeature.holderOf(player(s)).isDrinking())
			&& context.computeOnClient(minecraft -> !BeltClientFeature.isDrinking(minecraft.player)), "Loslassen beendet das Trinken");

		// Zu früh losgelassen: kein Trank verbraucht
		context.getInput().holdKey(InputConstants.KEY_G);
		context.waitTicks(15);
		context.getInput().releaseKey(InputConstants.KEY_G);
		context.waitTicks(30);
		check(quiverItem(server, 0).getCount() == 7, "Wer zu früh loslässt, trinkt nichts");

		// Ein Wurftrank fliegt sofort
		selectOnBothSides(context, server, 3);
		context.getInput().pressKey(InputConstants.KEY_G);
		context.waitTicks(5);
		check(quiverItem(server, 3).isEmpty(), "Der Wurftrank ist aus dem Trankgurt geworfen");
		check(server.computeOnServer(s -> player(s).getInventory().countItem(Items.SPLASH_POTION)) == 0, "Er ist nicht im Inventar gelandet");
		context.takeScreenshot("qom_bandolier_hud");

		// Herausnehmen: Der ganze Stapel kommt an die Maus, auf einen normalen Platz passt aber nur einer
		int stackSlot = server.computeOnServer(s -> beltSlot(s, 1).index);
		click(server, stackSlot, 0, ContainerInput.PICKUP);
		check(server.computeOnServer(s -> player(s).inventoryMenu.getCarried().getCount()) == 7, "Ein Klick nimmt den ganzen Stapel an die Maus");
		click(server, 9, 0, ContainerInput.PICKUP);
		check(server.computeOnServer(s -> player(s).getInventory().getItem(9).getCount() == 1 && player(s).inventoryMenu.getCarried().getCount() == 6),
			"Auf einen normalen Platz legt der Klick nur einen Trank");
		click(server, stackSlot, 0, ContainerInput.PICKUP);
		check(quiverItem(server, 0).getCount() == 6 && server.computeOnServer(s -> player(s).inventoryMenu.getCarried().isEmpty()),
			"Zurück in den Trankgurt geht der ganze Rest");

		// Zahlentaste: Nur ein Trank wandert auf den freien Platz der Item-Leiste, der Rest bleibt im Trankgurt
		server.runOnServer(s -> player(s).getInventory().setItem(0, ItemStack.EMPTY));
		click(server, stackSlot, 0, ContainerInput.SWAP);
		check(server.computeOnServer(s -> player(s).getInventory().getItem(0).getCount() == 1) && quiverItem(server, 0).getCount() == 5,
			"Die Zahlentaste nimmt nur einen Trank aus dem Stapel");
		server.runOnServer(s -> player(s).getInventory().setItem(1, new ItemStack(Items.DIRT)));
		click(server, stackSlot, 1, ContainerInput.SWAP);
		check(server.computeOnServer(s -> player(s).getInventory().getItem(1).is(Items.DIRT)) && quiverItem(server, 0).getCount() == 5,
			"Auf einen belegten Platz tauscht die Zahlentaste keinen Stapel");

		// Umschalt-Klick verteilt den Stapel einzeln aufs Inventar
		server.runCommand("clear @a minecraft:potion");
		click(server, stackSlot, 0, ContainerInput.QUICK_MOVE);
		check(server.computeOnServer(s -> player(s).getInventory().countItem(Items.POTION)) == 5 && quiverItem(server, 0).isEmpty(),
			"Umschalt-Klick holt alle Tränke aus dem Stapel ins Inventar");
		check(server.computeOnServer(s -> IntStream.range(0, 36).allMatch(i -> player(s).getInventory().getItem(i).getCount() <= player(s).getInventory().getItem(i).getMaxStackSize())),
			"Im normalen Inventar liegt nie mehr als ein Trank auf einem Platz");
		context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
		context.waitTicks(5);
		context.takeScreenshot("qom_bandolier_inventory");
		context.setScreen(() -> null);
		server.runOnServer(s -> beltSlot(s, 0).set(ItemStack.EMPTY));
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
		server.runCommand("effect clear @a");
	}

	/** Die 6 Plätze des Trankgurts erscheinen auch in Truhe und Braustand, auf Server und Client gleich, und lassen sich dort benutzen. */
	private static void testBeltInMenus(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("clear @a");
		ItemStack swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
		server.runOnServer(s -> {
			beltSlot(s, 0).set(new ItemStack(Belts.BANDOLIER));
			beltSlot(s, 1).set(swiftness.copyWithCount(8));
		});
		// Erst muss das Inventar den Trankgurt zum Client schicken, das passiert nur, solange kein anderes Menü offen ist
		context.waitTicks(5);

		// Truhe: 27 Plätze, 36 im Inventar, danach die 6 des Trankgurts
		SimpleContainer chest = new SimpleContainer(27);
		server.runOnServer(s -> player(s).openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest), Component.literal("Test"))));
		context.waitTicks(5);
		check(server.computeOnServer(s -> player(s).containerMenu.slots.size()) == 27 + 36 + 6
			&& server.computeOnServer(s -> BeltFeature.isBeltSlot(player(s).containerMenu.slots.get(63))),
			"Die Truhe hat auf dem Server die 6 Plätze des Trankgurts am Ende");
		check(context.computeOnClient(minecraft -> minecraft.player.containerMenu instanceof ChestMenu && minecraft.player.containerMenu.slots.size() == 27 + 36 + 6),
			"Der Client hat dieselben Plätze in der Truhe");
		check(context.computeOnClient(minecraft -> {
			Slot hotbar = minecraft.player.containerMenu.slots.get(54);
			Slot belt = minecraft.player.containerMenu.slots.get(63);
			return belt.x == hotbar.x && belt.y == hotbar.y + 25 && belt.getItem().getCount() == 8;
		}), "Die Plätze liegen unter der Item-Leiste und zeigen die 8 Tränke");
		context.takeScreenshot("qom_belt_chest");

		server.runOnServer(s -> player(s).containerMenu.clicked(63, 0, ContainerInput.QUICK_MOVE, player(s)));
		check(quiverItem(server, 0).isEmpty() && chest.countItem(Items.POTION) == 8
			&& IntStream.range(0, 27).allMatch(i -> chest.getItem(i).getCount() <= 1),
			"Umschalt-Klick legt die 8 Tränke einzeln in die Truhe");
		server.runOnServer(s -> player(s).containerMenu.clicked(0, 0, ContainerInput.QUICK_MOVE, player(s)));
		check(quiverItem(server, 0).getCount() == 1 && chest.countItem(Items.POTION) == 7,
			"Umschalt-Klick aus der Truhe legt den Trank zurück in den Trankgurt");
		server.runOnServer(s -> player(s).closeContainer());
		context.waitTicks(5);

		// Braustand: 5 eigene Plätze, nur die 3 Flaschenplätze nehmen Tränke
		server.runOnServer(s -> beltSlot(s, 1).set(swiftness.copyWithCount(8)));
		context.waitTicks(5);
		server.runOnServer(s -> player(s).openMenu(new SimpleMenuProvider((id, inventory, p) -> new BrewingStandMenu(id, inventory), Component.literal("Test"))));
		context.waitTicks(5);
		check(context.computeOnClient(minecraft -> minecraft.player.containerMenu instanceof BrewingStandMenu && minecraft.player.containerMenu.slots.size() == 5 + 36 + 6),
			"Der Braustand hat die 6 Plätze des Trankgurts");
		context.takeScreenshot("qom_belt_brewing");
		server.runOnServer(s -> player(s).containerMenu.clicked(5 + 36, 0, ContainerInput.QUICK_MOVE, player(s)));
		check(server.computeOnServer(s -> IntStream.range(0, 3).allMatch(i -> player(s).containerMenu.slots.get(i).getItem().is(Items.POTION)))
			&& quiverItem(server, 0).getCount() == 5, "Umschalt-Klick füllt die 3 Flaschenplätze des Braustands aus dem Trankgurt");
		server.runOnServer(s -> player(s).closeContainer());
		context.waitTicks(5);
		server.runOnServer(s -> beltSlot(s, 0).set(ItemStack.EMPTY));
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	/** Ebnet und räumt eine Fläche vor dem Spieler frei, damit kein Hügel oder Baum die Wurfbahn versperrt. */
	private static void flattenArea(final TestServerContext server) {
		BlockPos base = server.computeOnServer(s -> player(s).blockPosition());
		String x1 = Integer.toString(base.getX() - 2);
		String x2 = Integer.toString(base.getX() + 12);
		String z1 = Integer.toString(base.getZ() - 3);
		String z2 = Integer.toString(base.getZ() + 3);
		server.runCommand("fill " + x1 + " " + (base.getY() - 1) + " " + z1 + " " + x2 + " " + (base.getY() - 1) + " " + z2 + " minecraft:stone");
		server.runCommand("fill " + x1 + " " + base.getY() + " " + z1 + " " + x2 + " " + (base.getY() + 6) + " " + z2 + " minecraft:air");
	}

	private static boolean isPanicking(final Animal animal) {
		try {
			java.lang.reflect.Field field = net.minecraft.world.entity.Mob.class.getDeclaredField("goalSelector");
			field.setAccessible(true);
			net.minecraft.world.entity.ai.goal.GoalSelector goals = (net.minecraft.world.entity.ai.goal.GoalSelector) field.get(animal);
			return goals.getAvailableGoals().stream().anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.PanicGoal);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** Der Gürtel fällt beim Tod mit Inhalt zu Boden. */
	private static void testBeltDeath(final ClientGameTestContext context, final TestServerContext server) {
		server.runOnServer(s -> player(s).inventoryMenu.slots.stream().filter(BeltFeature::isBeltSlot).findFirst().orElseThrow().set(new ItemStack(Belts.BELT)));
		server.runCommand("kill @a");
		context.waitForScreen(DeathScreen.class);
		check(!itemsOfClass(server, stack -> stack.is(Belts.BELT)).isEmpty(), "Der Gürtel liegt nach dem Tod zum Aufheben da");
		check(server.computeOnServer(s -> BeltFeature.beltOf(player(s)).isEmpty()), "Der Spieler trägt den Gürtel nicht mehr");
		context.runOnClient(minecraft -> minecraft.player.respawn());
		context.waitFor(minecraft -> minecraft.gui.screen() == null);
		context.waitTicks(10);
	}

	static ServerPlayer player(final MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
		LOGGER.info("[QoM-Test] OK: {}", message);
	}
}
