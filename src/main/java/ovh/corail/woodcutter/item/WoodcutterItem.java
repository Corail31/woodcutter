package ovh.corail.woodcutter.item;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkHooks;
import ovh.corail.woodcutter.config.ConfigWoodcutter;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModTabs;

import javax.annotation.Nullable;
import java.util.List;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WoodcutterItem extends BlockItem {
    private static final Style desc1 = Style.EMPTY.applyFormatting(TextFormatting.GRAY);
    private static final Style desc2 = Style.EMPTY.applyFormatting(TextFormatting.DARK_GRAY).setItalic(true);

    public WoodcutterItem(Block woodcutterBlock) {
        super(woodcutterBlock, new Item.Properties().group(ModTabs.mainTab).maxStackSize(1));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flagIn) {
        list.add(new TranslationTextComponent("item.corail_woodcutter.woodcutter.desc").mergeStyle(desc1));
        if (ConfigWoodcutter.general.openWoodcutterInInventory.get()) {
            list.add(new TranslationTextComponent("item.corail_woodcutter.woodcutter.inventory").mergeStyle(desc2));
        }
    }

    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (ConfigWoodcutter.general.openWoodcutterInInventory.get() && hand == Hand.MAIN_HAND && player.isSneaking()) {
            ItemStack heldItem = player.getHeldItem(hand);
            if (!world.isRemote) {
                NetworkHooks.openGui((ServerPlayerEntity) player, new SimpleNamedContainerProvider((id, playerInventory, p) -> new WoodcutterContainer(id, playerInventory), WoodcutterBlock.TRANSLATION));
            }
            return ActionResult.resultSuccess(heldItem);
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        if (context.getPlayer() != null && !(context.getPlayer() instanceof FakePlayer) && context.getHand() == Hand.MAIN_HAND && context.getPlayer().isSneaking() && onItemRightClick(context.getWorld(), context.getPlayer(), context.getHand()).getType().isSuccess()) {
            return ActionResultType.SUCCESS;
        }
        return super.onItemUseFirst(stack, context);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (ConfigWoodcutter.general.openWoodcutterInInventory.get() && event.getPlayer() != null && !(event.getPlayer() instanceof FakePlayer) && event.getPlayer().isSneaking()) {
            ItemStack heldStack = event.getPlayer().getHeldItem(event.getHand());
            /* prevents to interact with entities based on the held item */
            if (ModBlocks.WOODCUTTER_ITEMS.contains(heldStack.getItem())) {
                event.setCancellationResult(ActionResultType.PASS);
                event.setCanceled(true);
            }
        }
    }
}
