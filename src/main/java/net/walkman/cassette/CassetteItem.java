package net.walkman.cassette;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CassetteItem extends Item {

    public CassetteItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = getRecordableId(stack);
        if (id != null) {
            RecordableData data = CassetteRegistry.get(id);
            if (data != null) {
                return Component.translatable(this.getDescriptionId(stack))
                        .append(" (")
                        .append(Component.literal(data.displayName()))
                        .append(")");
            } else if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
                if (tag.contains("DisplayName")) {
                    String name = tag.getString("DisplayName");

                    if (name.startsWith("Music Disc - ")) {
                        name = name.substring("Music Disc - ".length());
                    }
                    return Component.translatable(this.getDescriptionId(stack))
                            .append(" (")
                            .append(Component.literal(name))
                            .append(")");
                }
            }
        }
        return super.getName(stack);
    }

    public static void setDisplayName(ItemStack stack, String name) {
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY,
            customData -> customData.update(tag -> tag.putString("DisplayName", name)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = getRecordableId(stack);
        if (id != null) {
            RecordableData data = CassetteRegistry.get(id);
            if (data == null) {
                tooltip.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    public static ResourceLocation getRecordableId(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("RecordableId")) {
                return ResourceLocation.tryParse(tag.getString("RecordableId"));
            }
        }
        return null;
    }

    public static void setRecordableId(ItemStack stack, ResourceLocation id) {
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY,
            customData -> customData.update(tag -> tag.putString("RecordableId", id.toString())));
    }
}
