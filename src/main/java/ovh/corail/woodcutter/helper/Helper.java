package ovh.corail.woodcutter.helper;

import net.minecraft.Util;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import ovh.corail.woodcutter.config.ConfigWoodcutter;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeTypes;
import ovh.corail.woodcutter.config.CustomConfig;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@SuppressWarnings("unused")
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
                .flatMap(recipe -> Util.toStream(ModRecipeTypes.WOODCUTTING.tryMatch(recipe, level, inventory)))
                .sorted(recipeComparator)
                .collect(Collectors.toList());
    }

    public static final Comparator<Recipe<Container>> recipeComparator = (r1, r2) -> {
        ResourceLocation registryName1 = r1.getResultItem().getItem().getRegistryName();
        ResourceLocation registryName2 = r2.getResultItem().getItem().getRegistryName();
        assert registryName1 != null && registryName2 != null;
        String[] name1 = registryName1.getPath().split("_");
        String[] name2 = registryName2.getPath().split("_");
        int comp = name1[name1.length - 1].compareTo(name2[name2.length - 1]);
        return comp == 0 ? registryName1.compareTo(registryName2) : comp;
    };
}
