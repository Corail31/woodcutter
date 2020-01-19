package ovh.corail.woodcutter.registry;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModTabs {
    public static final ItemGroup mainTab = FabricItemGroupBuilder.create(new Identifier(MOD_ID, "main")).icon(ModBlocks::createRandomStack).build();
}
