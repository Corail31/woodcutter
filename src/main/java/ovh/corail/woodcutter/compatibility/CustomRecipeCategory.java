package ovh.corail.woodcutter.compatibility;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleItemRecipe;

public class CustomRecipeCategory<T extends SingleItemRecipe> implements IRecipeCategory<T> {
    private static final ResourceLocation RECIPE_GUI_VANILLA = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
    private final ResourceLocation uid;
    private static final int WIDTH = 116, HEIGHT = 18;
    private final IDrawable background, icon;
    private final Component translation;
    private final Class<T> tClass;

    CustomRecipeCategory(Component translation, ResourceLocation uid, ItemStack icon, Class<T> tClass, IGuiHelper guiHelper) {
        this.uid = uid;
        this.background = guiHelper.drawableBuilder(RECIPE_GUI_VANILLA, 49, 168, WIDTH, HEIGHT).addPadding(0, 0, 40, 0).build();
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM, icon);
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
    public void setRecipe(IRecipeLayoutBuilder recipeLayoutBuilder, T recipe, IFocusGroup focuses) {
        recipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 40, 0).addIngredients(recipe.getIngredients().get(0));
        recipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 98, 0).addItemStack(recipe.getResultItem());
    }

    @Override
    public boolean isHandled(T recipe) {
        return true;
    }
}
