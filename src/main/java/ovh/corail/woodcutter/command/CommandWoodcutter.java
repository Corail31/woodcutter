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
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jline.utils.Levenshtein;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionItem;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionMod;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.helper.LangKey;

import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Files.asByteSource;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_NAME;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandWoodcutter {
    private final Map<Item, WoodCompo> plankToLog = new HashMap<>();

    private CommandWoodcutter() {
    }

    private int showUsage(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(LangKey.COMMAND_USAGE.getText(), false);
        return 1;
    }

    private int applyDataPack(CommandContext<CommandSource> context) {
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
            context.getSource().sendFeedback(LangKey.DATAPACK_APPLY_SUCCESS.getText(), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw LangKey.DATAPACK_APPLY_FAIL.asCommandException(LangKey.FILE_COPY_FAIL.getText(destination.getAbsolutePath()));
    }

    private int removeDataPack(CommandContext<CommandSource> context) {
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
        context.getSource().sendFeedback(LangKey.DATAPACK_REMOVE_SUCCESS.getText(), false);
        return 1;
    }

    private int generateDataPack(CommandContext<CommandSource> context) {
        // try to determine the existing wooden recipes, then generate theses recipes in the woodcutter format in the config folder and finalize that folder in a zip
        String modid = StringArgumentType.getString(context, MODID_PARAM);
        if (INVALID_MODID.test(modid)) {
            throw LangKey.INVALID_MODID.asCommandException();
        }
        final Map<String, WoodcuttingJsonRecipe> recipes = new HashMap<>();
        initPlanksToLogs(context.getSource().getServer());
        final Map<IRecipe<CraftingInventory>, Double> entries = context.getSource().getServer().getRecipeManager().getRecipes(IRecipeType.CRAFTING).entrySet().stream().filter(entry -> modid.equals(entry.getKey().getNamespace())).map(Map.Entry::getValue).filter(r -> VALID_RESULT.test(r.getRecipeOutput())).collect(Collectors.toMap(Function.identity(), this::getWeight));
        for (Map.Entry<IRecipe<CraftingInventory>, Double> entry : entries.entrySet()) {
            double weight = entry.getValue();
            if (weight == 0d) {
                continue;
            }
            ItemStack result = entry.getKey().getRecipeOutput();
            NonNullList<Ingredient> ingredients = entry.getKey().getIngredients();
            ResourceLocation outputName = result.getItem().getRegistryName();
            assert outputName != null;
            if (ingredients.size() == 1) {
                WoodCompo compo = this.plankToLog.get(result.getItem());
                if (compo != null) {
                    // logs to planks recipes
                    addRecipe(recipes, compo.logName, outputName, MathHelper.floor(result.getCount() / (double) ingredients.get(0).getMatchingStacks()[0].getCount()), compo.isLogTag);
                    continue;
                }
            }
            WoodCompo compo = getWoodCompo(ingredients);
            if (compo == null) {
                continue;
            }
            if (weight < 1d) { // slab
                int count = MathHelper.floor(1 / weight);
                addRecipe(recipes, compo.logName, outputName, count * 4, compo.isLogTag);
                addRecipe(recipes, compo.plankName, outputName, count, compo.isPlankTag);
            } else if (weight >= 3.1d) { // boat / fence_gate
                addRecipe(recipes, compo.logName, outputName, 1, compo.isLogTag);
            } else { // others
                addRecipe(recipes, compo.logName, outputName, 4, compo.isLogTag);
                addRecipe(recipes, compo.plankName, outputName, 1, compo.isPlankTag);
            }
        }
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
            context.getSource().sendFeedback(LangKey.DATAPACK_GENERATE_SUCCESS.getText(recipes.size()), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw LangKey.ZIP_CREATE_FAIL.asCommandException();
    }

    @Nullable
    private WoodCompo getWoodCompo(NonNullList<Ingredient> ingredients) {
        Set<Item> planks = new HashSet<>();
        int vanillaPlanks = 0;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.hasNoMatchingItems()) {
                for (ItemStack stack : ingredient.getMatchingStacks()) {
                    if (this.plankToLog.containsKey(stack.getItem()) && planks.add(stack.getItem()) && VANILLA_ITEM.test(stack) && ++vanillaPlanks > 1) {
                        return WoodCompo.ANY_WOOD;
                    }
                }
            }
        }
        return planks.size() == 0 ? WoodCompo.ANY_WOOD : planks.size() == 1 ? this.plankToLog.get(planks.iterator().next()) : planks.stream().map(this.plankToLog::get).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static class WoodCompo {
        private static final WoodCompo ANY_WOOD = new WoodCompo(ItemTags.PLANKS.getName(), true, ItemTags.LOGS.getName(), true);
        private final ResourceLocation plankName, logName;
        private final boolean isPlankTag, isLogTag;
        private WoodCompo(ResourceLocation plankName, boolean isPlankTag, ResourceLocation logName, boolean isLogTag) {
            this.plankName = plankName;
            this.isPlankTag = isPlankTag;
            this.logName = logName;
            this.isLogTag = isLogTag;
        }
    }

    private void initPlanksToLogs(MinecraftServer server) {
        // locate the logs to planks recipes to determine the pairs planks/logs and if a tag can be used
        if (!this.plankToLog.isEmpty()) {
            return;
        }
        server.getRecipeManager().getRecipes(IRecipeType.CRAFTING).entrySet().stream().filter(entry -> !"minecraft".equals(entry.getKey().getNamespace())).map(Map.Entry::getValue).filter(r -> r.getIngredients().size() == 1).filter(ShapelessRecipe.class::isInstance).filter(r -> NON_VANILLA_PLANKS.test(r.getRecipeOutput())).filter(r -> NON_VANILLA_LOG.test(r.getIngredients().get(0)))
            .forEach(logRecipe -> this.plankToLog.computeIfAbsent(logRecipe.getRecipeOutput().getItem(), plank -> {
                ResourceLocation plankName = plank.getRegistryName();
                assert plankName != null;
                Ingredient ingredient = logRecipe.getIngredients().get(0);
                ResourceLocation tagName = Arrays.stream(ingredient.acceptedItems).filter(v -> v instanceof Ingredient.TagList).findFirst().map(tagValue -> ((Ingredient.TagList) tagValue).tag).map(tag -> TagCollectionManager.getManager().getItemTags().getDirectIdFromTag(tag)).orElse(null);
                if (tagName != null) {
                    return new WoodCompo(plankName, false, tagName, true);
                }
                ItemStack[] stacks = ingredient.getMatchingStacks();
                final ResourceLocation logName = stacks[0].getItem().getRegistryName();
                assert logName != null;
                tagName = Arrays.stream(stacks).map(ItemStack::getItem).distinct().flatMap(item -> item.getTags().stream()).distinct().filter(rl -> plankName.getNamespace().equals(rl.getNamespace())).filter(rl -> Levenshtein.distance(logName.getPath(), rl.getPath(), 1, 0, 1, 10) <= 2).findFirst().orElse(null);
                return new WoodCompo(plankName, false, Optional.ofNullable(tagName).orElse(logName), tagName != null);
            }))
        ;
        this.plankToLog.put(Items.ACACIA_PLANKS, new WoodCompo(Objects.requireNonNull(Items.ACACIA_PLANKS.getRegistryName()), false, ItemTags.ACACIA_LOGS.getName(), true));
        this.plankToLog.put(Items.BIRCH_PLANKS, new WoodCompo(Objects.requireNonNull(Items.BIRCH_PLANKS.getRegistryName()), false, ItemTags.BIRCH_LOGS.getName(), true));
        this.plankToLog.put(Items.DARK_OAK_PLANKS, new WoodCompo(Objects.requireNonNull(Items.DARK_OAK_PLANKS.getRegistryName()), false, ItemTags.DARK_OAK_LOGS.getName(), true));
        this.plankToLog.put(Items.JUNGLE_PLANKS, new WoodCompo(Objects.requireNonNull(Items.JUNGLE_PLANKS.getRegistryName()), false, ItemTags.JUNGLE_LOGS.getName(), true));
        this.plankToLog.put(Items.OAK_PLANKS, new WoodCompo(Objects.requireNonNull(Items.OAK_PLANKS.getRegistryName()), false, ItemTags.OAK_LOGS.getName(), true));
        this.plankToLog.put(Items.SPRUCE_PLANKS, new WoodCompo(Objects.requireNonNull(Items.SPRUCE_PLANKS.getRegistryName()), false, ItemTags.SPRUCE_LOGS.getName(), true));
        this.plankToLog.put(Items.CRIMSON_PLANKS, new WoodCompo(Objects.requireNonNull(Items.CRIMSON_PLANKS.getRegistryName()), false, ItemTags.CRIMSON_STEMS.getName(), true));
        this.plankToLog.put(Items.WARPED_PLANKS, new WoodCompo(Objects.requireNonNull(Items.WARPED_PLANKS.getRegistryName()), false, ItemTags.WARPED_STEMS.getName(), true));
    }

    private String getZipName(String modid) {
        return "corail_woodcutter_" + modid + ".zip";
    }

    private void toZip(Path source, String modid) throws IOException {
        try (final ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(CONFIG_FOLDER.apply(getZipName(modid)).toPath())))) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @SuppressWarnings("UnstableApiUsage")
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

    private void disableDataPack(CommandSource source, String modid) {
        // disable datapack if enabled (selected)
        ResourcePackList packs = source.getServer().getResourcePacks();
        ResourcePackInfo pack = packs.getPackInfo("file/" + getZipName(modid));
        if (pack != null && packs.getEnabledPacks().contains(pack)) {
            List<ResourcePackInfo> selectedPacks = Lists.newArrayList(packs.getEnabledPacks());
            selectedPacks.remove(pack);
            source.getServer().func_240780_a_(selectedPacks.stream().map(ResourcePackInfo::getName).collect(Collectors.toList()));
        }
    }

    private void discoverNewDataPack(MinecraftServer server) {
        ResourcePackList packs = server.getResourcePacks();
        Collection<String> selectedPackIds = Lists.newArrayList(packs.func_232621_d_());
        packs.reloadPacksFromFinders();
        Collection<String> disabledPackIds = server.getServerConfiguration().getDatapackCodec().getDisabled();
        for (String packId : packs.func_232616_b_()) {
            if (!disabledPackIds.contains(packId) && !selectedPackIds.contains(packId)) {
                selectedPackIds.add(packId);
            }
        }
        server.func_240780_a_(selectedPackIds);
    }

    private boolean toFile(File file, WoodcuttingJsonRecipe recipe) {
        try {
            if (file.createNewFile()) {
                FileWriter fw = new FileWriter(file);
                fw.write(GSON.toJson(recipe));
                fw.close();
                return true;
            }
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
        try {
            if (file.createNewFile()) {
                FileWriter fw = new FileWriter(file);
                JsonObject json = new JsonObject();
                JsonObject pack = new JsonObject();
                json.add("pack", pack);
                pack.addProperty("description", MOD_NAME + " " + StringUtils.capitalize(modid) + " Resources");
                pack.addProperty("pack_format", PACK_FORMAT);
                fw.write(GSON.toJson(json));
                fw.close();
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private void addRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, ResourceLocation input, ResourceLocation output, int count, boolean isTag) {
        recipes.put(output.getPath() + "_from_" + input.getPath(), new WoodcuttingJsonRecipe(input.toString(), output.toString(), count, isTag));
    }

    private double getWeight(IRecipe<CraftingInventory> recipe) {
        final NonNullList<Ingredient> ingredients = recipe.getIngredients();
        double weight = 0d;
        double maxWeight = 5d * recipe.getRecipeOutput().getCount();
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.hasNoMatchingItems()) {
                ItemStack[] stacks = ingredient.getMatchingStacks();
                ItemStack stack = stacks[0];
                final Predicate<Item> predicate;
                if (ItemTags.LOGS.contains(stack.getItem())) {
                    predicate = ItemTags.LOGS::contains;
                    weight += 4d * stack.getCount();
                } else if (this.plankToLog.containsKey(stack.getItem())) {
                    predicate = this.plankToLog::containsKey;
                    weight += 1d * stack.getCount();
                } else if (Tags.Items.RODS_WOODEN.contains(stack.getItem())) {
                    predicate = Tags.Items.RODS_WOODEN::contains;
                    weight += 0.5d * stack.getCount();
                } else if (ItemTags.WOODEN_SLABS.contains(stack.getItem())) {
                    predicate = ItemTags.WOODEN_SLABS::contains;
                    weight += 0.5d * stack.getCount();
                } else {
                    return 0d;
                }
                if (weight > maxWeight || Arrays.stream(stacks).anyMatch(s -> !predicate.test(s.getItem()))) {
                    return 0d;
                }
            }
        }
        return weight / (double) recipe.getRecipeOutput().getCount();
    }

    private void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralCommandNode<CommandSource> command = dispatcher.register(Commands.literal("cwc").requires(p -> p.hasPermissionLevel(2))
                .executes(this::showUsage)
                .then(BaseAction.INFO.literal().executes(this::showUsage)
                ).then(BaseAction.DATAPACK.literal().executes(this::showUsage)
                        .then(DataPackAction.GENERATE.literal().executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::generateDataPack))
                        ).then(DataPackAction.APPLY.literal().requires(p -> p.getServer().isSinglePlayer()).executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::applyDataPack))
                        ).then(DataPackAction.REMOVE.literal().requires(p -> p.getServer().isSinglePlayer()).executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::removeDataPack))
                        )
                )
        );
        // alphabetic order of literal is relevant for redirect (fixed in 1.17.1)
        dispatcher.register(Commands.literal("woodcutter").requires(p -> p.hasPermissionLevel(2)).redirect(command));
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new CommandWoodcutter().register(event.getDispatcher());
    }

    private enum BaseAction implements IAction {
        INFO, DATAPACK;
        private final String name;

        BaseAction() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getString() {
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
        public String getString() {
            return this.name;
        }
    }

    private interface IAction extends IStringSerializable {
        default LiteralArgumentBuilder<CommandSource> literal() {
            return Commands.literal(getString());
        }
    }

    private static final String MODID_PARAM = "modid";
    private static final int PACK_FORMAT = 5;
    private static final Predicate<ItemStack> VANILLA_ITEM = stack -> Optional.ofNullable(stack.getItem().getRegistryName()).map(ResourceLocation::getNamespace).map("minecraft"::equals).orElse(false);
    private static final Predicate<ItemStack> VALID_RESULT = result -> !result.isEmpty() && !VANILLA_ITEM.test(result);
    private static final Predicate<ItemStack> NON_VANILLA_PLANKS = stack -> VALID_RESULT.test(stack) && (ItemTags.PLANKS.contains(stack.getItem()) || Optional.ofNullable(stack.getItem().getRegistryName()).map(ResourceLocation::getPath).map(e -> e.endsWith("_planks")).orElse(false));
    private static final Predicate<Ingredient> NON_VANILLA_LOG = ing -> !ing.hasNoMatchingItems() && (ItemTags.LOGS.contains(ing.getMatchingStacks()[0].getItem()) || Optional.ofNullable(ing.getMatchingStacks()[0].getItem().getRegistryName()).map(ResourceLocation::getPath).map(e -> e.endsWith("_log") || e.endsWith("_stem")).orElse(false)) && Arrays.stream(ing.getMatchingStacks()).noneMatch(VANILLA_ITEM);
    private static final Predicate<String> INVALID_MODID = modid -> modid == null || "minecraft".equals(modid) || !ModList.get().isLoaded(modid) || SupportMods.hasSupport(modid);
    private static final BiFunction<MinecraftServer, String, File> DATAPACK_FOLDER = (server, folder) -> new File(server.func_240776_a_(FolderName.DATAPACKS).toFile(), folder);
    private static final Function<String, File> CONFIG_FOLDER = folder -> new File(FMLPaths.CONFIGDIR.get().toFile(), "corail_woodcutter" + File.separatorChar + folder);
    private static final SuggestionProvider<CommandSource> SUGGESTION_MODID = (ctx, build) -> ISuggestionProvider.suggest(ModList.get().applyForEachModContainer(ModContainer::getModId).filter(SupportMods::noSupport).filter(modid -> !"minecraft".equals(modid)), build);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
}
