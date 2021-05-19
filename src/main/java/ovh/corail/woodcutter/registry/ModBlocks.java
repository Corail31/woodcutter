package ovh.corail.woodcutter.registry;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.item.WoodcutterItem;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {
    enum WoodcutterVariant {OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK, CRIMSON, WARPED}

    public enum WoodcutterBOPVariant implements IStringSerializable {
        CHERRY, DEAD, FIR, HELLBARK, JACARANDA, MAGIC, MAHOGANY, PALM, REDWOOD, UMBRAN, WILLOW;
        private final String name;
        WoodcutterBOPVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getString() {
            return this.name;
        }
    }

    public static final Set<Block> WOODCUTTERS = new HashSet<>();
    public static final Set<Item> WOODCUTTER_ITEMS = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static ItemStack RANDOM_STACK = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onRegisterBlocks(final RegistryEvent.Register<Block> event) {
        for (WoodcutterVariant variant : WoodcutterVariant.values()) {
            registerWoodcutter(event.getRegistry(), variant.name().toLowerCase(Locale.US));
        }
        if (SupportMods.BIOMESOPLENTY.isLoaded()) {
            for (WoodcutterBOPVariant variant : WoodcutterBOPVariant.values()) {
                ModBlocks.registerWoodcutter(event.getRegistry(), variant.getString());
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterBlockItems(final RegistryEvent.Register<Item> event) {
        WOODCUTTERS.forEach(woodcutterBlock -> {
            Item woodcutter = new WoodcutterItem(woodcutterBlock).setRegistryName(woodcutterBlock.getRegistryName());
            WOODCUTTER_ITEMS.add(woodcutter);
            event.getRegistry().register(woodcutter);
        });
    }

    public static void registerWoodcutter(IForgeRegistry<Block> registry, String name) {
        Block woodcutter = new WoodcutterBlock().setRegistryName(MOD_ID, name + "_woodcutter");
        registry.register(woodcutter);
        WOODCUTTERS.add(woodcutter);
    }

    public static ItemStack createRandomStack() {
        if (RANDOM_STACK.isEmpty()) {
            RANDOM_STACK = new ItemStack(ModBlocks.WOODCUTTERS.stream().skip(RANDOM.nextInt(ModBlocks.WOODCUTTERS.size())).findFirst().orElse(Blocks.STONECUTTER));
        }
        return RANDOM_STACK;
    }
}
