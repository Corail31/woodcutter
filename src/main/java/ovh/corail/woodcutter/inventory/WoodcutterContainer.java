package ovh.corail.woodcutter.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.container.BlockContext;
import net.minecraft.container.Container;
import net.minecraft.container.Property;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.World;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModRecipeTypes;

import java.util.ArrayList;
import java.util.List;

public class WoodcutterContainer extends Container {
    private final BlockContext blockContext;
    private final Property selectedRecipe = Property.create();
    private final World world;
    private List<WoodcuttingRecipe> recipes = new ArrayList<>();
    private ItemStack result = ItemStack.EMPTY;
    private long lastTakeTime;
    private final Slot inputSlot;
    private final Slot outputSlot;
    private Runnable inventoryUpdateListener = () -> {
    };
    private final Inventory inventory = new BasicInventory(1) {
        @Override
        public void markDirty() {
            super.markDirty();
            onContentChanged(this);
            inventoryUpdateListener.run();
        }
    };
    private final CraftingResultInventory resultInventory = new CraftingResultInventory();
    public PlayerInventory playerInventory;

    public WoodcutterContainer(int id, PlayerInventory playerInventory) {
        this(id, playerInventory, BlockContext.EMPTY);
    }

    public WoodcutterContainer(int syncId, Identifier id, PlayerEntity player, PacketByteBuf buf) {
        this(syncId, player.inventory, BlockContext.create(player.world, buf.readBlockPos()));
    }

    public WoodcutterContainer(int id, PlayerInventory playerInventory, final BlockContext blockContext) {
        super(null, id);
        this.playerInventory = playerInventory;
        this.blockContext = blockContext;
        this.world = playerInventory.player.world;
        this.inputSlot = addSlot(new Slot(this.inventory, 0, 20, 33));
        this.outputSlot = addSlot(new Slot(this.resultInventory, 1, 143, 33) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTakeItem(PlayerEntity thePlayer, ItemStack stack) {
                ItemStack itemstack = inputSlot.takeStack(1);
                if (!itemstack.isEmpty()) {
                    populateResult();
                }
                stack.getItem().onCraft(stack, thePlayer.world, thePlayer);
                blockContext.run((world, pos) -> {
                    long l = world.getTime();
                    if (lastTakeTime != l) {
                        world.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundCategory.BLOCKS, 1f, 1f);
                        lastTakeTime = l;
                    }
                });
                return super.onTakeItem(thePlayer, stack);
            }
        });
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
        addProperty(this.selectedRecipe);
    }

    @Environment(EnvType.CLIENT)
    public int getSelectedRecipe() {
        return this.selectedRecipe.get();
    }

    @Environment(EnvType.CLIENT)
    public List<WoodcuttingRecipe> getRecipeList() {
        return this.recipes;
    }

    @Environment(EnvType.CLIENT)
    public int getRecipeListSize() {
        return this.recipes.size();
    }

    @Environment(EnvType.CLIENT)
    public boolean canCraft() {
        return this.inputSlot.hasStack() && !this.recipes.isEmpty();
    }

    @Override
    public boolean canUse(PlayerEntity playerIn) {
        return this.blockContext.run((world, pos) -> ModBlocks.WOODCUTTERS.contains(world.getBlockState(pos).getBlock()) && playerIn.squaredDistanceTo((double) pos.getX() + 0.5d, (double) pos.getY() + 0.5d, (double) pos.getZ() + 0.5d) <= 64d, true);
    }

    @Override
    public boolean onButtonClick(PlayerEntity playerIn, int id) {
        if (id >= 0 && id < this.recipes.size()) {
            this.selectedRecipe.set(id);
            populateResult();
        }
        return true;
    }

    @Override
    public void onContentChanged(Inventory inventoryIn) {
        ItemStack stack = this.inputSlot.getStack();
        if (stack.getItem() != this.result.getItem()) {
            this.result = stack.copy();
            this.updateInput(inventoryIn, stack);
        }
    }

    private void updateInput(Inventory inventoryIn, ItemStack stack) {
        this.recipes.clear();
        this.selectedRecipe.set(-1);
        this.outputSlot.setStack(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            this.recipes = this.world.getRecipeManager().getAllMatches(ModRecipeTypes.WOODCUTTING, inventoryIn, this.world);
        }
    }

    private void populateResult() {
        if (!this.recipes.isEmpty()) {
            WoodcuttingRecipe recipe = this.recipes.get(this.selectedRecipe.get());
            this.outputSlot.setStack(recipe.craft(this.inventory));
        } else {
            this.outputSlot.setStack(ItemStack.EMPTY);
        }
        sendContentUpdates();
    }

    @Environment(EnvType.CLIENT)
    public void setInventoryUpdateListener(Runnable runnable) {
        this.inventoryUpdateListener = runnable;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slotIn) {
        return slotIn.inventory != this.inventory && super.canInsertIntoSlot(stack, slotIn);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity playerIn, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack itemstack1 = slot.getStack();
            Item item = itemstack1.getItem();
            stack = itemstack1.copy();
            if (index == 1) {
                item.onCraft(itemstack1, playerIn.world, playerIn);
                if (!this.insertItem(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onStackChanged(itemstack1, stack);
            } else if (index == 0) {
                if (!this.insertItem(itemstack1, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.world.getRecipeManager().getFirstMatch(ModRecipeTypes.WOODCUTTING, new BasicInventory(itemstack1), this.world).isPresent()) {
                if (!this.insertItem(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 29) {
                if (!this.insertItem(itemstack1, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 38 && !this.insertItem(itemstack1, 2, 29, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            }
            slot.markDirty();
            if (itemstack1.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(playerIn, itemstack1);
            sendContentUpdates();
        }

        return stack;
    }

    @Override
    public void close(PlayerEntity playerIn) {
        super.close(playerIn);
        this.resultInventory.removeInvStack(1);
        this.blockContext.run((world, pos) -> {
            dropInventory(playerIn, playerIn.world, this.inventory);
        });
    }
}
