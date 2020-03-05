package ovh.corail.woodcutter.mixin;

import com.google.gson.JsonElement;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static ovh.corail.woodcutter.WoodCutterMod.LOGGER;
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
            if (customRecipeType.equals(getString(entry.getValue(), "type"))) {
                String result = getString(entry.getValue(), "result");
                if (result.contains(":") && !mods.computeIfAbsent(result.split(":")[0], FabricLoader.getInstance()::isModLoaded)) {
                    it.remove();
                    LOGGER.debug(String.format("%s: disabling recipe %s", MOD_ID, entry.getKey().toString()));
                } else {
                    JsonObject obj = getObject(entry.getValue(), "ingredient");
                    if (obj != null) {
                        String ingredient = "";
                        if (obj.has("tag")) {
                            ingredient = getString(obj, "tag");
                        } else if (obj.has("item")) {
                            ingredient = getString(obj, "item");
                        }
                        if (ingredient.contains(":") && !mods.computeIfAbsent(ingredient.split(":")[0], FabricLoader.getInstance()::isModLoaded)) {
                            it.remove();
                            LOGGER.debug(String.format("%s: disabling recipe %s", MOD_ID, entry.getKey().toString()));
                        }
                    }
                }
            }
        }
        String toDisplay = mods.entrySet().stream().filter(entry -> !entry.getValue()).map(Map.Entry::getKey).collect(Collectors.joining(", "));
        if (!toDisplay.isEmpty()) {
            LOGGER.info(String.format("%s: some recipes have been disabled for the following domains : %s", MOD_ID, toDisplay));
        }
    }

    private String getString(JsonObject object, String element) {
        JsonElement jsonElement;
        return object.has(element) && (jsonElement = object.get(element)).isJsonPrimitive() ? jsonElement.getAsString() : "";
    }

    @Nullable
    private JsonObject getObject(JsonObject object, String element) {
        JsonElement jsonElement;
        return !JsonHelper.hasArray(object, element) && object.has(element) && (jsonElement = object.get(element)).isJsonObject() ? jsonElement.getAsJsonObject() : null;
    }
}
