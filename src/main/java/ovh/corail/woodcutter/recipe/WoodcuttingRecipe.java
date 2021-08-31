package ovh.corail.woodcutter.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.Level;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeSerializers;
import ovh.corail.woodcutter.registry.ModRecipeTypes;

public class WoodcuttingRecipe extends SingleItemRecipe {

    public WoodcuttingRecipe(ResourceLocation id, String group, Ingredient ingredient, ItemStack result) {
        super(ModRecipeTypes.WOODCUTTING, ModRecipeSerializers.WOODCUTTING, id, group, ingredient, result);
    }

    @Override
    public boolean matches(Container inv, Level worldIn) {
        return this.ingredient.test(inv.getItem(0));
    }

    @Override
    public ItemStack getToastSymbol() {
        return ModBlocks.createRandomStack();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }
}
