package ovh.corail.woodcutter.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.recipe.WoodcuttingRecipe;
import ovh.corail.woodcutter.registry.ModBlocks;
import ovh.corail.woodcutter.registry.ModContainers;
import ovh.corail.woodcutter.registry.ModRecipeTypes;

import java.util.ArrayList;
import java.util.List;

public class WoodcutterContainer extends Container {
    private final IWorldPosCallable iWorldPosCallable;
    private final IntReferenceHolder selectedRecipe = IntReferenceHolder.single();
    private final World world;
    private List<WoodcuttingRecipe> recipes = new ArrayList<>();
    private ItemStack itemStackInput = ItemStack.EMPTY;
    private long lastTakeTime;
    private final Slot inputSlot;
    private final Slot outputSlot;
    private Runnable inventoryUpdateListener = () -> {};
    private final IInventory inputInventory = new Inventory(1) {
        @Override
        public void markDirty() {
            super.markDirty();
            onCraftMatrixChanged(this);
            inventoryUpdateListener.run();
        }
    };
    private final CraftResultInventory resultInventory = new CraftResultInventory();

    public WoodcutterContainer(int id, PlayerInventory playerInventory) {
        this(id, playerInventory, IWorldPosCallable.DUMMY);
    }

    public WoodcutterContainer(int id, PlayerInventory playerInventory, final IWorldPosCallable iWorldPosCallable) {
        super(ModContainers.WOODCUTTER, id);
        this.iWorldPosCallable = iWorldPosCallable;
        this.world = playerInventory.player.world;
        this.inputSlot = addSlot(new Slot(this.inputInventory, 0, 20, 33));
        this.outputSlot = addSlot(new Slot(this.resultInventory, 1, 143, 33) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
                stack.onCrafting(thePlayer.world, thePlayer, stack.getCount()); // stat 'item crafted' & item.onCreated()
                resultInventory.onCrafting(thePlayer);
                ItemStack itemstack = inputSlot.decrStackSize(1);
                if (!itemstack.isEmpty()) {
                    updateRecipeResultSlot();
                }
                iWorldPosCallable.consume((world, pos) -> {
                    long l = world.getGameTime();
                    if (lastTakeTime != l) {
                        world.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundCategory.BLOCKS, 1f, 1f);
                        lastTakeTime = l;
                    }
                });
                return super.onTake(thePlayer, stack);
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
        trackInt(this.selectedRecipe);
    }

    @OnlyIn(Dist.CLIENT)
    public int getSelectedRecipe() {
        return this.selectedRecipe.get();
    }

    @OnlyIn(Dist.CLIENT)
    public List<WoodcuttingRecipe> getRecipeList() {
        return this.recipes;
    }

    @OnlyIn(Dist.CLIENT)
    public int getRecipeListSize() {
        return this.recipes.size();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean hasItemsinInputSlot() {
        return this.inputSlot.getHasStack() && !this.recipes.isEmpty();
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return this.iWorldPosCallable.applyOrElse((world, pos) -> ModBlocks.WOODCUTTERS.contains(world.getBlockState(pos).getBlock()) && playerIn.getDistanceSq((double) pos.getX() + 0.5d, (double) pos.getY() + 0.5d, (double) pos.getZ() + 0.5d) <= 64d, true);
    }

    @Override
    public boolean enchantItem(PlayerEntity playerIn, int id) {
        if (isValidRecipeId(id)) {
            this.selectedRecipe.set(id);
            updateRecipeResultSlot();
        }
        return true;
    }

    private boolean isValidRecipeId(int id) {
        return id >= 0 && id < this.recipes.size();
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        ItemStack stack = this.inputSlot.getStack();
        if (stack.getItem() != this.itemStackInput.getItem()) {
            this.itemStackInput = stack.copy();
            updateAvailableRecipes(inventoryIn, stack);
        }
    }

    private void updateAvailableRecipes(IInventory inventoryIn, ItemStack stack) {
        this.recipes.clear();
        this.selectedRecipe.set(-1);
        this.outputSlot.putStack(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            this.recipes = Helper.getSortedMatchingRecipes(world, inventoryIn);
        }
    }

    private void updateRecipeResultSlot() {
        if (!this.recipes.isEmpty() && isValidRecipeId(this.selectedRecipe.get())) {
            WoodcuttingRecipe recipe = this.recipes.get(this.selectedRecipe.get());
            this.resultInventory.setRecipeUsed(recipe);
            this.outputSlot.putStack(recipe.getCraftingResult(this.inputInventory));
        } else {
            this.outputSlot.putStack(ItemStack.EMPTY);
        }
        detectAndSendChanges();
    }

    @Override
    public ContainerType<?> getType() {
        return ModContainers.WOODCUTTER;
    }

    @OnlyIn(Dist.CLIENT)
    public void setInventoryUpdateListener(Runnable runnable) {
        this.inventoryUpdateListener = runnable;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        return slotIn.inventory != this.resultInventory && super.canMergeSlot(stack, slotIn);
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            Item item = itemstack1.getItem();
            stack = itemstack1.copy();
            if (index == 1) {
                item.onCreated(itemstack1, playerIn.world, playerIn);
                if (!this.mergeItemStack(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, stack);
            } else if (index == 0) {
                if (!this.mergeItemStack(itemstack1, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.world.getRecipeManager().getRecipe(ModRecipeTypes.WOODCUTTING, new Inventory(itemstack1), this.world).isPresent()) {
                if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 29) {
                if (!this.mergeItemStack(itemstack1, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 38 && !this.mergeItemStack(itemstack1, 2, 29, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            }
            slot.onSlotChanged();
            if (itemstack1.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(playerIn, itemstack1);
            detectAndSendChanges();
        }

        return stack;
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        PlayerInventory playerinventory = playerIn.inventory;
        if (!playerinventory.getItemStack().isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(playerIn, playerinventory.getItemStack());
            playerinventory.setItemStack(ItemStack.EMPTY);
        }
        this.resultInventory.removeStackFromSlot(1);
        if (this.iWorldPosCallable == IWorldPosCallable.DUMMY) {
            ItemStack leftStack = this.inputInventory.removeStackFromSlot(0);
            if (!leftStack.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(playerIn, leftStack);
            }
        } else {
            this.iWorldPosCallable.consume((world, pos) -> clearContainer(playerIn, world, this.inputInventory));
        }
        playerIn.container.detectAndSendChanges();
    }
}
