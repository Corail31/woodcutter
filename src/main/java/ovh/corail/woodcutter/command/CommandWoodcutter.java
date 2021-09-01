package ovh.corail.woodcutter.command;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandWoodcutter {
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
        // try to determine the existing wooden recipes, then generate theses recipes in the woodcutter format in the config folder and finalize that folder in a zip
        String modid = StringArgumentType.getString(context, MODID_PARAM);
        if (INVALID_MODID.test(modid)) {
            throw LangKey.INVALID_MODID.asCommandException();
        }
        final Map<String, WoodcuttingJsonRecipe> recipes = new HashMap<>();
        initPlanksToLogs(context.getSource().getServer());
        final Map<Recipe<CraftingContainer>, Double> entries = context.getSource().getServer().getRecipeManager().byType(RecipeType.CRAFTING).entrySet().stream().filter(entry -> modid.equals(entry.getKey().getNamespace())).map(Map.Entry::getValue).filter(r -> VALID_RESULT.test(r.getResultItem())).collect(Collectors.toMap(Function.identity(), this::getWeight));
        for (Map.Entry<Recipe<CraftingContainer>, Double> entry : entries.entrySet()) {
            Recipe<CraftingContainer> recipe = entry.getKey();
            double weight = entry.getValue();
            if (weight == 0d) {
                continue;
            }
            ItemStack result = recipe.getResultItem();
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            ResourceLocation outputName = result.getItem().getRegistryName();
            assert outputName != null;
            // logs to planks recipes
            if (ingredients.size() == 1 && result.is(ItemTags.PLANKS)) {
                ItemStack stack = ingredients.get(0).getItems()[0];
                if (stack.is(ItemTags.LOGS)) {
                    WoodCompo compo = this.plankToLog.get(result.getItem());
                    ResourceLocation logName = Optional.ofNullable(compo).map(WoodCompo::logName).orElse(stack.getItem().getRegistryName());
                    assert logName != null;
                    boolean isLogTag = compo != null && compo.isLogTag;
                    addRecipe(recipes, logName, outputName, Mth.floor(result.getCount() / (double) stack.getCount()), isLogTag);
                    continue;
                }
            }
            WoodCompo compo = getWoodCompo(ingredients);
            if (compo == null) {
                continue;
            }
            if (weight < 1d) { // slab
                int count = Mth.floor(1 / weight);
                addRecipe(recipes, compo.logName(), outputName, count * 4, compo.isLogTag());
                addRecipe(recipes, compo.plankName(), outputName, count, compo.isPlankTag());
            } else if (weight >= 3.1d) { // boat / fence_gate
                addRecipe(recipes, compo.logName(), outputName, 1, compo.isLogTag());
            } else { // others
                addRecipe(recipes, compo.logName(), outputName, 4, compo.isLogTag());
                addRecipe(recipes, compo.plankName(), outputName, 1, compo.isPlankTag());
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
            context.getSource().sendSuccess(LangKey.DATAPACK_GENERATE_SUCCESS.getText(recipes.size()), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw LangKey.ZIP_CREATE_FAIL.asCommandException();
    }

    @Nullable
    private WoodCompo getWoodCompo(NonNullList<Ingredient> ingredients) {
        Set<Item> planks = new HashSet<>();
        int vanillaPlanks = (int) ingredients.stream().filter(ingredient -> !ingredient.isEmpty()).flatMap(ingredient -> Arrays.stream(ingredient.getItems())).filter(stack -> stack.is(ItemTags.PLANKS) && planks.add(stack.getItem()) && VANILLA_ITEM.test(stack)).count();
        if (planks.size() == 1) {
            return this.plankToLog.get(planks.iterator().next());
        } else if (planks.size() == 0 || vanillaPlanks > 1) {
            return new WoodCompo(ItemTags.PLANKS.getName(), true, ItemTags.LOGS.getName(), true);
        } else {
            return planks.stream().map(this.plankToLog::get).filter(Objects::nonNull).findFirst().orElse(lastResort(planks));
        }
    }

    @Nullable
    private WoodCompo lastResort(Collection<Item> planks) {
        for (Item plank : planks) {
            ResourceLocation plankRegistryName = plank.getRegistryName();
            assert plankRegistryName != null;
            String namespace = plankRegistryName.getNamespace();
            String path = plankRegistryName.getPath().replace("planks", "log");
            Item log = ForgeRegistries.ITEMS.getEntries().stream().filter(entry -> namespace.equals(entry.getKey().getRegistryName().getNamespace()) && entry.getKey().getRegistryName().getPath().contains(path)).map(Map.Entry::getValue).findFirst().orElse(null);
            if (log != null) {
                assert log.getRegistryName() != null;
                WoodCompo woodCompo = new WoodCompo(plankRegistryName, false, log.getRegistryName(), false);
                this.plankToLog.put(plank, woodCompo);
                return woodCompo;
            }
        }
        return null;
    }

    private record WoodCompo(ResourceLocation plankName, boolean isPlankTag, ResourceLocation logName, boolean isLogTag) {}

    private void initPlanksToLogs(MinecraftServer server) {
        // locate the logs to planks recipes to determine the pairs planks/logs and if a tag can be used
        if (!this.plankToLog.isEmpty()) {
            return;
        }
        final List<Recipe<CraftingContainer>> moddedLogToPlankRecipes = server.getRecipeManager().byType(RecipeType.CRAFTING).entrySet().stream().filter(entry -> !"minecraft".equals(entry.getKey().getNamespace())).map(Map.Entry::getValue).filter(r -> r.getIngredients().size() == 1).filter(ShapelessRecipe.class::isInstance).filter(r -> VALID_RESULT.test(r.getResultItem()) && r.getResultItem().is(ItemTags.PLANKS)).filter(r -> NON_VANILLA_LOG.test(r.getIngredients().get(0))).toList();
        for (Recipe<CraftingContainer> craftingRecipe : moddedLogToPlankRecipes) {
            if (craftingRecipe instanceof ShapelessRecipe) {
                this.plankToLog.computeIfAbsent(craftingRecipe.getResultItem().getItem(), plank -> {
                    ItemStack[] stacks = craftingRecipe.getIngredients().get(0).getItems();
                    ResourceLocation logName = stacks[0].getItem().getRegistryName();
                    ResourceLocation plankName = plank.getRegistryName();
                    assert logName != null && plankName != null;
                    boolean isLogTag = false;
                    for (ItemStack log : stacks) {
                        for (ResourceLocation rl : log.getItem().getTags()) {
                            if (plankName.getNamespace().equals(rl.getNamespace()) && rl.getPath().contains(logName.getPath())) {
                                logName = rl;
                                isLogTag = true;
                                break;
                            }
                        }
                        if (isLogTag) {
                            break;
                        }
                    }
                    return new WoodCompo(plankName, false, logName, isLogTag);
                });
            }
        }
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
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
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
                fw.write(GSON.toJson(new JsonParser().parse("{\"pack\":{\"description\":\"Corail WoodCutter " + StringUtils.capitalize(modid) + " Resources\",\"pack_format\":7}}")));
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

    private double getWeight(Recipe<CraftingContainer> recipe) {
        final NonNullList<Ingredient> ingredients = recipe.getIngredients();
        double weight = 0d;
        double maxWeight = 5d * recipe.getResultItem().getCount();
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                ItemStack[] stacks = ingredient.getItems();
                ItemStack stack = stacks[0];
                final Tag.Named<Item> tag;
                if (stack.is(ItemTags.LOGS)) {
                    tag = ItemTags.LOGS;
                    weight += 4d * stack.getCount();
                } else if (stack.is(ItemTags.PLANKS)) {
                    tag = ItemTags.PLANKS;
                    weight += 1d * stack.getCount();
                } else if (stack.is(Tags.Items.RODS_WOODEN)) {
                    tag = Tags.Items.RODS_WOODEN;
                    weight += 0.5d * stack.getCount();
                } else if (stack.is(ItemTags.WOODEN_SLABS)) {
                    tag = ItemTags.WOODEN_SLABS;
                    weight += 0.5d * stack.getCount();
                } else {
                    return 0d;
                }
                if (weight > maxWeight || Arrays.stream(stacks).anyMatch(s -> !s.is(tag))) {
                    return 0d;
                }
            }
        }
        return weight / (double) recipe.getResultItem().getCount();
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> command = dispatcher.register(Commands.literal("woodcutter").requires(p -> p.hasPermission(2))
                .executes(this::showUsage)
                .then(BaseAction.INFO.literal().executes(this::showUsage)
                ).then(BaseAction.DATAPACK.literal().executes(this::showUsage)
                        .then(DataPackAction.GENERATE.literal().executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::generateDataPack))
                        ).then(DataPackAction.APPLY.literal().requires(p -> p.getServer().isSingleplayer()).executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::applyDataPack))
                        ).then(DataPackAction.REMOVE.literal().requires(p -> p.getServer().isSingleplayer()).executes(this::showUsage)
                                .then(Commands.argument(MODID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::removeDataPack))
                        )
                )
        );
        dispatcher.register(Commands.literal("cwc").requires(p -> p.hasPermission(2)).redirect(command));
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
    private static final Predicate<ItemStack> VANILLA_ITEM = stack -> Optional.ofNullable(stack.getItem().getRegistryName()).map(ResourceLocation::getNamespace).map("minecraft"::equals).orElse(false);
    private static final Predicate<ItemStack> VALID_RESULT = result -> !result.isEmpty() && !VANILLA_ITEM.test(result);
    private static final Predicate<Ingredient> NON_VANILLA_LOG = ing -> !ing.isEmpty() && ing.getItems()[0].is(ItemTags.LOGS) && Arrays.stream(ing.getItems()).noneMatch(VANILLA_ITEM);
    private static final Predicate<String> INVALID_MODID = modid -> modid == null || "minecraft".equals(modid) || !ModList.get().isLoaded(modid) || SupportMods.hasSupport(modid);
    private static final BiFunction<MinecraftServer, String, File> DATAPACK_FOLDER = (server, folder) -> new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), folder);
    private static final Function<String, File> CONFIG_FOLDER = folder -> new File(FMLPaths.CONFIGDIR.get().toFile(), "corail_woodcutter" + File.separatorChar + folder);
    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_MODID = (ctx, build) -> SharedSuggestionProvider.suggest(ModList.get().applyForEachModContainer(ModContainer::getModId).filter(SupportMods::noSupport).filter(modid -> !"minecraft".equals(modid)), build);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
}
