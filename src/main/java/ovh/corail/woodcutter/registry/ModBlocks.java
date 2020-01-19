package ovh.corail.woodcutter.registry;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ovh.corail.woodcutter.block.WoodcutterBlock;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModBlocks {
    enum WoodcutterVariant { OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK }

    public static final Set<Block> WOODCUTTERS = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static ItemStack RANDOM_STACK = ItemStack.EMPTY;

    public static void init() {
        for (WoodcutterVariant variant : WoodcutterVariant.values()) {
            registerWoodcutter(variant.name().toLowerCase());
        }
    }

    public static void registerWoodcutter(String name) {
        Block woodcutter = new WoodcutterBlock();
        Identifier id = new Identifier(MOD_ID, name + "_woodcutter");
        Registry.register(Registry.BLOCK, id, woodcutter);
        Registry.register(Registry.ITEM, id, new BlockItem(woodcutter, new Item.Settings().group(ModTabs.mainTab)));
        WOODCUTTERS.add(woodcutter);
    }

    public static ItemStack createRandomStack() {
        if (RANDOM_STACK.isEmpty()) {
            RANDOM_STACK = new ItemStack(ModBlocks.WOODCUTTERS.stream().skip(RANDOM.nextInt(ModBlocks.WOODCUTTERS.size())).findFirst().orElse(Blocks.STONECUTTER));
        }
        return RANDOM_STACK;
    }
}
