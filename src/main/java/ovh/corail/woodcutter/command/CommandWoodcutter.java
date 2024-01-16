package ovh.corail.woodcutter.command;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jline.utils.Levenshtein;
import org.jetbrains.annotations.Nullable;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionItem;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionMod;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.helper.LangKey;

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

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandWoodcutter {
    private final Set<Item> logs = new HashSet<>();
    private final Map<Item, WoodCompo> plankToLog = new HashMap<>();

    private CommandWoodcutter() {
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(LangKey.COMMAND_USAGE::getText, false);
        return 1;
    }

    private int applyDataPack(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
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
            context.getSource().sendSuccess(LangKey.DATAPACK_APPLY_SUCCESS::getText, false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw LangKey.DATAPACK_APPLY_FAIL.asCommandException(LangKey.FILE_COPY_FAIL.getText(destination.getAbsolutePath()));
    }

    private int removeDataPack(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
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
        context.getSource().sendSuccess(LangKey.DATAPACK_REMOVE_SUCCESS::getText, false);
        return 1;
    }

    private int generateDataPack(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // try to determine the existing wooden recipes, then generate these recipes in the woodcutter format in the config folder and finalize that folder in a zip
        String modid = StringArgumentType.getString(context, MODID_PARAM);
        if (INVALID_MODID.test(modid)) {
            throw LangKey.INVALID_MODID.asCommandException();
        }
        MinecraftServer server = context.getSource().getServer();
        RegistryAccess registryAccess = context.getSource().getLevel().registryAccess();
        initPlanksToLogs(server, registryAccess);
        final Map<String, WoodcuttingJsonRecipe> recipes = getJsonRecipes(getCraftingRecipes(server, recipe -> modid.equals(recipe.id().getNamespace()) && NOT_VANILLA_ITEM.test(recipe.value().getResultItem(registryAccess))), registryAccess);
        if (recipes.isEmpty()) {
            throw LangKey.NO_VALID_RECIPE_FOR_MODID.asCommandException(modid);
        }
        File datapackFolder = CONFIG_FOLDER.apply(MOD_ID + "_" + modid);
        File dataFolder = new File(datapackFolder, "data");
        if (dataFolder.exists()) {
            try {
                FileUtils.cleanDirectory(dataFolder);
            } catch (Exception ignored) {
            }
        }
        File recipeFolder = new File(dataFolder, MOD_ID + "_" + modid + File.separatorChar + "recipes" + File.separatorChar + modid);
        if (!recipeFolder.exists() && !recipeFolder.mkdirs()) {
            throw LangKey.DATAPACK_GENERATE_FAIL.asCommandException(LangKey.FOLDER_CREATE_FAIL.getText(recipeFolder.getAbsolutePath()));
        }
        for (Map.Entry<String, WoodcuttingJsonRecipe> entry : recipes.entrySet()) {
            File file = new File(recipeFolder, entry.getKey() + ".json");
            if (file.exists() && !file.delete()) {
                throw LangKey.DATAPACK_GENERATE_FAIL.asCommandException(LangKey.FILE_DELETE_FAIL.getText(file.getAbsolutePath()));
            }
            if (!generateJson(file, entry.getValue().withConditions(new ConditionItem(entry.getValue().result), new ConditionMod(modid), new ConditionMod(MOD_ID)))) {
                throw LangKey.DATAPACK_GENERATE_FAIL.asCommandException(LangKey.FILE_WRITE_FAIL.getText(file.getAbsolutePath()));
            }
        }
        if (!addMcMeta(datapackFolder, modid)) {
            throw LangKey.MCMETA_CREATE_FAIL.asCommandException();
        }
        try {
            toZip(datapackFolder.toPath(), modid);
            context.getSource().sendSuccess(() -> LangKey.DATAPACK_GENERATE_SUCCESS.getText(recipes.size()), false);
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
        private static final WoodCompo BAMBOO = WoodCompo.of(Items.BAMBOO_PLANKS, ItemTags.BAMBOO_BLOCKS);

        public static WoodCompo of(Item item, TagKey<Item> tagKey) {
            return of(item, tagKey.location(), true);
        }

        public static WoodCompo of(Item item, @Nullable ResourceLocation logName, boolean isLogTag) {
            return new WoodCompo(Helper.getRegistryRL(item), false, logName, isLogTag);
        }
    }

    private void initPlanksToLogs(MinecraftServer server, RegistryAccess registryAccess) {
        // locate the logs to planks recipes to determine the pairs planks/logs and if a tag can be used
        if (!this.plankToLog.isEmpty()) {
            return;
        }
        // init log
        Helper.fillItemSet(this.logs, ItemTags.LOGS);
        ForgeRegistries.ITEMS.getEntries().stream().filter(entry -> entry.getKey().location().getPath().endsWith("_log") || entry.getKey().location().getPath().endsWith("_stem")).map(Map.Entry::getValue).forEach(this.logs::add);
        getCraftingRecipes(server, recipe -> isLogToPlankRecipe(recipe, registryAccess))
            .forEach(logRecipeHolder -> this.plankToLog.computeIfAbsent(logRecipeHolder.value().getResultItem(registryAccess).getItem(), plank -> {
                ResourceLocation plankName = Helper.getRegistryRL(plank);
                // if a tag is provided in the recipe
                Ingredient ingredient = logRecipeHolder.value().getIngredients().get(0);
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
                final Set<ResourceLocation> logNames = Arrays.stream(stacks).map(ItemStack::getItem).map(Helper::getRegistryRL).collect(Collectors.toSet());
                for (ResourceLocation tagRL : commonTags) {
                    // having a name similar to one of the logs
                    if (logNames.stream().anyMatch(logName -> ALMOSTLY_SIMILAR_PATH.test(logName.getPath().replace("stripped_", ""), tagRL.getPath()))) {
                        return new WoodCompo(plankName, false, tagRL, true);
                    }
                }
                return new WoodCompo(plankName, false, logNames.stream().min(Comparator.comparingInt(rl -> Levenshtein.distance(rl.getPath(), plankName.getPath()))).orElse(null), false);
            }))
        ;
        this.plankToLog.put(Items.ACACIA_PLANKS, WoodCompo.of(Items.ACACIA_PLANKS, ItemTags.ACACIA_LOGS));
        this.plankToLog.put(Items.BIRCH_PLANKS, WoodCompo.of(Items.BIRCH_PLANKS, ItemTags.BIRCH_LOGS));
        this.plankToLog.put(Items.DARK_OAK_PLANKS, WoodCompo.of(Items.DARK_OAK_PLANKS, ItemTags.DARK_OAK_LOGS));
        this.plankToLog.put(Items.JUNGLE_PLANKS, WoodCompo.of(Items.JUNGLE_PLANKS, ItemTags.JUNGLE_LOGS));
        this.plankToLog.put(Items.OAK_PLANKS, WoodCompo.of(Items.OAK_PLANKS, ItemTags.OAK_LOGS));
        this.plankToLog.put(Items.SPRUCE_PLANKS, WoodCompo.of(Items.SPRUCE_PLANKS, ItemTags.SPRUCE_LOGS));
        this.plankToLog.put(Items.CRIMSON_PLANKS, WoodCompo.of(Items.CRIMSON_PLANKS, ItemTags.CRIMSON_STEMS));
        this.plankToLog.put(Items.WARPED_PLANKS, WoodCompo.of(Items.WARPED_PLANKS, ItemTags.WARPED_STEMS));
        this.plankToLog.put(Items.MANGROVE_PLANKS, WoodCompo.of(Items.MANGROVE_PLANKS, ItemTags.MANGROVE_LOGS));
        this.plankToLog.put(Items.CHERRY_PLANKS, WoodCompo.of(Items.CHERRY_PLANKS, ItemTags.CHERRY_LOGS));
        this.plankToLog.put(Items.BAMBOO_PLANKS, WoodCompo.BAMBOO);
        this.plankToLog.put(Items.BAMBOO_MOSAIC, WoodCompo.of(Items.BAMBOO_MOSAIC, null, false));
        // planks with no log recipe
        Helper.getItems(ItemTags.PLANKS).forEach(key -> this.plankToLog.computeIfAbsent(key.value(), item -> WoodCompo.of(item, null, false)));
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

    private List<RecipeHolder<CraftingRecipe>> getCraftingRecipes(MinecraftServer server, Predicate<RecipeHolder<CraftingRecipe>> recipePredicate) {
        return server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream().filter(recipePredicate).collect(Collectors.toList());
    }

    private boolean isLogToPlankRecipe(RecipeHolder<CraftingRecipe> recipeHolder, RegistryAccess registryAccess) {
        if ("minecraft".equals(recipeHolder.id().getNamespace())) {
            return false;
        }
        CraftingRecipe recipe = recipeHolder.value();
        if (recipe.getIngredients().size() != 1 || !(recipe instanceof ShapelessRecipe)) {
            return false;
        }
        ItemStack resultItem = recipe.getResultItem(registryAccess);
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
        Collection<String> disabledPackIds = server.getWorldData().getDataConfiguration().dataPacks().getDisabled();
        for (String packId : packs.getAvailableIds()) {
            if (!disabledPackIds.contains(packId) && !selectedPackIds.contains(packId)) {
                selectedPackIds.add(packId);
            }
        }
        server.reloadResources(selectedPackIds);
    }

    private boolean generateJson(File file, WoodcuttingJsonRecipe jsonRecipe) {
        JsonObject json = new JsonObject();

        JsonObject data = new JsonObject();
        data.addProperty("type", "item_exists"); // TODO multiple conditions
        data.addProperty("item", ((ConditionItem) jsonRecipe.conditions[0]).item);
        json.add("conditions", data);

        json.addProperty("type", jsonRecipe.type);

        JsonObject ingredient = new JsonObject();
        if (jsonRecipe.ingredient.tag == null) {
            ingredient.addProperty("item", jsonRecipe.ingredient.item);
        } else {
            ingredient.addProperty("tag", jsonRecipe.ingredient.tag);
        }
        json.add("ingredient", ingredient);

        json.addProperty("result", jsonRecipe.result);
        json.addProperty("count", jsonRecipe.count);
        return toFile(file, json);
    }

    private <T> boolean toFile(File file, JsonObject jsonObject) {
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(GSON.toJson(jsonObject));
            return true;
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
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

        JsonObject text = new JsonObject();
        text.addProperty("text", MOD_NAME + ": " + StringUtils.capitalize(modid) + " Resources");
        pack.add("description", text);

        pack.addProperty("pack_format", PACK_FORMAT);
        return toFile(file, json);
    }

    private Map<String, WoodcuttingJsonRecipe> getJsonRecipes(Collection<RecipeHolder<CraftingRecipe>> recipeHolders, RegistryAccess registryAccess) {
        final Map<String, WoodcuttingJsonRecipe> jsonRecipes = new HashMap<>();
        for (RecipeHolder<CraftingRecipe> recipeHolder : recipeHolders) {
            CraftingRecipe recipe = recipeHolder.value();
            double weight = getWeight(recipe, registryAccess);
            if (weight == 0d) {
                continue;
            }
            ItemStack result = recipe.getResultItem(registryAccess);
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
            boolean isBamboo = compo.equals(WoodCompo.BAMBOO);
            if (weight < 3.1d) {
                addPlankRecipe(jsonRecipes, compo, outputName, count);
                addLogRecipe(jsonRecipes, compo, outputName, count * (isBamboo ? 2 : 4));
            } else if (!isBamboo) {
                addLogRecipe(jsonRecipes, compo, outputName, count);
            }
        }
        return jsonRecipes;
    }

    private void addRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, ResourceLocation input, ResourceLocation output, int count, boolean isTag) {
        recipes.put(RL_TO_NAME.apply(output) + "_from_" + RL_TO_NAME.apply(input), new WoodcuttingJsonRecipe(input.toString(), output.toString(), count, isTag));
    }

    private void addPlankRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, WoodCompo compo, ResourceLocation output, int count) {
        addRecipe(recipes, compo.plankName(), output, count, compo.isPlankTag());
    }

    private void addLogRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, WoodCompo compo, ResourceLocation output, int count) {
        Optional.ofNullable(compo.logName()).ifPresent(logName -> addRecipe(recipes, logName, output, count, compo.isLogTag()));
    }

    private double getWeight(Recipe<CraftingContainer> recipe, RegistryAccess registryAccess) {
        final NonNullList<Ingredient> ingredients = recipe.getIngredients();
        double weight = 0d;
        double maxWeight = 5d * recipe.getResultItem(registryAccess).getCount();
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                ItemStack[] stacks = ingredient.getItems();
                ItemStack stack = stacks[0];
                final Predicate<Item> predicate;
                if (stack.is(ItemTags.BAMBOO_BLOCKS)) {
                    predicate = item -> Helper.isInTag(item, ItemTags.BAMBOO_BLOCKS);
                    weight += 2d * stack.getCount();
                } else if (this.logs.contains(stack.getItem())) {
                    predicate = this.logs::contains;
                    weight += 4d * stack.getCount();
                } else if (this.plankToLog.containsKey(stack.getItem())) {
                    predicate = this.plankToLog::containsKey;
                    weight += 1d * stack.getCount();
                } else if (stack.is(Items.BAMBOO)) {
                    predicate = item -> item == Items.BAMBOO;
                    weight += 0.25d * stack.getCount();
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
        return weight / (double) recipe.getResultItem(registryAccess).getCount();
    }

    private int testRecipe(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        RegistryAccess registryAccess = context.getSource().registryAccess();
        initPlanksToLogs(context.getSource().getServer(), registryAccess);
        ResourceLocation recipeRL = ResourceLocationArgument.getId(context, RECIPE_PARAM);
        RecipeHolder<CraftingRecipe> recipe = context.getSource().getServer().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream().filter(r -> r.id().equals(recipeRL)).findFirst().orElseThrow(() -> LangKey.asStringCommandException(String.format("[ %s ] is not a crafting recipe", recipeRL)));
        final Map<String, WoodcuttingJsonRecipe> recipes = getJsonRecipes(Collections.singleton(recipe), registryAccess);
        if (recipes.isEmpty()) {
            throw LangKey.asStringCommandException(String.format("[ %s ] is not a wood recipe", recipeRL));
        }
        boolean genericTag = false;
        for (Map.Entry<String, WoodcuttingJsonRecipe> entry : recipes.entrySet()) {
            String ingredient = Optional.ofNullable(entry.getValue().ingredient.tag).orElse(entry.getValue().ingredient.item);
            context.getSource().sendSuccess(() -> Component.literal("name=" + entry.getKey()).append("\n").append("ingredient=" + ingredient + " (" + (entry.getValue().ingredient.tag == null ? "item" : "tag") + ")").append("\n").append("result=" + entry.getValue().result + "*" + entry.getValue().count), false);
            if (entry.getValue().ingredient.tag != null && (ingredient.equals("minecraft:logs") || ingredient.equals("minecraft:planks"))) {
                genericTag = true;
            }
        }
        Component successText = Component.literal(genericTag ? "check this recipe as it uses a generic tag that may be incorrect" : "[" + recipeRL + "] is a valid recipe");
        context.getSource().sendSuccess(() -> successText, false);
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
    private static final int PACK_FORMAT = 15;
    private static final Predicate<ItemStack> VANILLA_ITEM = stack -> !stack.isEmpty() && "minecraft".equals(Helper.getRegistryNamespace(stack.getItem()));
    private static final Predicate<ItemStack> NOT_VANILLA_ITEM = stack -> !stack.isEmpty() && !"minecraft".equals(Helper.getRegistryNamespace(stack.getItem()));
    private static final Predicate<String> INVALID_MODID = modid -> modid == null || "minecraft".equals(modid) || !ModList.get().isLoaded(modid) || SupportMods.hasSupport(modid);
    private static final BiPredicate<String, String> ALMOSTLY_SIMILAR_PATH = (s1, s2) -> Levenshtein.distance(s1, s2, 1, 0, 1, 10) <= 3;
    private static final BiFunction<MinecraftServer, String, File> DATAPACK_FOLDER = (server, folder) -> new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), folder);
    private static final Function<String, File> CONFIG_FOLDER = folder -> new File(FMLPaths.CONFIGDIR.get().toFile(), "corail_woodcutter" + File.separatorChar + folder);
    private static final Function<ResourceLocation, String> RL_TO_NAME = rl -> rl.getNamespace().equals("forge") && rl.getPath().startsWith("planks/") ? rl.getPath().replace("planks/", "") + "_planks" : rl.getPath().replaceAll("/|\\\\", "_");
    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_MODID = (ctx, build) -> SharedSuggestionProvider.suggest(ModList.get().applyForEachModContainer(ModContainer::getModId).filter(SupportMods::noSupport).filter(modid -> !"minecraft".equals(modid)), build);
    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_CRAFTING_RECIPES = (ctx, build) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream().map(RecipeHolder::id), build);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
}
