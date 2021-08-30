package ovh.corail.woodcutter.compatibility;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeTypes;
import ovh.corail.woodcutter.registry.ModTabs;

import java.util.Optional;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@SuppressWarnings("unused")
@JeiPlugin
public class IntegrationJEI implements IModPlugin {
    private static final ResourceLocation WOOD_RL = new ResourceLocation(MOD_ID, "woodcutting");

    @Override
    public ResourceLocation getPluginUid() {
        return WOOD_RL;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new CustomRecipeCategory<>(WoodcutterBlock.TRANSLATION, WOOD_RL, ModTabs.mainTab.getIconItem(), WoodcuttingRecipe.class, registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Optional.ofNullable(Minecraft.getInstance().level).ifPresent(w -> registration.addRecipes(w.getRecipeManager().byType(ModRecipeTypes.WOODCUTTING).values().stream().sorted(Helper.recipeComparator).toList(), WOOD_RL));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ModBlocks.WOODCUTTERS.forEach(woodcutter -> registration.addRecipeCatalyst(new ItemStack(woodcutter), WOOD_RL));
    }
}
