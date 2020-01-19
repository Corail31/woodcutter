package ovh.corail.woodcutter.recipe;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeSerializers;
import ovh.corail.woodcutter.registry.ModRecipeTypes;

public class WoodcuttingRecipe extends CuttingRecipe {

    public WoodcuttingRecipe(Identifier id, String group, Ingredient ingredient, ItemStack result) {
        super(ModRecipeTypes.WOODCUTTING, ModRecipeSerializers.WOODCUTTING, id, group, ingredient, result);
    }

    @Override
    public boolean matches(Inventory inv, World world) {
        return this.input.test(inv.getInvStack(0));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public ItemStack getRecipeKindIcon() {
        return ModBlocks.createRandomStack();
    }
}
