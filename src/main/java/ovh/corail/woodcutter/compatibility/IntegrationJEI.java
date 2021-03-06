package ovh.corail.woodcutter.compatibility;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeTypes;
import ovh.corail.woodcutter.registry.ModTabs;

import java.util.Optional;
import java.util.stream.Collectors;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@JeiPlugin
public class IntegrationJEI implements IModPlugin {
    private static final ResourceLocation WOOD_RL = new ResourceLocation(MOD_ID, "woodcutting");

    @Override
    public ResourceLocation getPluginUid() {
        return WOOD_RL;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new CustomRecipeCategory<>(WoodcutterBlock.TRANSLATION, WOOD_RL, ModTabs.mainTab.getIcon(), WoodcuttingRecipe.class, registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Optional.ofNullable(Minecraft.getInstance().world).ifPresent(w -> {
            registration.addRecipes(w.getRecipeManager().getRecipes(ModRecipeTypes.WOODCUTTING).values().stream().sorted(Helper.recipeComparator).collect(Collectors.toList()), WOOD_RL);
        });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ModBlocks.WOODCUTTERS.forEach(woodcutter -> registration.addRecipeCatalyst(new ItemStack(woodcutter), WOOD_RL));
    }
}
