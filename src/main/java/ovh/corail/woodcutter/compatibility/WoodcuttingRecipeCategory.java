package ovh.corail.woodcutter.compatibility;

/*import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;*/

public class WoodcuttingRecipeCategory { // TODO disabled // implements IRecipeCategory<WoodcuttingRecipe> {
    /*private static final ResourceLocation RECIPE_GUI_VANILLA = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
    private static final ResourceLocation UID = new ResourceLocation(MOD_ID, "woodcutting");
    private static final int WIDTH = 116, HEIGHT = 18;
    private final IDrawable background, icon;
    private final String localizedName;

    WoodcuttingRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(RECIPE_GUI_VANILLA, 49, 168, WIDTH, HEIGHT).addPadding(0, 0, 40, 0).build();
        this.icon = guiHelper.createDrawableIngredient(ModBlocks.createRandomStack());
        this.localizedName = I18n.format("container.corail_woodcutter.woodcutter");
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<WoodcuttingRecipe> getRecipeClass() {
        return WoodcuttingRecipe.class;
    }

    @Override
    public String getTitle() {
        return this.localizedName;
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
    public void setIngredients(WoodcuttingRecipe recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(recipe.getIngredients());
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getRecipeOutput());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WoodcuttingRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
        guiItemStacks.init(0, true, 40, 0);
        guiItemStacks.init(1, false, 98, 0);
        guiItemStacks.set(0, ingredients.getInputs(VanillaTypes.ITEM).get(0));
        guiItemStacks.set(1, ingredients.getOutputs(VanillaTypes.ITEM).get(0));
    }*/
}
