package ovh.corail.woodcutter.recipe;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
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
    public static class CustomSerializer<T extends WoodcuttingRecipe> implements RecipeSerializer<T> {
        final RecipeFactory<T> recipeFactory;

        public CustomSerializer(RecipeFactory<T> recipeFactory) {
            this.recipeFactory = recipeFactory;
        }

        @Override
        public T read(Identifier id, JsonObject json) {
            String s = JsonHelper.getString(json, "group", "");
            Ingredient ingredient;
            if (JsonHelper.hasArray(json, "ingredient")) {
                ingredient = Ingredient.fromJson(JsonHelper.getArray(json, "ingredient"));
            } else {
                ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"));
            }

            String s1 = JsonHelper.getString(json, "result");
            int i = JsonHelper.getInt(json, "count");
            ItemStack itemstack = new ItemStack(Registry.ITEM.get(new Identifier(s1)), i);
            return this.recipeFactory.create(id, s, ingredient, itemstack);
        }

        @Override
        public T read(Identifier id, PacketByteBuf buf) {
            String s = buf.readString(32767);
            Ingredient ingredient = Ingredient.fromPacket(buf);
            ItemStack itemstack = buf.readItemStack();
            return this.recipeFactory.create(id, s, ingredient, itemstack);
        }

        @Override
        public void write(PacketByteBuf buf, T recipe) {
            buf.writeString(recipe.group);
            recipe.input.write(buf);
            buf.writeItemStack(recipe.output);
        }

        public interface RecipeFactory<T extends CuttingRecipe> {
            T create(Identifier identifier, String string, Ingredient ingredient, ItemStack itemStack);
        }
    }
}
