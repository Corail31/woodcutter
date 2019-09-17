package ovh.corail.woodcutter.compatibility;

import net.minecraft.util.IStringSerializable;
import net.minecraftforge.fml.ModList;

public enum SupportMods implements IStringSerializable {
    BIOMESOPLENTY("biomesoplenty"),
    MIDNIGHT("midnight");

    private final String modid;
    private final boolean loaded;

    SupportMods(String modid) {
        this.modid = modid;
        this.loaded = ModList.get() != null && ModList.get().getModContainerById(modid).isPresent();
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public String getName() {
        return modid;
    }
}
