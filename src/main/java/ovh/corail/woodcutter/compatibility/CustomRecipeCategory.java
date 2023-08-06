package ovh.corail.woodcutter.compatibility;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleItemRecipe;

public class CustomRecipeCategory <T extends SingleItemRecipe> implements IRecipeCategory<T> {
    private static final ResourceLocation RECIPE_GUI_VANILLA = new ResourceLocation("jei", "textures/jei/gui/gui_vanilla.png");
    private static final int WIDTH = 82, HEIGHT = 34;
    private final RecipeType<T> recipeType;
    private final Component translation;
    private final IDrawable icon, background;

    CustomRecipeCategory(RecipeType<T> recipeType, Component translation, ItemStack icon, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.translation = translation;
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon);
        this.background = guiHelper.createDrawable(RECIPE_GUI_VANILLA, 0, 220, WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return this.translation;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 9).addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 9).addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public boolean isHandled(T recipe) {
        return true;
    }
}
