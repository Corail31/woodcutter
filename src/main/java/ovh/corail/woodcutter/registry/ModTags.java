package ovh.corail.woodcutter.registry;

import net.minecraft.item.Item;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModTags {
    public static class Items {
        public static final Tag<Item> ALLOWED_ITEMS = tag("allowed_items");

        private static Tag<Item> tag(String name) {
            return new ItemTags.CachingTag(new Identifier(MOD_ID, name));
        }
    }
}
