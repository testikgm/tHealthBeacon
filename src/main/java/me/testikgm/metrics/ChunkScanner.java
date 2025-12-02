package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.*;

public class ChunkScanner {
    
    private final tHealthBeacon plugin;
    private final Map<String, List<HotZone>> hotZones;
    private final Map<String, Integer> chunkCounts;
    private boolean running;
    
    public ChunkScanner(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.hotZones = new HashMap<>();
        this.chunkCounts = new HashMap<>();
        this.running = false;
    }
    
    public void scan() {
        if (!running) return;
        
        int threshold = plugin.getConfig().getInt("hot-zone-threshold", 500);
        hotZones.clear();
        chunkCounts.clear();
        
        for (World world : Bukkit.getWorlds()) {
            List<HotZone> worldHotZones = new ArrayList<>();
            int totalChunks = 0;

            for (Chunk chunk : world.getLoadedChunks()) {
                int entityCount = chunk.getEntities().length;
                
                if (entityCount > threshold) {
                    worldHotZones.add(new HotZone(
                        world.getName(),
                        chunk.getX(),
                        chunk.getZ(),
                        entityCount,
                        chunk.getEntities()
                    ));
                }
            }

            totalChunks = world.getLoadedChunks().length;
            
            hotZones.put(world.getName(), worldHotZones);
            chunkCounts.put(world.getName(), totalChunks);
        }
    }
    
    public void start() {
        running = true;
    }
    
    public void stop() {
        running = false;
    }

    public List<HotZone> getHotZones(String worldName) {
        return new ArrayList<>(hotZones.getOrDefault(worldName, new ArrayList<>()));
    }

    public Map<String, List<HotZone>> getAllHotZones() {
        return new HashMap<>(hotZones);
    }

    public int getLoadedChunkCount(String worldName) {
        return chunkCounts.getOrDefault(worldName, 0);
    }

    public WorldStats getWorldStats() {
        Map<String, Integer> entityCountsByType = new HashMap<>();
        Map<String, Integer> chunkCountsByWorld = new HashMap<>();
        Map<String, List<Entity>> entitiesByWorld = new HashMap<>();
        
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            int chunks = world.getLoadedChunks().length;
            chunkCountsByWorld.put(worldName, chunks);
            
            List<Entity> worldEntities = new ArrayList<>();
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    String entityType = entity.getType().name().toLowerCase();
                    entityCountsByType.put(entityType, 
                        entityCountsByType.getOrDefault(entityType, 0) + 1);
                    worldEntities.add(entity);
                }
            }
            entitiesByWorld.put(worldName, worldEntities);
        }
        
        return new WorldStats(entityCountsByType, chunkCountsByWorld, entitiesByWorld);
    }

    public List<RegionData> getRegionData(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return new ArrayList<>();
        }

        Map<String, RegionData> regions = new HashMap<>();
        
        for (Chunk chunk : world.getLoadedChunks()) {

            int regionX = Math.floorDiv(chunk.getX(), 32);
            int regionZ = Math.floorDiv(chunk.getZ(), 32);
            String regionKey = regionX + "," + regionZ;
            
            RegionData region = regions.computeIfAbsent(regionKey, 
                k -> new RegionData(worldName, regionX, regionZ));

            Entity[] entities = chunk.getEntities();
            region.totalEntities += entities.length;
            region.totalChunks++;

            for (Entity entity : entities) {
                String entityType = entity.getType().name().toLowerCase();
                region.entityCounts.put(entityType, 
                    region.entityCounts.getOrDefault(entityType, 0) + 1);
            }

            region.chunks.add(new ChunkInfo(chunk.getX(), chunk.getZ(), entities.length));
        }
        
        return new ArrayList<>(regions.values());
    }

    public Map<String, Integer> getEntityCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    String entityType = entity.getType().name().toLowerCase();
                    counts.put(entityType, counts.getOrDefault(entityType, 0) + 1);
                }
            }
        }
        return counts;
    }

    public List<PersistentChunk> findPersistentChunks() {
        List<PersistentChunk> persistent = new ArrayList<>();

        return persistent;
    }

    public static class HotZone {
        public final String world;
        public final int x;
        public final int z;
        public final int entityCount;
        public final Entity[] entities;
        
        public HotZone(String world, int x, int z, int entityCount, Entity[] entities) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.entityCount = entityCount;
            this.entities = entities;
        }
        
        public String getLocationString() {
            return world + ": (" + x + ", " + z + ")";
        }
    }
    
    public static class PersistentChunk {
        public final String world;
        public final int x;
        public final int z;
        public final String reason;
        
        public PersistentChunk(String world, int x, int z, String reason) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.reason = reason;
        }
    }
    
    public static class WorldStats {
        public final Map<String, Integer> entityCountsByType;
        public final Map<String, Integer> chunkCountsByWorld;
        public final Map<String, List<Entity>> entitiesByWorld;
        
        public WorldStats(Map<String, Integer> entityCountsByType,
                         Map<String, Integer> chunkCountsByWorld,
                         Map<String, List<Entity>> entitiesByWorld) {
            this.entityCountsByType = entityCountsByType;
            this.chunkCountsByWorld = chunkCountsByWorld;
            this.entitiesByWorld = entitiesByWorld;
        }
    }
    
    public static class RegionData {
        public final String world;
        public final int regionX;
        public final int regionZ;
        public int totalEntities;
        public int totalChunks;
        public final Map<String, Integer> entityCounts;
        public final List<ChunkInfo> chunks;
        
        public RegionData(String world, int regionX, int regionZ) {
            this.world = world;
            this.regionX = regionX;
            this.regionZ = regionZ;
            this.totalEntities = 0;
            this.totalChunks = 0;
            this.entityCounts = new HashMap<>();
            this.chunks = new ArrayList<>();
        }
        
        public String getRegionString() {
            return "Region #" + (regionX * 1000 + regionZ);
        }
    }
    
    public static class ChunkInfo {
        public final int x;
        public final int z;
        public final int entityCount;
        
        public ChunkInfo(int x, int z, int entityCount) {
            this.x = x;
            this.z = z;
            this.entityCount = entityCount;
        }
        
        public String getLocationString() {
            return x + ", " + z + " (" + entityCount + " entities)";
        }
    }
}

