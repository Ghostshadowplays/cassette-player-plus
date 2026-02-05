package net.walkman.cassette;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CassetteCaseItem extends Item {

    public CassetteCaseItem(Properties props) {
        super(props);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        if (action == net.minecraft.world.inventory.ClickAction.SECONDARY && !other.isEmpty()) {
            boolean isCassette = other.is(net.walkman.music.Music.CASSETTE.get());
            boolean isBlank = other.is(net.walkman.music.Music.BLANK_CASSETTE.get());
            
            if (isCassette || isBlank) {
                net.neoforged.neoforge.items.ItemStackHandler handler = new net.neoforged.neoforge.items.ItemStackHandler(28);
                if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                    net.minecraft.nbt.CompoundTag tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
                    if (tag.contains("Inventory")) {
                        handler.deserializeNBT(player.level().registryAccess(), tag.getCompound("Inventory"));
                    }
                }

                ItemStack remainder = other;
                if (isBlank) {
                    remainder = handler.insertItem(27, other, false);
                } else {
                    for (int i = 0; i < 27; i++) {
                        remainder = handler.insertItem(i, remainder, false);
                        if (remainder.isEmpty()) break;
                    }
                }

                if (remainder.getCount() != other.getCount()) {
                    access.set(remainder);
                    stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY,
                        customData -> customData.update(tag -> {
                            tag.put("Inventory", handler.serializeNBT(player.level().registryAccess()));
                        }));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CassetteCaseMenu(containerId, playerInventory, stack),
                stack.getHoverName()
            ), buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

}
