package ovh.corail.woodcutter.helper;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModLoadingContext;
import ovh.corail.woodcutter.client.gui.WoodcutterScreen;
import ovh.corail.woodcutter.config.ConfigWoodcutter;
import ovh.corail.woodcutter.config.CustomModConfig;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModContainers;
import ovh.corail.woodcutter.registry.ModRecipeTypes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Helper {
    public static void initItemModels() {
        ScreenManager.registerFactory(ModContainers.WOODCUTTER, WoodcutterScreen::new);
        ModBlocks.WOODCUTTERS.forEach(block -> RenderTypeLookup.setRenderLayer(block, RenderType.getCutout()));
    }

    public static void registerSharedConfig() {
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.getActiveContainer().addConfig(new CustomModConfig(ConfigWoodcutter.GENERAL_SPEC, ctx.getActiveContainer()));
    }

    public static List<WoodcuttingRecipe> getSortedMatchingRecipes(World world, IInventory inventory) {
        return world.getRecipeManager().getRecipes(ModRecipeTypes.WOODCUTTING)
                .values().stream()
                .flatMap(recipe -> Util.streamOptional(ModRecipeTypes.WOODCUTTING.matches(recipe, world, inventory)))
                .sorted(recipeComparator)
                .collect(Collectors.toList());
    }

    private static final Comparator<WoodcuttingRecipe> recipeComparator = (r1, r2) -> {
        ResourceLocation registryName1 = r1.getRecipeOutput().getItem().getRegistryName();
        ResourceLocation registryName2 = r2.getRecipeOutput().getItem().getRegistryName();
        assert registryName1 != null && registryName2 != null;
        String[] name1 = registryName1.getPath().split("_");
        String[] name2 = registryName2.getPath().split("_");
        int comp = name1[name1.length - 1].compareTo(name2[name2.length - 1]);
        return comp == 0 ? registryName1.compareTo(registryName2) : comp;
    };
}
