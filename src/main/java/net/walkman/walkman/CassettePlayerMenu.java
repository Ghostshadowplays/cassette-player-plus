package net.walkman.walkman;

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

public class CassettePlayerMenu extends AbstractContainerMenu {

    private final ItemStack cassettePlayerStack;
    private final ItemStackHandler itemHandler;
    private final Inventory playerInventory;
    private float volume = 1.0f;

    public CassettePlayerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.getMainHandItem());
    }

    public CassettePlayerMenu(int containerId, Inventory playerInventory, ItemStack cassettePlayerStackParam) {
        super(ModMenus.CASSETTE_PLAYER_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.cassettePlayerStack = cassettePlayerStackParam;
        
        this.itemHandler = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(Music.CASSETTE.get());
            }

            @Override
            protected void onContentsChanged(int slot) {
                saveToStack();
            }
        };

        loadFromStack();

        // Walkman slot
        this.addSlot(new SlotItemHandler(itemHandler, 0, 80, 35));

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

    public float getVolume() {
        return volume;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 100) {
            this.volume = id / 100f;
            saveToStack();
            return true;
        }
        return false;
    }

    private void loadFromStack() {
        if (cassettePlayerStack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = cassettePlayerStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("Cassette")) {
                itemHandler.deserializeNBT(playerInventory.player.level().registryAccess(), tag.getCompound("Cassette"));
            }
            if (tag.contains("Volume")) {
                this.volume = tag.getFloat("Volume");
            }
        }
    }

    private void saveToStack() {
        cassettePlayerStack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY, 
            customData -> customData.update(tag -> {
                tag.put("Cassette", itemHandler.serializeNBT(playerInventory.player.level().registryAccess()));
                tag.putFloat("Volume", this.volume);
            }));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 1) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                return ItemStack.EMPTY;
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
        return true;
    }
}
