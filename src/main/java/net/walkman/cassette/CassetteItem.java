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
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("DisplayName")) {
                String name = tag.getString("DisplayName");
                String cleaned = cleanName(name);
                if (!cleaned.equals("Unknown Track")) {
                    return Component.translatable(this.getDescriptionId(stack))
                            .append(" (")
                            .append(Component.literal(cleaned))
                            .append(")");
                }
            }
        }

        ResourceLocation id = getRecordableId(stack);
        if (id != null) {
            RecordableData data = CassetteRegistry.get(id);
            if (data != null) {
                String cleaned = cleanName(data.displayName());
                if (!cleaned.equals("Unknown Track")) {
                    return Component.translatable(this.getDescriptionId(stack))
                            .append(" (")
                            .append(Component.literal(cleaned))
                            .append(")");
                }
            }
        }
        return super.getName(stack);
    }

    private static String cleanName(String name) {
        if (name == null || name.isEmpty()) return "Unknown Track";
        
        String cleaned = name;
        if (cleaned.startsWith("Music Disc - ")) {
            cleaned = cleaned.substring("Music Disc - ".length());
        } else if (cleaned.startsWith("Music Disc ")) {
            cleaned = cleaned.substring("Music Disc ".length());
        }
        
        if (cleaned.equalsIgnoreCase("Music Disc") || cleaned.equalsIgnoreCase("- Music Disc") || cleaned.isEmpty()) {
            return "Unknown Track";
        }
        return cleaned;
    }

    public static void setDisplayName(ItemStack stack, String name) {
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY,
            customData -> customData.update(tag -> tag.putString("DisplayName", cleanName(name))));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.getBoolean("IsMixTape")) {
                tooltip.add(Component.literal("Custom Mix Tape").withStyle(ChatFormatting.GOLD));
                if (tag.contains("Tracks")) {
                    net.minecraft.nbt.ListTag tracks = tag.getList("Tracks", net.minecraft.nbt.Tag.TAG_COMPOUND);
                    boolean isCompound = !tracks.isEmpty();
                    if (!isCompound) {
                        tracks = tag.getList("Tracks", net.minecraft.nbt.Tag.TAG_STRING);
                    }
                    
                    for (int i = 0; i < tracks.size(); i++) {
                        ResourceLocation trackId = null;
                        if (isCompound) {
                            trackId = ResourceLocation.tryParse(tracks.getCompound(i).getString("id"));
                        } else {
                            trackId = ResourceLocation.tryParse(tracks.getString(i));
                        }
                        
                        if (trackId != null) {
                            RecordableData data = CassetteRegistry.get(trackId);
                            String name;
                            if (data != null) {
                                name = data.displayName();
                            } else {
                                // Fallback to item name if possible
                                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(trackId);
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    name = item.getName(new ItemStack(item)).getString();
                                } else {
                                    name = trackId.getPath();
                                }
                            }
                            
                            name = cleanName(name);
                            if (name.equals("Unknown Track")) {
                                name = trackId.getPath();
                            }

                            tooltip.add(Component.literal("- " + name).withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
            }
        }
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
        assignRandomTexture(stack);
    }

    public static void assignRandomTexture(ItemStack stack) {
        if (!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) || 
            !stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().contains("TextureIndex")) {
            
            int index = new java.util.Random().nextInt(4); // 0 to 3
            stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                customData -> customData.update(tag -> tag.putInt("TextureIndex", index)));
        }
    }

    public static int getTextureIndex(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("TextureIndex")) {
                return tag.getInt("TextureIndex");
            }
        }
        return 0;
    }
}
