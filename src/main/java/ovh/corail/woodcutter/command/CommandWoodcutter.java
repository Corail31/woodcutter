package ovh.corail.woodcutter.command;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jline.utils.Levenshtein;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionItem;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionMod;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.helper.LangKey;

import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Files.asByteSource;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_NAME;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandWoodcutter {
    private final Set<Item> logs = new HashSet<>();
    private final Map<Item, WoodCompo> plankToLog = new HashMap<>();

    private CommandWoodcutter() {
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(LangKey.COMMAND_USAGE.getText(), false);
        return 1;
    }

    private int applyDataPack(CommandContext<CommandSourceStack> context) {
        // copy the generated datapack in the datapack folder
        String modid = StringArgumentType.getString(context, MODID_PARAM);
        if (INVALID_MODID.test(modid)) {
            throw LangKey.INVALID_MODID.asCommandException();
        }
        String zipName = getZipName(modid);
        File configDatapackZip = CONFIG_FOLDER.apply(zipName);
        if (!configDatapackZip.exists()) {
            throw LangKey.DATAPACK_NOT_GENERATED.asCommandException(modid);
        }
        File destination = DATAPACK_FOLDER.apply(context.getSource().getServer(), zipName);
        if (destination.exists()) {
            disableDataPack(context.getSource(), modid);
            if (!destination.delete()) {
                throw LangKey.DATAPACK_APPLY_FAIL.asCommandException(LangKey.FILE_DELETE_FAIL.getText(destination.getAbsolutePath()));
            }
        }
        try {
            FileUtils.copyFile(configDatapackZip, destination);
            discoverNewDataPack(context.getSource().getServer());
            context.getSource().sendSuccess(LangKey.DATAPACK_APPLY_SUCCESS.getText(), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw LangKey.DATAPACK_APPLY_FAIL.asCommandException(LangKey.FILE_COPY_FAIL.getText(destination.getAbsolutePath()));
    }

    private int removeDataPack(CommandContext<CommandSourceStack> context) {
        // delete the datapack from the datapack folder
        String modid = StringArgumentType.getString(context, MODID_PARAM);
        if (INVALID_MODID.test(modid)) {
            throw LangKey.INVALID_MODID.asCommandException();
        }
        File destination = DATAPACK_FOLDER.apply(context.getSource().getServer(), getZipName(modid));
        if (!destination.exists()) {
            throw LangKey.DATAPACK_REMOVE_ABSENT.asCommandException();
        }
        disableDataPack(context.getSource(), modid);
        if (!destination.delete()) {
            throw LangKey.DATAPACK_REMOVE_FAIL.asCommandException(LangKey.FILE_DELETE_FAIL.getText(destination.getAbsolutePath()));
        }
        context.getSource().sendSuccess(LangKey.DATAPACK_REMOVE_SUCCESS.getText(), false);
        return 1;
    }

    private int generateDataPack(CommandContext<CommandSourceStack> context) {
        // try to determine the existing wooden recipes, then generate these recipes in the woodcutter format in the config folder and finalize that folder in a zip
        String modid = StringArgumentType.getString(context, MODID_PARAM);
        if (INVALID_MODID.test(modid)) {
            throw LangKey.INVALID_MODID.asCommandException();
        }
        MinecraftServer server = context.getSource().getServer();
        initPlanksToLogs(server);
        final Map<String, WoodcuttingJsonRecipe> recipes = getJsonRecipes(getCraftingRecipes(server, recipe -> modid.equals(recipe.getId().getNamespace()) && NOT_VANILLA_ITEM.test(recipe.getResultItem())));
        if (recipes.isEmpty()) {
            throw LangKey.NO_VALID_RECIPE_FOR_MODID.asCommandException(modid);
        }
        File datapackFolder = CONFIG_FOLDER.apply(MOD_ID + "_" + modid);
        File dataFolder = new File(datapackFolder, "data");
        File recipeFolder = new File(dataFolder, MOD_ID + "_" + modid + File.separatorChar + "recipes");
        if (recipeFolder.exists()) {
            try {
                FileUtils.cleanDirectory(recipeFolder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!recipeFolder.exists() && !recipeFolder.mkdirs()) {
            throw LangKey.DATAPACK_GENERATE_FAIL.asCommandException(LangKey.FOLDER_CREATE_FAIL.getText(recipeFolder.getAbsolutePath()));
        }
        for (Map.Entry<String, WoodcuttingJsonRecipe> entry : recipes.entrySet()) {
            File file = new File(recipeFolder, entry.getKey() + ".json");
            if (file.exists() && !file.delete()) {
                throw LangKey.DATAPACK_GENERATE_FAIL.asCommandException(LangKey.FILE_DELETE_FAIL.getText(file.getAbsolutePath()));
            }
            if (!toFile(file, entry.getValue().withConditions(new ConditionMod(modid), new ConditionMod(MOD_ID), new ConditionItem(entry.getValue().result)))) {
                throw LangKey.DATAPACK_GENERATE_FAIL.asCommandException(LangKey.FILE_WRITE_FAIL.getText(file.getAbsolutePath()));
            }
        }
        if (!addMcMeta(datapackFolder, modid)) {
            throw LangKey.MCMETA_CREATE_FAIL.asCommandException();
        }
        try {
            toZip(datapackFolder.toPath(), modid);
            context.getSource().sendSuccess(LangKey.DATAPACK_GENERATE_SUCCESS.getText(recipes.size()), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            throw LangKey.ZIP_CREATE_FAIL.asCommandException();
        }
    }

    private WoodCompo getWoodCompo(NonNullList<Ingredient> ingredients) {
        final Function<Predicate<ItemStack>, Ingredient> findIngredient = predic -> ingredients.stream().filter(ing -> !ing.isEmpty() && Arrays.stream(ing.getItems()).allMatch(predic)).findFirst().orElse(null);
        // from planks
        Ingredient plankIngredient = findIngredient.apply(stack -> this.plankToLog.containsKey(stack.getItem()));
        if (plankIngredient != null) {
            Set<Item> planks = new HashSet<>();
            int vanillaPlanks = 0;
            for (ItemStack stack : plankIngredient.getItems()) {
                if (planks.add(stack.getItem()) && VANILLA_ITEM.test(stack) && ++vanillaPlanks > 1) {
                    return WoodCompo.ANY_WOOD;
                }
            }
            if (planks.size() == 1) {
                return this.plankToLog.get(planks.iterator().next());
            }
            WoodCompo compo = this.plankToLog.get(planks.iterator().next());
            return getTagForIngredient(plankIngredient).map(tag -> new WoodCompo(tag, true, compo.logName(), compo.isLogTag())).orElse(compo);
        }
        // from logs & slabs
        Ingredient ingredient = Optional.ofNullable(findIngredient.apply(stack -> this.logs.contains(stack.getItem()))).orElse(findIngredient.apply(stack -> Helper.isInTag(stack.getItem(), ItemTags.WOODEN_SLABS)));
        if (ingredient != null) {
            String itemName = Helper.getRegistryName(ingredient.getItems()[0].getItem()).replaceAll("_log|_stem|_slab", "_plank");
            WoodCompo compo = this.plankToLog.entrySet().stream().filter(entry -> ALMOSTLY_SIMILAR_PATH.test(Helper.getRegistryName(entry.getKey()), itemName)).findFirst().map(Map.Entry::getValue).orElse(null);
            if (compo != null) {
                return compo;
            }
        }
        // from sticks
        return WoodCompo.ANY_WOOD;
    }

    private record WoodCompo(ResourceLocation plankName, boolean isPlankTag, @Nullable ResourceLocation logName, boolean isLogTag) {
        private static final WoodCompo ANY_WOOD = new WoodCompo(ItemTags.PLANKS.location(), true, ItemTags.LOGS.location(), true);
    }

    private void initPlanksToLogs(MinecraftServer server) {
        // locate the logs to planks recipes to determine the pairs planks/logs and if a tag can be used
        if (!this.plankToLog.isEmpty()) {
            return;
        }
        // init log
        Helper.fillItemSet(this.logs, ItemTags.LOGS);
        ForgeRegistries.ITEMS.getEntries().stream().filter(entry -> entry.getKey().getRegistryName().getPath().endsWith("_log") || entry.getKey().getRegistryName().getPath().endsWith("_stem")).map(Map.Entry::getValue).forEach(this.logs::add);
        getCraftingRecipes(server, this::isLogToPlankRecipe)
            .forEach(logRecipe -> this.plankToLog.computeIfAbsent(logRecipe.getResultItem().getItem(), plank -> {
                ResourceLocation plankName = Helper.getRegistryRL(plank);
                // if a tag is provided in the recipe
                Ingredient ingredient = logRecipe.getIngredients().get(0);
                Optional<ResourceLocation> tagName = getTagForIngredient(ingredient);
                if (tagName.isPresent()) {
                    return new WoodCompo(plankName, false, tagName.get(), true);
                }
                ItemStack[] stacks = ingredient.getItems();
                // fallback from the common tags of that namespace
                final Set<ResourceLocation> commonTags = getCommonTags(stacks, plankName.getNamespace());
                if (stacks.length == 1) {
                    ResourceLocation logName = Helper.getRegistryRL(stacks[0]);
                    String logPath = logName.getPath().replace("stripped_", "");
                    return commonTags.stream().filter(rl -> ALMOSTLY_SIMILAR_PATH.test(logPath, rl.getPath())).findFirst().map(rl -> new WoodCompo(plankName, false, rl, true)).orElse(new WoodCompo(plankName, false, logName, false));
                }
                final Set<ResourceLocation> logNames = Arrays.stream(stacks).map(ItemStack::getItem).map(ForgeRegistryEntry::getRegistryName).collect(Collectors.toSet());
                for (ResourceLocation tagRL : commonTags) {
                    // having a name similar to one of the logs
                    if (logNames.stream().anyMatch(logName -> ALMOSTLY_SIMILAR_PATH.test(logName.getPath().replace("stripped_", ""), tagRL.getPath()))) {
                        return new WoodCompo(plankName, false, tagRL, true);
                    }
                }
                return new WoodCompo(plankName, false, logNames.stream().min(Comparator.comparingInt(rl -> Levenshtein.distance(rl.getPath(), plankName.getPath()))).orElse(null), false);
            }))
        ;
        this.plankToLog.put(Items.ACACIA_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/acacia"), true, ItemTags.ACACIA_LOGS.location(), true));
        this.plankToLog.put(Items.BIRCH_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/birch"), true, ItemTags.BIRCH_LOGS.location(), true));
        this.plankToLog.put(Items.DARK_OAK_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/dark_oak"), true, ItemTags.DARK_OAK_LOGS.location(), true));
        this.plankToLog.put(Items.JUNGLE_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/jungle"), true, ItemTags.JUNGLE_LOGS.location(), true));
        this.plankToLog.put(Items.OAK_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/oak"), true, ItemTags.OAK_LOGS.location(), true));
        this.plankToLog.put(Items.SPRUCE_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/spruce"), true, ItemTags.SPRUCE_LOGS.location(), true));
        this.plankToLog.put(Items.CRIMSON_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/crimson"), true, ItemTags.CRIMSON_STEMS.location(), true));
        this.plankToLog.put(Items.WARPED_PLANKS, new WoodCompo(new ResourceLocation("forge", "planks/warped"), true, ItemTags.WARPED_STEMS.location(), true));
        // planks with no log recipe
        Helper.getItems(ItemTags.PLANKS).forEach(key -> this.plankToLog.computeIfAbsent(key.value(), item -> new WoodCompo(Helper.getRegistryRL(item), false, null, false)));
    }

    private Set<ResourceLocation> getCommonTags(ItemStack[] stacks, String namespace) {
        if (stacks.length == 0) {
            return Collections.emptySet();
        }
        Set<ResourceLocation> commonTags = stacks[0].getTags().map(TagKey::location).collect(Collectors.toSet());
        commonTags.removeIf(rl -> !namespace.equals(rl.getNamespace()));
        if (stacks.length > 1) {
            IntStream.range(1, stacks.length).forEach(i -> commonTags.retainAll(stacks[i].getTags().map(TagKey::location).collect(Collectors.toSet())));
        }
        return commonTags;
    }

    private Optional<ResourceLocation> getTagForIngredient(Ingredient ingredient) {
        return Arrays.stream(ingredient.values).filter(v -> v instanceof Ingredient.TagValue).findFirst().map(tagValue -> ((Ingredient.TagValue) tagValue).tag).map(TagKey::location);
    }

    private Set<Recipe<CraftingContainer>> getCraftingRecipes(MinecraftServer server, Predicate<Recipe<CraftingContainer>> recipePredicate) {
        return server.getRecipeManager().byType(RecipeType.CRAFTING).values().stream().filter(recipePredicate).collect(Collectors.toSet());
    }

    private boolean isLogToPlankRecipe(Recipe<CraftingContainer> recipe) {
        if ("minecraft".equals(recipe.getId().getNamespace()) || recipe.getIngredients().size() != 1 || !(recipe instanceof ShapelessRecipe)) {
            return false;
        }
        ItemStack resultItem = recipe.getResultItem();
        if (NOT_VANILLA_ITEM.test(resultItem) && (resultItem.is(ItemTags.PLANKS) || Helper.getRegistryPath(resultItem.getItem()).endsWith("_planks"))) {
            final Ingredient ingredient = recipe.getIngredients().stream().filter(ing -> !ing.isEmpty()).findFirst().orElse(Ingredient.EMPTY);
            return NOT_VANILLA_ITEM.test(ingredient.getItems()[0]) && this.logs.contains(ingredient.getItems()[0].getItem()) && Arrays.stream(ingredient.getItems()).allMatch(NOT_VANILLA_ITEM);
        }
        return false;
    }

    private String getZipName(String modid) {
        return "corail_woodcutter_" + modid + ".zip";
    }

    private void toZip(Path source, String modid) throws IOException {
        try (final ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(CONFIG_FOLDER.apply(getZipName(modid)).toPath())))) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path visitedPath, BasicFileAttributes attributes) {
                    if (!visitedPath.toFile().isDirectory()) {
                        String stringPath = visitedPath.toString();
                        ZipEntry zipentry = new ZipEntry(stringPath.endsWith("pack.mcmeta") ? "pack.mcmeta" : stringPath.substring(stringPath.indexOf("data")).replace('\\', '/'));
                        try {
                            outputStream.putNextEntry(zipentry);
                            asByteSource(visitedPath.toFile()).copyTo(outputStream);
                            outputStream.closeEntry();
                        } catch (Throwable ignored) {
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void disableDataPack(CommandSourceStack source, String modid) {
        // disable datapack if enabled (selected)
        PackRepository packs = source.getServer().getPackRepository();
        Pack pack = packs.getPack("file/" + getZipName(modid));
        if (pack != null && packs.getSelectedPacks().contains(pack)) {
            List<Pack> selectedPacks = Lists.newArrayList(packs.getSelectedPacks());
            selectedPacks.remove(pack);
            source.getServer().reloadResources(selectedPacks.stream().map(Pack::getId).toList());
        }
    }

    private void discoverNewDataPack(MinecraftServer server) {
        PackRepository packs = server.getPackRepository();
        Collection<String> selectedPackIds = Lists.newArrayList(packs.getSelectedIds());
        packs.reload();
        Collection<String> disabledPackIds = server.getWorldData().getDataPackConfig().getDisabled();
        for (String packId : packs.getAvailableIds()) {
            if (!disabledPackIds.contains(packId) && !selectedPackIds.contains(packId)) {
                selectedPackIds.add(packId);
            }
        }
        server.reloadResources(selectedPackIds);
    }

    private <T> boolean toFile(File file, T object) {
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(GSON.toJson(object));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean addMcMeta(File datapackFolder, String modid) {
        File file = new File(datapackFolder, "pack.mcmeta");
        if (file.exists()) {
            return true;
        }
        JsonObject json = new JsonObject();
        JsonObject pack = new JsonObject();
        json.add("pack", pack);
        pack.addProperty("description", MOD_NAME + ": " + StringUtils.capitalize(modid) + " Resources");
        pack.addProperty("pack_format", PACK_FORMAT);
        return toFile(file, json);
    }

    private Map<String, WoodcuttingJsonRecipe> getJsonRecipes(Set<Recipe<CraftingContainer>> recipes) {
        final Map<String, WoodcuttingJsonRecipe> jsonRecipes = new HashMap<>();
        for (Recipe<CraftingContainer> recipe : recipes) {
            double weight = getWeight(recipe);
            if (weight == 0d) {
                continue;
            }
            ItemStack result = recipe.getResultItem();
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            ResourceLocation outputName = Helper.getRegistryRL(result);
            if (ingredients.size() == 1) {
                ItemStack stack = ingredients.get(0).getItems()[0];
                if (this.logs.contains(stack.getItem())) {
                    WoodCompo compo = this.plankToLog.get(result.getItem());
                    if (compo != null) {
                        // logs to planks recipes
                        addLogRecipe(jsonRecipes, compo, outputName, result.getCount() / stack.getCount());
                        continue;
                    }
                }
            }
            WoodCompo compo = getWoodCompo(ingredients);
            final int count = weight < 1d ? Mth.floor(1d / weight) : 1;
            if (weight < 3.1d) {
                addPlankRecipe(jsonRecipes, compo, outputName, count);
                addLogRecipe(jsonRecipes, compo, outputName, count * 4);
            } else {
                addLogRecipe(jsonRecipes, compo, outputName, count);
            }
        }
        return jsonRecipes;
    }

    private void addRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, ResourceLocation input, ResourceLocation output, int count, boolean isTag) {
        recipes.put(output.getPath() + "_from_" + input.getPath().replaceAll("/|\\\\", "_"), new WoodcuttingJsonRecipe(input.toString(), output.toString(), count, isTag));
    }

    private void addPlankRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, WoodCompo compo, ResourceLocation output, int count) {
        addRecipe(recipes, compo.plankName(), output, count, compo.isPlankTag());
    }

    private void addLogRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, WoodCompo compo, ResourceLocation output, int count) {
        Optional.ofNullable(compo.logName()).ifPresent(logName -> addRecipe(recipes, logName, output, count, compo.isLogTag()));
    }

    private double getWeight(Recipe<CraftingContainer> recipe) {
        final NonNullList<Ingredient> ingredients = recipe.getIngredients();
        double weight = 0d;
        double maxWeight = 5d * recipe.getResultItem().getCount();
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                ItemStack[] stacks = ingredient.getItems();
                ItemStack stack = stacks[0];
                final Predicate<Item> predicate;
                if (this.logs.contains(stack.getItem())) {
                    predicate = this.logs::contains;
                    weight += 4d * stack.getCount();
                } else if (this.plankToLog.containsKey(stack.getItem())) {
                    predicate = this.plankToLog::containsKey;
                    weight += 1d * stack.getCount();
                } else if (stack.is(Tags.Items.RODS_WOODEN)) {
                    predicate = item -> Helper.isInTag(item, Tags.Items.RODS_WOODEN);
                    weight += 0.5d * stack.getCount();
                } else if (stack.is(ItemTags.WOODEN_SLABS)) {
                    predicate = item -> Helper.isInTag(item, ItemTags.WOODEN_SLABS);
                    weight += 0.5d * stack.getCount();
                } else {
                    return 0d;
                }
                if (weight > maxWeight || Arrays.stream(stacks).anyMatch(s -> !predicate.test(s.getItem()))) {
                    return 0d;
                }
            }
        }
        return weight / (double) recipe.getResultItem().getCount();
    }

    private int testRecipe(CommandContext<CommandSourceStack> context) {
        initPlanksToLogs(context.getSource().getServer());
        ResourceLocation recipeRL = ResourceLocationArgument.getId(context, RECIPE_PARAM);
        Recipe<CraftingContainer> recipe = context.getSource().getServer().getRecipeManager().byType(RecipeType.CRAFTING).values().stream().filter(r -> r.getId().equals(recipeRL)).findFirst().orElseThrow(() -> new CommandRuntimeException(new TextComponent("[" + recipeRL + "] is not a crafting recipe")));
        final Map<String, WoodcuttingJsonRecipe> recipes = getJsonRecipes(Collections.singleton(recipe));
        if (recipes.isEmpty()) {
            throw new CommandRuntimeException(new TextComponent("[" + recipeRL + "] is not a wood recipe"));
        }
        boolean genericTag = false;
        for (Map.Entry<String, WoodcuttingJsonRecipe> entry : recipes.entrySet()) {
            String ingredient = Optional.ofNullable(entry.getValue().ingredient.tag).orElse(entry.getValue().ingredient.item);
            context.getSource().sendSuccess(new TextComponent("name=" + entry.getKey()).append("\n").append("ingredient=" + ingredient + " (" + (entry.getValue().ingredient.tag == null ? "item" : "tag") + ")").append("\n").append("result=" + entry.getValue().result + "*" + entry.getValue().count), false);
            if (entry.getValue().ingredient.tag != null && (ingredient.equals("minecraft:logs") || ingredient.equals("minecraft:planks"))) {
                genericTag = true;
            }
        }
        context.getSource().sendSuccess(new TextComponent(genericTag ? "check this recipe as it uses a generic tag that may be incorrect" : "[" + recipeRL + "] is a valid recipe"), false);
        return 1;
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> command = dispatcher.register(Commands.literal("woodcutter").requires(this::hasPermission)
                .executes(this::showUsage)
                .then(BaseAction.INFO.literal().executes(this::showUsage)
                ).then(BaseAction.DATAPACK.literal().executes(this::showUsage)
                        .then(DataPackAction.GENERATE.literal().executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::generateDataPack))
                        ).then(DataPackAction.APPLY.literal().requires(this::isSinglePlayerOwner).executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::applyDataPack))
                        ).then(DataPackAction.REMOVE.literal().requires(this::isSinglePlayerOwner).executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::removeDataPack))
                        )
                ).then(BaseAction.TEST.literal().executes(this::showUsage)
                        .then(Commands.argument(RECIPE_PARAM, ResourceLocationArgument.id()).suggests(SUGGESTION_CRAFTING_RECIPES).executes(this::testRecipe))
                )
        );
        dispatcher.register(Commands.literal("cwc").requires(this::hasPermission).redirect(command));
    }

    private boolean hasPermission(CommandSourceStack source) {
        return source.hasPermission(2) || isSinglePlayerOwner(source);
    }

    private boolean isSinglePlayerOwner(CommandSourceStack source) {
        return !source.getServer().isDedicatedServer() && source.getServer().isSingleplayer() && Optional.ofNullable(source.getEntity()).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).map(Player::getGameProfile).map(profil -> source.getServer().isSingleplayerOwner(profil)).orElse(false);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new CommandWoodcutter().register(event.getDispatcher());
    }

    private enum BaseAction implements IAction {
        INFO, DATAPACK, TEST;
        private final String name;

        BaseAction() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    private enum DataPackAction implements IAction {
        GENERATE, APPLY, REMOVE;
        private final String name;

        DataPackAction() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    private interface IAction extends StringRepresentable {
        default LiteralArgumentBuilder<CommandSourceStack> literal() {
            return Commands.literal(getSerializedName());
        }
    }

    private static final String MODID_PARAM = "modid";
    private static final String RECIPE_PARAM = "recipe";
    private static final int PACK_FORMAT = 9;
    private static final Predicate<ItemStack> VANILLA_ITEM = stack -> !stack.isEmpty() && "minecraft".equals(Helper.getRegistryNamespace(stack.getItem()));
    private static final Predicate<ItemStack> NOT_VANILLA_ITEM = stack -> !stack.isEmpty() && !"minecraft".equals(Helper.getRegistryNamespace(stack.getItem()));
    private static final Predicate<String> INVALID_MODID = modid -> modid == null || "minecraft".equals(modid) || !ModList.get().isLoaded(modid) || SupportMods.hasSupport(modid);
    private static final BiPredicate<String, String> ALMOSTLY_SIMILAR_PATH = (s1, s2) -> Levenshtein.distance(s1, s2, 1, 0, 1, 10) <= 3;
    private static final BiFunction<MinecraftServer, String, File> DATAPACK_FOLDER = (server, folder) -> new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), folder);
    private static final Function<String, File> CONFIG_FOLDER = folder -> new File(FMLPaths.CONFIGDIR.get().toFile(), "corail_woodcutter" + File.separatorChar + folder);
    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_MODID = (ctx, build) -> SharedSuggestionProvider.suggest(ModList.get().applyForEachModContainer(ModContainer::getModId).filter(SupportMods::noSupport).filter(modid -> !"minecraft".equals(modid)), build);
    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_CRAFTING_RECIPES = (ctx, build) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getRecipeManager().byType(RecipeType.CRAFTING).keySet().stream(), build);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
}
