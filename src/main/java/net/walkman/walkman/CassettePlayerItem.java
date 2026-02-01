package net.walkman.walkman;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CassettePlayerItem extends Item {

    public CassettePlayerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CassettePlayerMenu(containerId, playerInventory, stack),
                stack.getHoverName()
            ), buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

}
