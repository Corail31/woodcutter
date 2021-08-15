package ovh.corail.woodcutter.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModStats {
    public static final ResourceLocation INTERACT_WITH_SAWMILL = register("interact_with_sawmill");

    private static ResourceLocation register(String name) {
        return Stats.makeCustomStat(MOD_ID + ":" + name, StatFormatter.DEFAULT);
    }
}
