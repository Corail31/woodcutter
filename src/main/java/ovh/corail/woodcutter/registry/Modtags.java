package ovh.corail.woodcutter.registry;

import net.minecraft.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class Modtags {
    public static class Items {
        public static final Tag<Item> ALLOWED_ITEMS = tag("allowed_items");

        private static Tag<Item> tag(String name) {
            return new ItemTags.Wrapper(new ResourceLocation(MOD_ID, name));
        }
    }
}
