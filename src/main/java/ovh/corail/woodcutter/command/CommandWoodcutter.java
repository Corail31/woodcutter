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
import net.minecraft.util.IStringSerializable;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionItem;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionMod;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.helper.LangKey;

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

    private int showUsage(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(LangKey.COMMAND_USAGE.getText(), false);
        return 1;
    }

    private int applyDataPack(CommandContext<CommandSource> context) {
        // copy the generated datapack in the datapack folder
        String modid = StringArgumentType.getString(context, MODID);
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
            reload(context.getSource().getServer());
            context.getSource().sendFeedback(LangKey.DATAPACK_APPLY_SUCCESS.getText(), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw LangKey.DATAPACK_APPLY_FAIL.asCommandException(LangKey.FILE_COPY_FAIL.getText(destination.getAbsolutePath()));
    }

    private int removeDataPack(CommandContext<CommandSource> context) {
        // delete the datapack from the datapack folder
        String modid = StringArgumentType.getString(context, MODID);
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
        reload(context.getSource().getServer());
        context.getSource().sendFeedback(LangKey.DATAPACK_REMOVE_SUCCESS.getText(), false);
        return 1;
    }

    private int generateDataPack(CommandContext<CommandSource> context) {
        // try to determine the existing wooden recipes, then generate theses recipes in the woodcutter format in the config folder and finalize that folder in a zip
        String modid = StringArgumentType.getString(context, MODID);
        if (INVALID_MODID.test(modid)) {
            throw LangKey.INVALID_MODID.asCommandException();
        }
        final Map<String, WoodcuttingJsonRecipe> recipes = new HashMap<>();
        initPlanksToLogs(context.getSource().getServer());

        final List<IRecipe<CraftingInventory>> craftingRecipes = context.getSource().getServer().getRecipeManager().getRecipes(IRecipeType.CRAFTING).entrySet().stream().filter(entry -> modid.equals(entry.getKey().getNamespace())).map(Map.Entry::getValue).filter(r -> !r.getRecipeOutput().isEmpty() && !"minecraft".equals(Objects.requireNonNull(r.getRecipeOutput().getItem().getRegistryName()).getNamespace())).filter(r -> r.getIngredients().stream().allMatch(ing -> Arrays.stream(ing.getMatchingStacks()).allMatch(stack -> VALID_INGREDIENT.test(stack.getItem())))).collect(Collectors.toList());
        for (IRecipe<CraftingInventory> craftingRecipe : craftingRecipes) {
            List<ItemStack[]> ingredients = craftingRecipe.getIngredients().stream().map(Ingredient::getMatchingStacks).filter(stacks -> stacks.length > 0 && !stacks[0].isEmpty()).collect(Collectors.toList());
            if (ingredients.size() == 0) {
                continue;
            }
            Item result = craftingRecipe.getRecipeOutput().getItem();
            // theses recipes are handled by default with the tag for any planks/logs
            if (result == Items.BOWL || result == Items.STICK || result == Items.LADDER) {
                continue;
            }
            ResourceLocation outputName = result.getRegistryName();
            assert outputName != null;
            WoodCompo compo = getWoodCompo(ingredients);
            if (compo == WoodCompo.INVALID) {
                continue;
            }

            // logs to planks recipes
            if (ingredients.size() == 1 && result.isIn(ItemTags.PLANKS) && ingredients.get(0)[0].getItem().isIn(ItemTags.LOGS)) {
                addRecipe(recipes, compo.logName, compo.plankName, MathHelper.floor(craftingRecipe.getRecipeOutput().getCount() / (double) ingredients.get(0)[0].getCount()), compo.isLogTag);
                continue;
            }

            double weight = ingredients.stream().mapToDouble(stacks -> getWeight(stacks[0])).sum() / (double) craftingRecipe.getRecipeOutput().getCount();
            if (weight == 0d || weight > 5d) {
                continue;
            }
            if (weight < 1d) { // slab
                int count = MathHelper.floor(1 / weight);
                addRecipe(recipes, compo.logName, outputName, count * 4, compo.isLogTag);
                addRecipe(recipes, compo.plankName, outputName, count, compo.isPlankTag);
            } else if (weight >= 3.1d && weight <= 5d) { // boat / fence_gate
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
            if (!toFile(file, entry.getValue().setConditions(new ConditionMod(modid), new ConditionMod(MOD_ID), new ConditionItem(entry.getValue().result)))) {
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

    private WoodCompo getWoodCompo(List<ItemStack[]> ingredients) {
        Set<Item> planks = new HashSet<>();
        int vanillaPlanks = 0;
        for (ItemStack[] stacks : ingredients) {
            for (ItemStack stack : stacks) {
                if (stack.getItem().isIn(ItemTags.PLANKS)) {
                    if (planks.add(stack.getItem()) && Optional.ofNullable(stack.getItem().getRegistryName()).map(ResourceLocation::getNamespace).filter("minecraft"::equals).isPresent()) {
                        vanillaPlanks++;
                    }
                }
            }
        }
        if (planks.size() == 1) {
            Item plankItem = planks.iterator().next();
            WoodCompo woodCompo = this.plankToLog.get(plankItem);
            return new WoodCompo(woodCompo.plankName, woodCompo.isPlankTag, woodCompo.logName, woodCompo.isLogTag);
        } else if (planks.size() == 0 || vanillaPlanks > 1) {
            return new WoodCompo(ItemTags.PLANKS.getName(), true, ItemTags.LOGS.getName(), true);
        } else {
            WoodCompo woodCompo = planks.stream().map(this.plankToLog::get).filter(Objects::nonNull).findFirst().orElse(null);
            if (woodCompo != null) {
                return new WoodCompo(woodCompo.plankName, woodCompo.isPlankTag, woodCompo.logName, woodCompo.isLogTag);
            } else {
                for (Item plank : planks) {
                    ResourceLocation plankRegistryName = plank.getRegistryName();
                    assert plankRegistryName != null;
                    String namespace = plankRegistryName.getNamespace();
                    String path = plankRegistryName.getPath().replace("planks", "log");
                    Item log = ForgeRegistries.ITEMS.getEntries().stream().filter(entry -> namespace.equals(entry.getKey().getRegistryName().getNamespace()) && entry.getKey().getRegistryName().getPath().contains(path)).map(Map.Entry::getValue).findFirst().orElse(null);
                    if (log != null) {
                        assert log.getRegistryName() != null;
                        this.plankToLog.put(plank, (woodCompo = new WoodCompo(plankRegistryName, false, log.getRegistryName(), false)));
                        return woodCompo;
                    }
                }
            }
        }
        return WoodCompo.INVALID;
    }

    private static class WoodCompo {
        private static final WoodCompo INVALID = new WoodCompo(null, false, null, false);
        private final ResourceLocation plankName, logName;
        private final boolean isPlankTag, isLogTag;

        public WoodCompo(ResourceLocation plankName, boolean isPlankTag, ResourceLocation logName, boolean isLogTag) {
            this.plankName = plankName;
            this.isPlankTag = isPlankTag;
            this.logName = logName;
            this.isLogTag = isLogTag;
        }
    }

    private boolean isNonVanillaPlank(ItemStack stack) {
        return !stack.isEmpty() && !"minecraft".equals(Objects.requireNonNull(stack.getItem().getRegistryName()).getNamespace()) && stack.getItem().isIn(ItemTags.PLANKS);
    }

    private boolean isNonVanillaLog(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getMatchingStacks();
        return stacks.length > 0 && !stacks[0].isEmpty() && stacks[0].getItem().isIn(ItemTags.LOGS) && Arrays.stream(stacks).noneMatch(stack -> "minecraft".equals(Objects.requireNonNull(stack.getItem().getRegistryName()).getNamespace()));
    }

    private void initPlanksToLogs(MinecraftServer server) {
        // locate the logs to planks recipes to determine the pairs planks/logs and if a tag can be used
        if (!this.plankToLog.isEmpty()) {
            return;
        }
        final List<IRecipe<CraftingInventory>> moddedLogToPlankRecipes = server.getRecipeManager().getRecipes(IRecipeType.CRAFTING).entrySet().stream().filter(entry -> !"minecraft".equals(entry.getKey().getNamespace())).map(Map.Entry::getValue).filter(r -> r.getIngredients().size() == 1).filter(ShapelessRecipe.class::isInstance).filter(r -> isNonVanillaPlank(r.getRecipeOutput())).filter(r -> isNonVanillaLog(r.getIngredients().get(0))).collect(Collectors.toList());
        for (IRecipe<CraftingInventory> craftingRecipe : moddedLogToPlankRecipes) {
            if (craftingRecipe instanceof ShapelessRecipe) {
                this.plankToLog.computeIfAbsent(craftingRecipe.getRecipeOutput().getItem(), plank -> {
                    ItemStack[] stacks = craftingRecipe.getIngredients().get(0).getMatchingStacks();
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
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @SuppressWarnings("UnstableApiUsage")
                @Override
                public FileVisitResult visitFile(Path visitedPath, BasicFileAttributes attributes) {
                    try {
                        if (!visitedPath.toFile().isDirectory() && !visitedPath.endsWith("session.lock")) {
                            String stringPath = visitedPath.toString();
                            ZipEntry zipentry = new ZipEntry(stringPath.endsWith("pack.mcmeta") ? "pack.mcmeta" : stringPath.substring(stringPath.indexOf("data")).replace('\\', '/'));
                            outputStream.putNextEntry(zipentry);
                            asByteSource(visitedPath.toFile()).copyTo(outputStream);
                            outputStream.closeEntry();
                        }
                    } catch (Throwable ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void disableDataPack(CommandSource source, String modid) {
        // disable datapack if enabled (without reload)
        ResourcePackList packs = source.getServer().getResourcePacks();
        ResourcePackInfo packInfo = packs.getPackInfo("file/" + getZipName(modid));
        if (packInfo != null && packs.getEnabledPacks().contains(packInfo)) {
            List<ResourcePackInfo> enabledPackInfos = Lists.newArrayList(packs.getEnabledPacks());
            enabledPackInfos.remove(packInfo);
            source.getServer().func_240780_a_(enabledPackInfos.stream().map(ResourcePackInfo::getName).collect(Collectors.toList()));
        }
    }

    private void reload(MinecraftServer server) {
        ResourcePackList packs = server.getResourcePacks();
        Collection<String> enabledPackInfos = Lists.newArrayList(server.getResourcePacks().func_232621_d_());
        packs.reloadPacksFromFinders();
        Collection<String> disabledPackInfos = server.getServerConfiguration().getDatapackCodec().getDisabled();
        for (String packInfo : packs.func_232616_b_()) {
            if (!disabledPackInfos.contains(packInfo) && !enabledPackInfos.contains(packInfo)) {
                enabledPackInfos.add(packInfo);
            }
        }
        server.func_240780_a_(enabledPackInfos);
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
                fw.write(GSON.toJson(new JsonParser().parse("{\"pack\":{\"description\":\"Corail WoodCutter " + StringUtils.capitalize(modid) + " Resources\",\"pack_format\":5}}")));
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

    private double getWeight(ItemStack stack) {
        return stack.getCount() * (stack.getItem().isIn(ItemTags.LOGS) ? 4d : stack.getItem().isIn(ItemTags.PLANKS) ? 1d : 0.5d);
    }

    private void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("woodcutter").requires(p -> p.hasPermissionLevel(2))
                .executes(this::showUsage)
                .then(BaseAction.INFO.literal().executes(this::showUsage)
                ).then(BaseAction.DATAPACK.literal().executes(this::showUsage)
                        .then(DatapackAction.GENERATE.literal().executes(this::showUsage)
                                .then(Commands.argument(MODID, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::generateDataPack))
                        ).then(DatapackAction.APPLY.literal().requires(p -> p.getServer().isSinglePlayer()).executes(this::showUsage)
                                .then(Commands.argument(MODID, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::applyDataPack))
                        ).then(DatapackAction.REMOVE.literal().requires(p -> p.getServer().isSinglePlayer()).executes(this::showUsage)
                                .then(Commands.argument(MODID, StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::removeDataPack))
                        )
                )
        );
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onServerStarting(RegisterCommandsEvent event) {
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

    private enum DatapackAction implements IAction {
        GENERATE, APPLY, REMOVE;
        private final String name;

        DatapackAction() {
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

    private static final String MODID = "modid";
    private static final Predicate<String> INVALID_MODID = modid -> modid == null || "minecraft".equals(modid) || (!ModList.get().isLoaded(modid) || SupportMods.hasSupport(modid));
    private static final Predicate<Item> VALID_INGREDIENT = item -> item == Items.AIR || item.isIn(ItemTags.LOGS) || item.isIn(ItemTags.PLANKS) || item.isIn(Tags.Items.RODS_WOODEN);
    private static final BiFunction<MinecraftServer, String, File> DATAPACK_FOLDER = (server, folder) -> new File(server.func_240776_a_(FolderName.DATAPACKS).toFile(), folder);
    private static final Function<String, File> CONFIG_FOLDER = folder -> new File(FMLPaths.CONFIGDIR.get().toFile(), "corail_woodcutter" + File.separatorChar + folder);
    private static final SuggestionProvider<CommandSource> SUGGESTION_MODID = (ctx, build) -> ISuggestionProvider.suggest(ModList.get().applyForEachModContainer(ModContainer::getModId).filter(SupportMods::noSupport).filter(modid -> !"minecraft".equals(modid)), build);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
}
