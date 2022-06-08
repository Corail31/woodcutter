package ovh.corail.woodcutter.helper;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistryEntry;
import ovh.corail.woodcutter.config.ConfigWoodcutter;
import ovh.corail.woodcutter.config.CustomConfig;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeTypes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Helper {
    public static void initItemModels() {
        ModBlocks.WOODCUTTERS.forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout()));
    }

    public static void registerSharedConfig() {
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.getActiveContainer().addConfig(new CustomConfig(ConfigWoodcutter.GENERAL_SPEC, ctx.getActiveContainer()));
    }

    public static List<WoodcuttingRecipe> getSortedMatchingRecipes(Level level, Container inventory) {
        return level.getRecipeManager().byType(ModRecipeTypes.WOODCUTTING)
                .values().stream()
                .flatMap(recipe -> ModRecipeTypes.WOODCUTTING.tryMatch(recipe, level, inventory).stream())
                .sorted(RECIPE_COMPARATOR)
                .toList();
    }

    public static final Comparator<Recipe<Container>> RECIPE_COMPARATOR = Comparator.<Recipe<Container>, String>comparing(recipe -> {
        String[] name = getRegistryPath(recipe.getResultItem().getItem()).split("_");
        return name[name.length - 1];
    }).thenComparing(recipe -> getRegistryName(recipe.getResultItem().getItem()));

    public static ResourceLocation getRegistryRL(ItemStack stack) {
        return getRegistryRL(stack.getItem());
    }

    public static ResourceLocation getRegistryRL(IForgeRegistryEntry<?> entry) {
        return entry.getRegistryName();
    }

    public static String getRegistryNamespace(IForgeRegistryEntry<?> entry) {
        return Optional.ofNullable(entry.getRegistryName()).map(ResourceLocation::getNamespace).map(String::toString).orElse("");
    }

    public static String getRegistryPath(IForgeRegistryEntry<?> entry) {
        return Optional.ofNullable(entry.getRegistryName()).map(ResourceLocation::getPath).map(String::toString).orElse("");
    }

    public static String getRegistryName(IForgeRegistryEntry<?> entry) {
        return Optional.ofNullable(entry.getRegistryName()).map(ResourceLocation::toString).orElse("");
    }

    public static void fillItemSet(Set<Item> items, TagKey<Item> tagKey) {
        //noinspection deprecation
        Registry.ITEM.getTagOrEmpty(tagKey).forEach(holder -> items.add(holder.value()));
    }

    public static Iterable<Holder<Item>> getItems(TagKey<Item> tagKey) {
        //noinspection deprecation
        return Registry.ITEM.getTagOrEmpty(tagKey);
    }

    public static boolean isInTag(Item item, TagKey<Item> tagKey) {
        // TODO re-evaluate
        return StreamSupport.stream(Registry.ITEM.getTagOrEmpty(tagKey).spliterator(), false).anyMatch(holder -> holder.value() == item);
    }
}
