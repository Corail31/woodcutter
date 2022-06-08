package ovh.corail.woodcutter.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.config.ConfigWoodcutter;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModTabs;

import javax.annotation.Nullable;
import java.util.List;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WoodcutterItem extends BlockItem {
    private static final Component DEFAULT_DESCRIPTION = Component.translatable("item.corail_woodcutter.woodcutter.desc").withStyle(ChatFormatting.GRAY);
    private static final Component ACCESS_FROM_INVENTORY = Component.translatable("item.corail_woodcutter.woodcutter.inventory").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);

    public WoodcutterItem(Block woodcutterBlock) {
        super(woodcutterBlock, new Item.Properties().tab(ModTabs.mainTab).stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flagIn) {
        list.add(DEFAULT_DESCRIPTION);
        if (ConfigWoodcutter.general.openWoodcutterInInventory.get()) {
            list.add(ACCESS_FROM_INVENTORY);
        }
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (ConfigWoodcutter.general.openWoodcutterInInventory.get() && hand == InteractionHand.MAIN_HAND && player.isDiscrete()) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (!world.isClientSide()) {
                NetworkHooks.openGui((ServerPlayer) player, new SimpleMenuProvider((id, playerInventory, p) -> new WoodcutterContainer(id, playerInventory), WoodcutterBlock.TRANSLATION));
            }
            return InteractionResultHolder.success(heldItem);
        }
        return super.use(world, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && !(context.getPlayer() instanceof FakePlayer) && context.getHand() == InteractionHand.MAIN_HAND && context.getPlayer().isDiscrete()) {
            return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
        }
        return super.useOn(context);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (ConfigWoodcutter.general.openWoodcutterInInventory.get() && event.getPlayer() != null && !(event.getPlayer() instanceof FakePlayer) && event.getPlayer().isDiscrete()) {
            ItemStack heldStack = event.getPlayer().getItemInHand(event.getHand());
            /* prevents to interact with entities based on the held item */
            if (ModBlocks.WOODCUTTER_ITEMS.contains(heldStack.getItem())) {
                event.setCancellationResult(InteractionResult.PASS);
                event.setCanceled(true);
            }
        }
    }
}
