package ovh.corail.woodcutter.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.item.WoodcutterItem;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModBlocks {
    public static final Set<Block> WOODCUTTERS = new HashSet<>();
    public static final Set<Item> WOODCUTTER_ITEMS = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static ItemStack RANDOM_STACK = ItemStack.EMPTY;

    static void onRegisterBlocks(final RegisterEvent event) {
        registerWoodcutter(event, "", "acacia", "bamboo", "bamboo_mosaic", "birch", "cherry", "crimson", "dark_oak", "jungle", "spruce", "mangrove", "oak", "warped");
        if (SupportMods.BIOMESOPLENTY.isLoaded()) {
            registerWoodcutter(event, "bop", "dead", "fir", "hellbark", "jacaranda", "magic", "mahogany", "palm", "redwood", "umbran", "willow", "pine", "maple", "empyreal");
        }
    }

    static void onRegisterBlockItems(final RegisterEvent event) {
        WOODCUTTERS.forEach(woodcutterBlock -> {
            Item woodcutter = new WoodcutterItem(woodcutterBlock);
            WOODCUTTER_ITEMS.add(woodcutter);
            event.register(ForgeRegistries.Keys.ITEMS, Helper.getRegistryRL(woodcutterBlock), () -> woodcutter);
        });
    }

    private static void registerWoodcutter(final RegisterEvent event, String folder, String... names) {
        final Function<String, String> funcName = folder.isEmpty() ? name -> name + "_woodcutter" : name -> folder + "_" + name + "_woodcutter";
        for (String name : names) {
            Block woodcutter = new WoodcutterBlock();
            WOODCUTTERS.add(woodcutter);
            event.register(ForgeRegistries.Keys.BLOCKS, new ResourceLocation(MOD_ID, funcName.apply(name)), () -> woodcutter);
        }
    }

    public static ItemStack createRandomStack() {
        if (RANDOM_STACK.isEmpty()) {
            RANDOM_STACK = new ItemStack(WOODCUTTERS.stream().skip(RANDOM.nextInt(WOODCUTTERS.size())).findFirst().orElse(Blocks.STONECUTTER));
        }
        return RANDOM_STACK;
    }
}
