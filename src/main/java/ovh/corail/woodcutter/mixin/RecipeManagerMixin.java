package ovh.corail.woodcutter.mixin;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void apply(Map<Identifier, JsonObject> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        Iterator<Map.Entry<Identifier, JsonObject>> it = map.entrySet().iterator();
        String customRecipeType = MOD_ID + ":woodcutting";
        Map<String, Boolean> mods = new HashMap<>();
        while (it.hasNext()) {
            Map.Entry<Identifier, JsonObject> entry = it.next();
            if (MOD_ID.equals(entry.getKey().getNamespace()) && customRecipeType.equals(JsonHelper.getString(entry.getValue(), "type"))) {
                String result = JsonHelper.getString(entry.getValue(), "result", "");
                if (result.contains(":") && !mods.computeIfAbsent(result.split(":")[0], FabricLoader.getInstance()::isModLoaded)) {
                    it.remove();
                }
                JsonObject obj = JsonHelper.hasArray(entry.getValue(), "ingredient") ? null : JsonHelper.getObject(entry.getValue(), "ingredient", null);
                if (obj != null && obj.isJsonObject()) {
                    String ingredient = "";
                    if (obj.has("tag")) {
                        ingredient = JsonHelper.getString(obj, "tag", "");
                    } else if (obj.has("item")) {
                        ingredient = JsonHelper.getString(obj, "item", "");
                    }
                    if (ingredient.contains(":") && !mods.computeIfAbsent(ingredient.split(":")[0], FabricLoader.getInstance()::isModLoaded)) {
                        it.remove();
                    }
                }
            }
        }
    }
}
