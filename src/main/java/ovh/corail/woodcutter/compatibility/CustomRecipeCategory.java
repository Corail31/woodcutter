package ovh.corail.woodcutter.compatibility;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleItemRecipe;

public class CustomRecipeCategory <T extends SingleItemRecipe> implements IRecipeCategory<T> {
    private static final ResourceLocation RECIPE_GUI_VANILLA = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
    private final ResourceLocation uid;
    private static final int WIDTH = 116, HEIGHT = 18;
    private final IDrawable background, icon;
    private final Component translation;
    private final Class<T> tClass;

    CustomRecipeCategory(Component translation, ResourceLocation uid, ItemStack icon, Class<T> tClass, IGuiHelper guiHelper) {
        this.uid = uid;
        this.background = guiHelper.drawableBuilder(RECIPE_GUI_VANILLA, 49, 168, WIDTH, HEIGHT).addPadding(0, 0, 40, 0).build();
        this.icon = guiHelper.createDrawableIngredient(icon);
        this.translation = translation;
        this.tClass = tClass;
    }

    @Override
    public ResourceLocation getUid() {
        return this.uid;
    }

    @Override
    public Class<T> getRecipeClass() {
        return this.tClass;
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
    public void setIngredients(T recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(recipe.getIngredients());
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getResultItem());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, T recipe, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
        guiItemStacks.init(0, true, 40, 0);
        guiItemStacks.init(1, false, 98, 0);
        guiItemStacks.set(0, ingredients.getInputs(VanillaTypes.ITEM).get(0));
        guiItemStacks.set(1, ingredients.getOutputs(VanillaTypes.ITEM).get(0));
    }
}
