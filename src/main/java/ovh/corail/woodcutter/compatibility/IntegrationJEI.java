package ovh.corail.woodcutter.compatibility;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
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

import java.util.Optional;
import java.util.stream.Collectors;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@SuppressWarnings("unused")
@JeiPlugin
public class IntegrationJEI implements IModPlugin {
    private static final ResourceLocation WOOD_RL = new ResourceLocation(MOD_ID, "woodcutting");
    private final RecipeType<WoodcuttingRecipe> recipeType;

    public IntegrationJEI() {
        this.recipeType = RecipeType.create(MOD_ID, "woodcutting", WoodcuttingRecipe.class);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return WOOD_RL;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new CustomRecipeCategory<>(this.recipeType, WoodcutterBlock.TRANSLATION, ModBlocks.createRandomStack(), registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Optional.ofNullable(Minecraft.getInstance().level).ifPresent(level -> registration.addRecipes(recipeType, level.getRecipeManager().byType(ModRecipeTypes.WOODCUTTING).values().stream().map(WoodcuttingRecipe.class::cast).sorted(Helper.RECIPE_COMPARATOR.apply(level)).collect(Collectors.toList())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ModBlocks.WOODCUTTERS.forEach(woodcutter -> registration.addRecipeCatalyst(new ItemStack(woodcutter), recipeType));
    }
}
