package net.walkman.cassette;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class RecordableDataLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();

    public RecordableDataLoader() {

        super(GSON, "recordables");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         ResourceManager manager,
                         ProfilerFiller profiler) {

        System.out.println("RecordableDataLoader fired!");
        System.out.println("Found recordables: " + jsonMap.keySet());

        CassetteRegistry.clear();

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


        CassetteRegistry.discoverDiscs();
        System.out.println("Auto-discovered music discs. Total recordables: " + CassetteRegistry.getAll().size());
    }
}
