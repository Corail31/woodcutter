package ovh.corail.woodcutter.compatibility;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeTypes;
import ovh.corail.woodcutter.registry.ModTabs;

import java.util.Collection;

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
        registry.addRecipeCategories(new CustomRecipeCategory<>("container.corail_woodcutter.woodcutter", WOOD_RL, ModTabs.mainTab.getIcon(), WoodcuttingRecipe.class, registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Collection<IRecipe<IInventory>> recipes = Minecraft.getInstance().world.getRecipeManager().getRecipes(ModRecipeTypes.WOODCUTTING).values();
        registration.addRecipes(recipes, WOOD_RL);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ModBlocks.WOODCUTTERS.forEach(woodcutter -> registration.addRecipeCatalyst(new ItemStack(woodcutter), WOOD_RL));
    }
}
