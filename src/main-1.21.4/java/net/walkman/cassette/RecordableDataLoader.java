package net.walkman.cassette;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class RecordableDataLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {

    private static final FileToIdConverter LISTER = FileToIdConverter.json("recordables");

    public RecordableDataLoader() {
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        for (var entry : LISTER.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation fileId = entry.getKey();
            ResourceLocation id = LISTER.fileToId(fileId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                map.put(id, json);
            } catch (Exception e) {
                System.out.println("❌ Failed to parse JSON recordable from " + fileId + ": " + e.getMessage());
            }
        }
        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         ResourceManager manager,
                         ProfilerFiller profiler) {

        System.out.println("[DEBUG_LOG] RecordableDataLoader fired!");
        System.out.println("[DEBUG_LOG] Found recordables: " + jsonMap.keySet());

        CassetteRegistry.clear();
        // discoverDiscs will be called either here or during first crafting attempt
        CassetteRegistry.discoverDiscs();

        // 2. Load JSON recordables
        jsonMap.forEach((id, json) -> {
            try {
                RecordableData data =
                        RecordableData.fromJson(json.getAsJsonObject());

                CassetteRegistry.registerRecordable(id, data);
                System.out.println("Loaded recordable: " + id);

            } catch (Exception e) {
                System.out.println("❌ Failed to load recordable: " + id);
                e.printStackTrace();
            }
        });

        System.out.println("Total recordables: " + CassetteRegistry.getAll().size());
    }
}
