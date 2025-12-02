package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiskProbe {
    
    private final tHealthBeacon plugin;
    private final List<DiskSnapshot> diskHistory;
    private final Map<String, Long> regionFileSizes;
    private boolean running;
    
    public DiskProbe(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.diskHistory = new ArrayList<>();
        this.regionFileSizes = new HashMap<>();
        this.running = false;
    }
    
    public void probe() {
        if (!running) return;
        
        long startTime = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                long probeTime = System.currentTimeMillis() - startTime;

                File serverFolder = getServerFolder();
                if (serverFolder == null) {
                    plugin.getLogger().warning("Не удалось определить корневую директорию сервера");
                    return;
                }
                
                long totalSpace = serverFolder.getTotalSpace();
                long freeSpace = serverFolder.getFreeSpace();

                if (totalSpace < 0 || freeSpace < 0) {
                    plugin.getLogger().warning("Не удалось получить информацию о размере диска");
                    return;
                }
                
                long usedSpace = totalSpace - freeSpace;
                
                DiskSnapshot snapshot = new DiskSnapshot(
                    System.currentTimeMillis(),
                    totalSpace,
                    freeSpace,
                    usedSpace,
                    probeTime
                );
                
                diskHistory.add(snapshot);

                if (diskHistory.size() > 100) {
                    diskHistory.remove(0);
                }

                scanRegionFiles();
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при проверке диска: " + e.getMessage());
            }
        });
    }
    
    public void start() {
        running = true;
    }
    
    public void stop() {
        running = false;
    }

    private void scanRegionFiles() {
        regionFileSizes.clear();
        
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            File worldFolder = new File(Bukkit.getWorldContainer(), world.getName());
            File regionFolder = new File(worldFolder, "region");
            
            if (regionFolder.exists() && regionFolder.isDirectory()) {
                File[] regionFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
                if (regionFiles != null) {
                    long totalSize = 0;
                    for (File file : regionFiles) {
                        totalSize += file.length();
                    }
                    regionFileSizes.put(world.getName(), totalSize);
                }
            }
        }
    }

    public long measureSaveAllTime() {
        long startTime = System.currentTimeMillis();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.savePlayers();
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                world.save();
            }
        });
        
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    public DiskLatencyAnalysis analyzeLatency() {
        if (diskHistory.isEmpty()) {
            return new DiskLatencyAnalysis(0, false, "Недостаточно данных");
        }
        
        long avgLatency = diskHistory.stream()
            .mapToLong(s -> s.probeTime)
            .sum() / diskHistory.size();
        
        int threshold = plugin.getConfig().getInt("disk-latency-threshold", 2000);
        boolean hasIssue = avgLatency > threshold;
        
        String diagnosis;
        if (hasIssue) {
            diagnosis = String.format("Высокая задержка диска: %.2f мс (порог: %d мс)", 
                (double) avgLatency, threshold);
        } else {
            diagnosis = String.format("Задержка диска в норме: %.2f мс", (double) avgLatency);
        }
        
        return new DiskLatencyAnalysis(avgLatency, hasIssue, diagnosis);
    }

    private File getServerFolder() {

        File worldContainer = Bukkit.getWorldContainer();
        if (worldContainer != null && worldContainer.exists()) {
            return worldContainer;
        }

        try {
            File dataFolder = plugin.getDataFolder();
            if (dataFolder != null) {
                File parent = dataFolder.getParentFile();
                if (parent != null) {
                    File grandParent = parent.getParentFile();
                    if (grandParent != null && grandParent.exists()) {
                        return grandParent;
                    }
                }
            }
        } catch (Exception e) {

        }

        return new File(System.getProperty("user.dir"));
    }

    public DiskSnapshot getCurrentSnapshot() {
        if (diskHistory.isEmpty()) {
            File serverFolder = getServerFolder();
            if (serverFolder == null || !serverFolder.exists()) {

                return new DiskSnapshot(System.currentTimeMillis(), 0, 0, 0, 0);
            }
            
            long totalSpace = serverFolder.getTotalSpace();
            long freeSpace = serverFolder.getFreeSpace();

            if (totalSpace < 0 || freeSpace < 0) {

                return new DiskSnapshot(System.currentTimeMillis(), 0, 0, 0, 0);
            }
            
            long usedSpace = totalSpace - freeSpace;
            
            return new DiskSnapshot(System.currentTimeMillis(), totalSpace, freeSpace, usedSpace, 0);
        }
        return diskHistory.get(diskHistory.size() - 1);
    }
    
    public Map<String, Long> getRegionFileSizes() {
        return new HashMap<>(regionFileSizes);
    }

    public static class DiskSnapshot {
        public final long timestamp;
        public final long totalSpace;
        public final long freeSpace;
        public final long usedSpace;
        public final long probeTime;
        
        public DiskSnapshot(long timestamp, long totalSpace, long freeSpace, long usedSpace, long probeTime) {
            this.timestamp = timestamp;
            this.totalSpace = totalSpace;
            this.freeSpace = freeSpace;
            this.usedSpace = usedSpace;
            this.probeTime = probeTime;
        }
    }
    
    public static class DiskLatencyAnalysis {
        public final long avgLatency;
        public final boolean hasIssue;
        public final String diagnosis;
        
        public DiskLatencyAnalysis(long avgLatency, boolean hasIssue, String diagnosis) {
            this.avgLatency = avgLatency;
            this.hasIssue = hasIssue;
            this.diagnosis = diagnosis;
        }
    }
}

