package ovh.corail.woodcutter.registry;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;
import static ovh.corail.woodcutter.WoodCutterMod.MOD_NAME;

public class ModTabs {
    public static final ItemGroup mainTab = new ItemGroup(MOD_ID) {
        @Override
        @OnlyIn(Dist.CLIENT)
        public ItemStack createIcon() {
            return ModBlocks.createRandomStack();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public ITextComponent getGroupName() {
            return new StringTextComponent(MOD_NAME);
        }
    };
}
