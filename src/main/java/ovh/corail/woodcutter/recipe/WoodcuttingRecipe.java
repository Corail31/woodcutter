package ovh.corail.woodcutter.recipe;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
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

    @SuppressWarnings("unchecked")
    public static class CustomSerializer<T extends CuttingRecipe> implements RecipeSerializer<T> {
        final RecipeFactory<T> recipeFactory;

        public CustomSerializer(RecipeFactory<T> recipeFactory) {
            this.recipeFactory = recipeFactory;
        }

        @Override
        public T read(Identifier id, JsonObject json) {
            return (T) STONECUTTING.read(id, json);
        }

        @Override
        public T read(Identifier id, PacketByteBuf buf) {
            return (T) STONECUTTING.read(id, buf);
        }

        @Override
        public void write(PacketByteBuf buf, T recipe) {
            STONECUTTING.write(buf, (StonecuttingRecipe) recipe);
        }

        public interface RecipeFactory<T extends CuttingRecipe> {
            T create(Identifier identifier, String string, Ingredient ingredient, ItemStack itemStack);
        }
    }
}
