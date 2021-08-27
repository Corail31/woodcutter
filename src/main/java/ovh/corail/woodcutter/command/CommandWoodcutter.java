package ovh.corail.woodcutter.command;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
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
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionItem;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.ConditionMod;
import ovh.corail.woodcutter.command.WoodcuttingJsonRecipe.Conditions;
import ovh.corail.woodcutter.compatibility.SupportMods;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Files.*;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandWoodcutter {
    private final File BASE_FOLDER = new File(FMLPaths.CONFIGDIR.get().toFile(), "corail_woodcutter");

    private int showUsage(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("Generate the wooden crafting recipes of a mod in the woodcutter format in your config folder\n/woodcutter info\n/woodcutter gen_datapack <modid>\n/woodcutter apply_datapack <modid>\n/woodcutter remove_datapack <modid>"), false);
        return 1;
    }

    private int applyDataPack(CommandContext<CommandSource> context) {
        String modid = getModid(context);
        if (isInvalidModid(modid)) {
            context.getSource().sendErrorMessage(new StringTextComponent("Invalid modid"));
            return 0;
        }
        File datapackFile = new File(BASE_FOLDER, "corail_woodcutter_" + modid + ".zip");
        if (!datapackFile.exists()) {
            context.getSource().sendErrorMessage(new StringTextComponent("No generated datapack for modid: " + modid));
            return 0;
        }
        File destination = new File(context.getSource().getServer().func_240776_a_(FolderName.DATAPACKS).toFile(), "corail_woodcutter_" + modid + ".zip");
        if (destination.exists() && !destination.delete()) {
            context.getSource().sendErrorMessage(new StringTextComponent("The datapack wasn't applied, file can't be deleted " + destination.getAbsolutePath()));
            return 0;
        }
        try {
            FileUtils.copyFile(datapackFile, destination);
            reload(context.getSource().getServer());
            context.getSource().sendFeedback(new StringTextComponent("The datapack was added"), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        context.getSource().sendErrorMessage(new StringTextComponent("The datapack wasn't applied, file can't be copied " + datapackFile.getAbsolutePath()));
        return 0;
    }

    private int removeDataPack(CommandContext<CommandSource> context) {
        String modid = getModid(context);
        if (isInvalidModid(modid)) {
            context.getSource().sendErrorMessage(new StringTextComponent("Invalid modid"));
            return 0;
        }
        File destination = new File(context.getSource().getServer().func_240776_a_(FolderName.DATAPACKS).toFile(), "corail_woodcutter_" + modid + ".zip");
        if (!destination.exists() || !destination.delete()) {
            context.getSource().sendErrorMessage(new StringTextComponent("The datapack was absent, or can't be deleted " + destination.getAbsolutePath()));
            return 0;
        }
        reload(context.getSource().getServer());
        context.getSource().sendFeedback(new StringTextComponent("The datapack was removed"), false);
        return 1;
    }

    private int genDataPack(CommandContext<CommandSource> context) {
        String modid = getModid(context);
        if (isInvalidModid(modid)) {
            context.getSource().sendErrorMessage(new StringTextComponent("Invalid modid"));
            return 0;
        }

        final Map<String, WoodcuttingJsonRecipe> recipes = new HashMap<>();
        final List<IRecipe<CraftingInventory>> craftingRecipes = context.getSource().getServer().getRecipeManager().getRecipes(IRecipeType.CRAFTING).entrySet().stream().filter(entry -> modid.equals(entry.getKey().getNamespace())).map(Map.Entry::getValue).filter(r -> !r.getRecipeOutput().isEmpty() && !"minecraft".equals(Objects.requireNonNull(r.getRecipeOutput().getItem().getRegistryName()).getNamespace())).filter(r -> r.getIngredients().stream().allMatch(ing -> Arrays.stream(ing.getMatchingStacks()).allMatch(stack -> VALID_INGREDIENT.test(stack.getItem())))).collect(Collectors.toList());
        final Map<Item, Pair<ResourceLocation, Boolean>> plankToLog = new HashMap<>();
        for (IRecipe<CraftingInventory> craftingRecipe : craftingRecipes) {
            if (craftingRecipe instanceof ShapelessRecipe) {
                final NonNullList<Ingredient> ingredients = craftingRecipe.getIngredients();
                if (ingredients.size() == 1 && craftingRecipe.getRecipeOutput().getItem().isIn(ItemTags.PLANKS)) {
                    final ItemStack[] stacks = ingredients.get(0).getMatchingStacks();
                    if (stacks.length > 0 && stacks[0].getItem().isIn(ItemTags.LOGS)) {
                        plankToLog.computeIfAbsent(craftingRecipe.getRecipeOutput().getItem(), plank -> {
                            Item log = stacks[0].getItem();
                            ResourceLocation logName = log.getRegistryName();
                            boolean isTag = false;
                            assert logName != null && plank.getRegistryName() != null;
                            for (ResourceLocation rl : log.getTags()) {
                                if (modid.equals(rl.getNamespace()) && (rl.getPath().contains(logName.getPath()))) {
                                    logName = rl;
                                    isTag = true;
                                    break;
                                }
                            }
                            addRecipe(recipes, logName, plank.getRegistryName(), MathHelper.floor(craftingRecipe.getRecipeOutput().getCount() / (double) stacks[0].getCount()), isTag);
                            return Pair.of(logName, isTag);
                        });
                    }
                }
            }
        }

        for (IRecipe<CraftingInventory> craftingRecipe : craftingRecipes) {
            List<ItemStack[]> ingredients = craftingRecipe.getIngredients().stream().map(Ingredient::getMatchingStacks).filter(stacks -> stacks.length > 0 && !stacks[0].isEmpty()).collect(Collectors.toList());
            if (ingredients.size() == 0) {
                continue;
            }
            Item result = craftingRecipe.getRecipeOutput().getItem();
            if (result == Items.BOWL || result == Items.STICK || result == Items.LADDER) {
                continue; // handled by default
            }

            ResourceLocation outputName = result.getRegistryName();
            Item plank = ingredients.stream().map(stacks -> stacks[0].getItem()).filter(ItemTags.PLANKS::contains).filter(p -> !"minecraft".equals(Objects.requireNonNull(p.getRegistryName()).getNamespace())).findFirst().orElse(Items.OAK_PLANKS);
            ResourceLocation plankName = plank.getRegistryName();

            Pair<ResourceLocation, Boolean> pair = plankToLog.get(plank);
            final boolean isTag;
            final ResourceLocation logName;
            if (pair != null) {
                isTag = pair.getRight();
                logName = pair.getLeft();
            } else {
                isTag = false;
                logName = ingredients.stream().map(stacks -> stacks[0].getItem()).filter(ItemTags.LOGS::contains).filter(p -> !"minecraft".equals(Objects.requireNonNull(p.getRegistryName()).getNamespace())).findFirst().orElse(Items.OAK_LOG).getRegistryName();
            }
            assert outputName != null && plankName != null && logName != null;

            if (ingredients.size() == 1 && result.isIn(ItemTags.PLANKS) && ingredients.get(0)[0].getItem().isIn(ItemTags.LOGS)) {
                continue; // already done
            }

            double weight = ingredients.stream().mapToDouble(stacks -> getWeight(stacks[0])).sum() / craftingRecipe.getRecipeOutput().getCount();
            if (weight == 0d || weight > 5d) {
                continue;
            }
            if (weight < 1d) {
                int count = MathHelper.floor(1 / weight);
                addRecipe(recipes, logName, outputName, count * 4, isTag);
                addRecipe(recipes, plankName, outputName, count, isTag);
            } else if (weight >= 3.1d && weight <= 5d) {
                addRecipe(recipes, logName, outputName, 1, isTag);
            } else {
                addRecipe(recipes, logName, outputName, 4, isTag);
                addRecipe(recipes, plankName, outputName, 1, isTag);
            }
        }
        if (recipes.isEmpty()) {
            context.getSource().sendErrorMessage(new StringTextComponent("No valid recipes were found for the modid " + modid));
            return 0;
        }
        String datapack = MOD_ID + "_" + modid;
        File datapackFile = new File(BASE_FOLDER, datapack);
        File recipeFile = new File(datapackFile, "data" + File.separatorChar + "recipes");
        if (recipeFile.exists()) {
            try {
                FileUtils.cleanDirectory(recipeFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        recipeFile.mkdirs();
        for (Map.Entry<String, WoodcuttingJsonRecipe> entry : recipes.entrySet()) {
            entry.getValue().conditions = new Conditions[3];
            entry.getValue().conditions[0] = new ConditionMod(modid);
            entry.getValue().conditions[1] = new ConditionMod(MOD_ID);
            entry.getValue().conditions[2] = new ConditionItem(entry.getValue().result);
            File file = new File(recipeFile, entry.getKey() + ".json");
            if (file.exists() && !file.delete()) {
                context.getSource().sendErrorMessage(new StringTextComponent("The datapack wasn't generated, file can't be deleted " + file.getAbsolutePath()));
                return 0;
            }
            if (!toFile(file, entry.getValue())) {
                context.getSource().sendErrorMessage(new StringTextComponent("The datapack wasn't generated, file can't be written " + file.getAbsolutePath()));
                return 0;
            }
        }
        addMcMeta(datapackFile, modid);
        try {
            toZip(datapackFile.toPath(), modid);
            context.getSource().sendFeedback(new StringTextComponent("The datapack was generated in your config folder (" + recipes.size() + " recipes)"), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        context.getSource().sendErrorMessage(new StringTextComponent("The datapack wasn't generated, zip failed"));
        return 0;
    }

    private void toZip(Path source, String modid) throws IOException {
        try (final ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(new File(BASE_FOLDER, "corail_woodcutter_" + modid + ".zip").toPath())))) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path visitedPath, BasicFileAttributes attributes) {
                    try {
                        if (!visitedPath.toFile().isDirectory() && !visitedPath.endsWith("session.lock")) { // TODO recheck
                            String stringPath = visitedPath.toString();
                            ZipEntry zipentry = new ZipEntry(stringPath.substring(stringPath.indexOf("corail_woodcutter_" + modid)).replace('\\', '/'));
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

    private void reload(MinecraftServer server) {
        ResourcePackList packList = server.getResourcePacks();
        Collection<String> collection = Lists.newArrayList(packList.func_232621_d_());
        packList.reloadPacksFromFinders();
        Collection<String> collection1 = server.getServerConfiguration().getDatapackCodec().getDisabled();

        for(String s : packList.func_232616_b_()) {
            if (!collection1.contains(s) && !collection.contains(s)) {
                collection.add(s);
            }
        }
        server.func_240780_a_(collection);
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

    private void addMcMeta(File datapackFile, String modid) {
        try {
            File file = new File(datapackFile, "pack.mcmeta");
            if (file.exists() && !file.delete()) {
                return;
            }
            if (file.createNewFile()) {
                FileWriter fw = new FileWriter(file);
                fw.write(GSON.toJson(new JsonParser().parse("{\"pack\":{\"description\":\"Corail WoodCutter " + modid + " Resources\",\"pack_format\":5}}")));
                fw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addRecipe(final Map<String, WoodcuttingJsonRecipe> recipes, ResourceLocation input, ResourceLocation output, int count, boolean isTag) {
        recipes.put(output.getPath() + " from " + input.getPath(), new WoodcuttingJsonRecipe(input.toString(), output.toString(), count, isTag));
    }

    private double getWeight(ItemStack stack) {
        return stack.getCount() * (stack.getItem().isIn(ItemTags.LOGS) ? 4d : stack.getItem().isIn(ItemTags.PLANKS) ? 1d : 0.5d);
    }

    @Nullable
    private String getModid(CommandContext<CommandSource> source) {
        try{
            String modid = source.getArgument("modid", String.class);
            if (ModList.get().isLoaded(modid)) {
                return modid;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    private boolean isInvalidModid(@Nullable String modid) {
        return modid == null || "minecraft".equals(modid) || Arrays.stream(SupportMods.values()).map(SupportMods::getString).anyMatch(modid::equals);
    }

    private LiteralArgumentBuilder<CommandSource> getBuilder() {
        return Commands.literal("woodcutter")
                .requires(p -> p.hasPermissionLevel(2))
                .executes(this::showUsage)
                    .then(CommandAction.INFO.subCommand().executes(this::showUsage)
                    ).then(CommandAction.GEN_DATAPACK.subCommand().executes(this::showUsage)
                        .then(Commands.argument("modid", StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::genDataPack))
                    ).then(CommandAction.APPLY_DATAPACK.subCommand().executes(this::showUsage)
                        .then(Commands.argument("modid", StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::applyDataPack))
                    ).then(CommandAction.REMOVE_DATAPACK.subCommand().executes(this::showUsage)
                        .then(Commands.argument("modid", StringArgumentType.word()).suggests(SUGGESTION_MODID).executes(this::removeDataPack))
                    );
    }

    private static final Predicate<Item> VALID_INGREDIENT = item -> item == Items.AIR || item.isIn(ItemTags.LOGS) || item.isIn(ItemTags.PLANKS) || item.isIn(Tags.Items.RODS_WOODEN);

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onServerStarting(RegisterCommandsEvent event) {
        event.getDispatcher().register(new CommandWoodcutter().getBuilder());
    }

    private enum CommandAction implements IStringSerializable {
        INFO, GEN_DATAPACK, APPLY_DATAPACK, REMOVE_DATAPACK;
        private final String name;

        CommandAction() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getString() {
            return this.name;
        }

        public LiteralArgumentBuilder<CommandSource> subCommand() {
            return Commands.literal(this.name);
        }
    }

    private static final SuggestionProvider<CommandSource> SUGGESTION_MODID = (ctx, build) -> {
        List<String> modids = ModList.get().getMods().stream().map(IModInfo::getModId).collect(Collectors.toList());
        List<String> supportMods = Arrays.stream(SupportMods.values()).map(SupportMods::getString).collect(Collectors.toList());
        modids.removeIf(modid -> modid.equals("minecraft") || supportMods.contains(modid));
        return ISuggestionProvider.suggest(modids.stream(), build);
    };
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
}
