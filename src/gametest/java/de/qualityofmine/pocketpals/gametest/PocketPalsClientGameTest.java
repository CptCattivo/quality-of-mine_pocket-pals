package de.qualityofmine.pocketpals.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
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
import net.minecraft.world.inventory.Slot;
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
			testBelt(server);
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
		check(taken.is(Belts.BELT) && taken.get(net.minecraft.core.component.DataComponents.CONTAINER).nonEmptyItemCopyStream().count() == 2,
			"Der Gürtel nimmt seinen Inhalt mit, wenn man ihn ablegt");
		check(server.computeOnServer(s -> beltSlot(s, 1).getItem().isEmpty() && !beltSlot(s, 1).isActive()), "Ohne Gürtel sind die Plätze wieder leer");
		server.runOnServer(s -> beltSlot(s, 0).set(taken));
		check(server.computeOnServer(s -> Pokeballs.isFilled(beltSlot(s, 1).getItem())), "Wieder angelegt ist der Inhalt zurück");
		server.runOnServer(s -> beltSlot(s, 0).set(ItemStack.EMPTY));
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
