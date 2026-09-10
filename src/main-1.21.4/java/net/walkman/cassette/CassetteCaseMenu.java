package net.walkman.cassette;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.walkman.registry.ModMenus;
import net.walkman.music.Music;

public class CassetteCaseMenu extends AbstractContainerMenu {

    private final ItemStack cassetteCaseStack;
    private final ItemStackHandler itemHandler;
    private final Inventory playerInventory;

    public CassetteCaseMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.getItemInHand(buf.readBoolean() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND));
    }

    public CassetteCaseMenu(int containerId, Inventory playerInventory, ItemStack cassetteCaseStackParam) {
        super(ModMenus.CASSETTE_CASE_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.cassetteCaseStack = cassetteCaseStackParam;
        
        this.itemHandler = new ItemStackHandler(28) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == 27) {
                    return stack.is(Music.BLANK_CASSETTE.get());
                }
                return stack.is(Music.CASSETTE.get());
            }

            @Override
            protected void onContentsChanged(int slot) {
                saveToStack();
            }
        };

        loadFromStack();

        // Case slots (9x3 grid)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new SlotItemHandler(itemHandler, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }

        // Blank cassette slot (at slot 27)
        this.addSlot(new SlotItemHandler(itemHandler, 27, 180, 18));

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Player Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    private void loadFromStack() {
        if (cassetteCaseStack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = cassetteCaseStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("Inventory")) {
                itemHandler.deserializeNBT(playerInventory.player.level().registryAccess(), tag.getCompound("Inventory"));
            }
        }
    }

    private void saveToStack() {
        cassetteCaseStack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY, 
            customData -> customData.update(tag -> {
                tag.put("Inventory", itemHandler.serializeNBT(playerInventory.player.level().registryAccess()));
            }));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 28) { // From Case to Player
                if (!this.moveItemStackTo(itemstack1, 28, 64, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Player to Case
                // Try to move to appropriate slot based on item type
                if (itemstack1.is(Music.BLANK_CASSETTE.get())) {
                    if (!this.moveItemStackTo(itemstack1, 27, 28, false)) { // Try blank slot first
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.is(Music.CASSETTE.get())) {
                    if (!this.moveItemStackTo(itemstack1, 0, 27, false)) { // Try cassette slots
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.cassetteCaseStack.isEmpty() && (player.getMainHandItem() == this.cassetteCaseStack || player.getOffhandItem() == this.cassetteCaseStack);
    }
}
