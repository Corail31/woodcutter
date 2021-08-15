package ovh.corail.woodcutter.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_NAME;

public class ModTabs {
    public static final CreativeModeTab mainTab = new CreativeModeTab(MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return ModBlocks.createRandomStack();
        }

        @Override
        public Component getDisplayName() {
            return new TextComponent(MOD_NAME);
        }
    };
}
