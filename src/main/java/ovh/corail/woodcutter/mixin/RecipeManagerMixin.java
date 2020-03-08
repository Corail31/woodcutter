package ovh.corail.woodcutter.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                } else if (getIngredients(entry.getValue()).stream().anyMatch(element -> {
                    String ingredient = getIngredientString(element);
                    return ingredient.contains(":") && !mods.computeIfAbsent(ingredient.split(":")[0], FabricLoader.getInstance()::isModLoaded);
                })) {
                    it.remove();
                    LOGGER.debug(String.format("%s: disabling recipe %s", MOD_ID, entry.getKey().toString()));
                }
            }
        }
        String toDisplay = mods.entrySet().stream().filter(entry -> !entry.getValue()).map(Map.Entry::getKey).collect(Collectors.joining(", "));
        if (!toDisplay.isEmpty()) {
            LOGGER.info(String.format("%s: some recipes have been disabled for the following domains : %s", MOD_ID, toDisplay));
        }
    }

    private String getIngredientString(JsonObject object) {
        String ingredient;
        return (ingredient = getString(object, "tag")).isEmpty() ? getString(object, "item") : ingredient;
    }

    private String getString(JsonObject object, String element) {
        JsonElement jsonElement;
        return object.has(element) && (jsonElement = object.get(element)).isJsonPrimitive() ? jsonElement.getAsString() : "";
    }

    private List<JsonObject> getIngredients(JsonObject object) {
        if (object.has("ingredient")) {
            JsonElement jsonElement = object.get("ingredient");
            if (jsonElement.isJsonObject()) {
                return Collections.singletonList((JsonObject) jsonElement);
            } else if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                return IntStream.range(0, jsonArray.size()).mapToObj(jsonArray::get).filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
