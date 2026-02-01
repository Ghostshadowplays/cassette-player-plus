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
    private boolean repeat = false;

    public CassettePlayerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.getItemInHand(buf.readBoolean() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND));
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
        this.addSlot(new SlotItemHandler(itemHandler, 0, 80, 21));

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 51 + i * 18));
            }
        }

        // Player Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 109));
        }
    }

    public float getVolume() {
        return volume;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 100) {
            this.volume = id / 100f;
            saveToStack();
            return true;
        } else if (id == 101) {
            this.repeat = !this.repeat;
            saveToStack();
            // Inform client handler if on client side
            if (player.level().isClientSide) {
                try {
                    Class<?> cls = Class.forName("net.walkman.walkman.CassettePlayerSoundHandler");
                    java.lang.reflect.Method m = cls.getMethod("setRepeat", java.util.UUID.class, boolean.class);
                    m.invoke(null, player.getUUID(), this.repeat);
                } catch (Throwable ignored) {}
            }
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
            if (tag.contains("Repeat")) {
                this.repeat = tag.getBoolean("Repeat");
                if (playerInventory.player.level().isClientSide) {
                    try {
                        Class<?> cls = Class.forName("net.walkman.walkman.CassettePlayerSoundHandler");
                        java.lang.reflect.Method m = cls.getMethod("setRepeat", java.util.UUID.class, boolean.class);
                        m.invoke(null, playerInventory.player.getUUID(), this.repeat);
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    private void saveToStack() {
        cassettePlayerStack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY, 
            customData -> customData.update(tag -> {
                tag.put("Cassette", itemHandler.serializeNBT(playerInventory.player.level().registryAccess()));
                tag.putFloat("Volume", this.volume);
                tag.putBoolean("Repeat", this.repeat);
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
        return !this.cassettePlayerStack.isEmpty() && (player.getMainHandItem() == this.cassettePlayerStack || player.getOffhandItem() == this.cassettePlayerStack);
    }
}
