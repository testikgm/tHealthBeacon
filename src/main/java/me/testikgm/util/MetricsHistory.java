package me.testikgm.util;

import me.testikgm.tHealthBeacon;
import me.testikgm.metrics.MemoryMonitor;
import me.testikgm.metrics.NetworkMonitor;
import me.testikgm.metrics.CpuMonitor;
import me.testikgm.metrics.MsptMonitor;
import me.testikgm.metrics.ChunkScanner;
import me.testikgm.metrics.PluginProfiler;
import me.testikgm.analysis.HealthIndex;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsHistory {
    
    private final tHealthBeacon plugin;
    private final File historyFile;
    private final File snapshotsFolder;
    private final int keepSnapshots;
    
    public MetricsHistory(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.historyFile = new File(plugin.getDataFolder(), "history.yml");

        String snapshotFolderPath = plugin.getConfig().getString("collector.snapshot_folder", 
            "plugins/tHealthBeacon/snapshots/");
        this.snapshotsFolder = new File(snapshotFolderPath);
        this.keepSnapshots = plugin.getConfig().getInt("collector.keep_snapshots", 50);

        if (!snapshotsFolder.exists()) {
            snapshotsFolder.mkdirs();
        }
    }

    public void saveSnapshot() {
        long timestamp = System.currentTimeMillis();
        
        try {

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("timestamp", timestamp);

            double tps = plugin.getTpsAnalyzer().getCurrentTpsValue();
            snapshot.put("tps", tps);

            Map<String, Double> tpsTrends = new HashMap<>();
            tpsTrends.put("1m", plugin.getTpsAnalyzer().analyzeTrend(1).average);
            tpsTrends.put("5m", plugin.getTpsAnalyzer().analyzeTrend(5).average);
            tpsTrends.put("15m", plugin.getTpsAnalyzer().analyzeTrend(15).average);
            snapshot.put("tps_trends", tpsTrends);

            MsptMonitor.MsptStats msptStats = plugin.getMsptMonitor().getStats();
            Map<String, Double> mspt = new HashMap<>();
            mspt.put("min", msptStats.min);
            mspt.put("median", msptStats.median);
            mspt.put("p95", msptStats.p95);
            mspt.put("max", msptStats.max);
            snapshot.put("mspt", mspt);

            MemoryMonitor.MemorySnapshot memory = plugin.getMemoryMonitor().getCurrentSnapshot();
            Map<String, Object> memoryData = new HashMap<>();
            memoryData.put("used", memory.used);
            memoryData.put("max", memory.max);
            memoryData.put("percent", (memory.used * 100.0) / memory.max);

            MemoryMonitor.PhysicalMemoryInfo physicalMemory = plugin.getMemoryMonitor().getPhysicalMemoryInfo();
            Map<String, Object> physicalMemoryData = new HashMap<>();
            physicalMemoryData.put("total", physicalMemory.totalPhysical);
            physicalMemoryData.put("used", physicalMemory.usedPhysical);
            physicalMemoryData.put("free", physicalMemory.freePhysical);
            physicalMemoryData.put("percent", (physicalMemory.usedPhysical * 100.0) / physicalMemory.totalPhysical);
            memoryData.put("physical", physicalMemoryData);

            Map<String, Object> swapData = new HashMap<>();
            swapData.put("total", physicalMemory.totalSwap);
            swapData.put("used", physicalMemory.usedSwap);
            swapData.put("free", physicalMemory.freeSwap);
            swapData.put("percent", physicalMemory.totalSwap > 0 ? 
                (physicalMemory.usedSwap * 100.0) / physicalMemory.totalSwap : 0.0);
            memoryData.put("swap", swapData);

            Map<String, Object> processMemoryData = new HashMap<>();
            processMemoryData.put("used", memory.used);
            processMemoryData.put("max", memory.max);
            processMemoryData.put("percent", (memory.used * 100.0) / memory.max);
            memoryData.put("process", processMemoryData);
            
            snapshot.put("memory", memoryData);

            CpuMonitor.CpuStats cpuStats = plugin.getCpuMonitor().getStats(1);
            Map<String, Object> cpuData = new HashMap<>();
            cpuData.put("system_1m", cpuStats.systemAvg);
            cpuData.put("system_15m", plugin.getCpuMonitor().getStats(15).systemAvg);
            cpuData.put("process_1m", cpuStats.processAvg);
            cpuData.put("process_15m", plugin.getCpuMonitor().getStats(15).processAvg);
            snapshot.put("cpu", cpuData);

            MemoryMonitor.GcStats gcStats = plugin.getMemoryMonitor().getGcStats();
            Map<String, Object> gcData = new HashMap<>();
            Map<String, Object> youngGc = new HashMap<>();
            youngGc.put("total", gcStats.youngGc.total);
            youngGc.put("avg_time", gcStats.youngGc.avgTime);
            youngGc.put("avg_freq", gcStats.youngGc.avgFreq);
            Map<String, Object> oldGc = new HashMap<>();
            oldGc.put("total", gcStats.oldGc.total);
            oldGc.put("avg_time", gcStats.oldGc.avgTime);
            oldGc.put("avg_freq", gcStats.oldGc.avgFreq);
            gcData.put("young", youngGc);
            gcData.put("old", oldGc);
            snapshot.put("gc", gcData);

            NetworkMonitor.NetworkStabilityAnalysis network = plugin.getNetworkMonitor().analyzeStability();
            Map<String, Object> networkData = new HashMap<>();
            networkData.put("avgPing", network.avgPing);
            networkData.put("jitter", network.jitterPercent);
            snapshot.put("network", networkData);

            Map<String, Object> diskData = new HashMap<>();
            try {
                me.testikgm.metrics.DiskProbe.DiskSnapshot disk = plugin.getDiskProbe().getCurrentSnapshot();
                diskData.put("used", disk.usedSpace);
                diskData.put("free", disk.freeSpace);
                diskData.put("total", disk.usedSpace + disk.freeSpace);
                diskData.put("percent", ((disk.usedSpace * 100.0) / (disk.usedSpace + disk.freeSpace)));
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при получении данных диска: " + e.getMessage());
            }
            snapshot.put("disk", diskData);

            HealthIndex.ServerResilienceScore srs = plugin.getHealthIndex().calculateScore();
            snapshot.put("srs", srs.score);

            ChunkScanner.WorldStats worldStats = plugin.getChunkScanner().getWorldStats();
            Map<String, Object> chunksData = new HashMap<>();
            chunksData.put("totalChunks", worldStats.chunkCountsByWorld.values().stream().mapToInt(Integer::intValue).sum());
            chunksData.put("totalEntities", worldStats.entityCountsByType.values().stream().mapToInt(Integer::intValue).sum());
            chunksData.put("chunksByWorld", worldStats.chunkCountsByWorld);
            chunksData.put("entitiesByType", worldStats.entityCountsByType);
            snapshot.put("chunks", chunksData);

            List<PluginProfiler.PluginLoadInfo> topPlugins = plugin.getPluginProfiler().getTopLoadTimes(10);
            List<PluginProfiler.PluginIssue> pluginIssues = plugin.getPluginProfiler().analyzePluginIssues();
            Map<String, Object> pluginsData = new HashMap<>();
            List<Map<String, Object>> topPluginsList = new ArrayList<>();
            for (PluginProfiler.PluginLoadInfo pluginInfo : topPlugins) {
                Map<String, Object> pluginMap = new HashMap<>();
                pluginMap.put("name", pluginInfo.name);
                pluginMap.put("loadTime", pluginInfo.loadTime);
                topPluginsList.add(pluginMap);
            }
            pluginsData.put("topPlugins", topPluginsList);
            List<Map<String, Object>> issuesList = new ArrayList<>();
            for (PluginProfiler.PluginIssue issue : pluginIssues) {
                Map<String, Object> issueMap = new HashMap<>();
                issueMap.put("pluginName", issue.pluginName);
                issueMap.put("issueType", issue.issueType.toString());
                issueMap.put("description", issue.description);
                issuesList.add(issueMap);
            }
            pluginsData.put("issues", issuesList);
            snapshot.put("plugins", pluginsData);

            String jsonSnapshot = JsonUtil.toJson(snapshot);
            File jsonFile = new File(snapshotsFolder, "snapshot-" + timestamp + ".json");
            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(jsonSnapshot);
            }

            cleanupOldSnapshots();

            saveSnapshotToYaml(timestamp, snapshot);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось сохранить снимок метрик: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveSnapshotToYaml(long timestamp, Map<String, Object> snapshot) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(historyFile);
            
            String key = "snapshots." + timestamp;
            config.set(key + ".tps", snapshot.get("tps"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> memory = (Map<String, Object>) snapshot.get("memory");
            config.set(key + ".memory.used", memory.get("used"));
            config.set(key + ".memory.max", memory.get("max"));
            config.set(key + ".memory.percent", memory.get("percent"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> network = (Map<String, Object>) snapshot.get("network");
            config.set(key + ".network.avgPing", network.get("avgPing"));
            config.set(key + ".network.jitter", network.get("jitter"));
            
            config.set(key + ".srs", snapshot.get("srs"));

            if (config.contains("snapshots")) {
                List<String> snapshots = new ArrayList<>(
                    config.getConfigurationSection("snapshots").getKeys(false));
                if (snapshots.size() > 100) {
                    snapshots.sort(String::compareTo);
                    for (int i = 0; i < snapshots.size() - 100; i++) {
                        config.set("snapshots." + snapshots.get(i), null);
                    }
                }
            }
            
            config.save(historyFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось сохранить снимок в YAML: " + e.getMessage());
        }
    }

    private void cleanupOldSnapshots() {
        try {
            File[] files = snapshotsFolder.listFiles((dir, name) -> name.startsWith("snapshot-") && name.endsWith(".json"));
            if (files == null || files.length <= keepSnapshots) {
                return;
            }

            List<File> fileList = new ArrayList<>();
            for (File file : files) {
                fileList.add(file);
            }
            fileList.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));

            int toDelete = fileList.size() - keepSnapshots;
            for (int i = 0; i < toDelete; i++) {
                fileList.get(i).delete();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при очистке старых снимков: " + e.getMessage());
        }
    }

    public List<String> getJsonSnapshotStrings(int hours) {
        List<String> jsonStrings = new ArrayList<>();
        try {
            long cutoff = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);
            File[] files = snapshotsFolder.listFiles((dir, name) -> name.startsWith("snapshot-") && name.endsWith(".json"));
            
            if (files == null) {
                return jsonStrings;
            }

            List<File> sortedFiles = new ArrayList<>();
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    String timestampStr = fileName.substring(9, fileName.length() - 5);
                    long timestamp = Long.parseLong(timestampStr);
                    
                    if (timestamp >= cutoff) {
                        sortedFiles.add(file);
                    }
                } catch (Exception e) {

                }
            }

            sortedFiles.sort((a, b) -> {
                try {
                    long timestampA = Long.parseLong(a.getName().substring(9, a.getName().length() - 5));
                    long timestampB = Long.parseLong(b.getName().substring(9, b.getName().length() - 5));
                    return Long.compare(timestampA, timestampB);
                } catch (Exception e) {
                    return 0;
                }
            });

            for (File file : sortedFiles) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    jsonStrings.add(content);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при чтении снимка " + file.getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении JSON снимков: " + e.getMessage());
        }
        
        return jsonStrings;
    }

    public int getSnapshotCount() {
        File[] files = snapshotsFolder.listFiles((dir, name) -> name.startsWith("snapshot-") && name.endsWith(".json"));
        return files != null ? files.length : 0;
    }

    public List<MetricsSnapshot> loadAllJsonSnapshots() {
        List<MetricsSnapshot> snapshots = new ArrayList<>();
        try {
            File[] files = snapshotsFolder.listFiles((dir, name) -> name.startsWith("snapshot-") && name.endsWith(".json"));
            if (files == null) {
                return snapshots;
            }

            List<File> sortedFiles = new ArrayList<>();
            for (File file : files) {
                sortedFiles.add(file);
            }
            sortedFiles.sort((a, b) -> {
                try {
                    long timestampA = Long.parseLong(a.getName().substring(9, a.getName().length() - 5));
                    long timestampB = Long.parseLong(b.getName().substring(9, b.getName().length() - 5));
                    return Long.compare(timestampA, timestampB);
                } catch (Exception e) {
                    return 0;
                }
            });

            for (File file : sortedFiles) {
                try {
                    String jsonContent = new String(Files.readAllBytes(file.toPath()));
                    MetricsSnapshot snapshot = parseJsonSnapshot(jsonContent);
                    if (snapshot != null) {
                        snapshots.add(snapshot);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при парсинге снимка " + file.getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при загрузке JSON снимков: " + e.getMessage());
        }
        
        return snapshots;
    }

    private MetricsSnapshot parseJsonSnapshot(String json) {
        try {

            long timestamp = extractLong(json, "\"timestamp\":", 0);

            double tps = extractDouble(json, "\"tps\":", 20.0);

            long memoryUsed = 0;
            long memoryMax = 0;
            double memoryPercent = 0.0;
            
            int memoryStart = json.indexOf("\"memory\":{");
            if (memoryStart != -1) {
                int memoryEnd = findMatchingBrace(json, memoryStart + 9);
                if (memoryEnd != -1) {
                    String memoryJson = json.substring(memoryStart, memoryEnd + 1);

                    int processStart = memoryJson.indexOf("\"process\":{");
                    if (processStart != -1) {
                        int processEnd = findMatchingBrace(memoryJson, processStart + 10);
                        if (processEnd != -1) {
                            String processJson = memoryJson.substring(processStart, processEnd + 1);
                            memoryUsed = extractLong(processJson, "\"used\":", 0);
                            memoryMax = extractLong(processJson, "\"max\":", 0);
                            memoryPercent = extractDouble(processJson, "\"percent\":", 0.0);
                        }
                    }

                    if (memoryUsed == 0) {
                        memoryUsed = extractLong(memoryJson, "\"used\":", 0);
                        memoryMax = extractLong(memoryJson, "\"max\":", 0);
                        memoryPercent = extractDouble(memoryJson, "\"percent\":", 0.0);
                    }
                }
            }

            double networkPing = extractDouble(json, "\"avgPing\":", 0.0);
            if (networkPing == 0.0) {
                int networkStart = json.indexOf("\"network\":{");
                if (networkStart != -1) {
                    int networkEnd = findMatchingBrace(json, networkStart + 10);
                    if (networkEnd != -1) {
                        String networkJson = json.substring(networkStart, networkEnd + 1);
                        networkPing = extractDouble(networkJson, "\"avgPing\":", 0.0);
                    }
                }
            }
            double networkJitter = extractDouble(json, "\"jitter\":", 0.0);
            if (networkJitter == 0.0) {
                int networkStart = json.indexOf("\"network\":{");
                if (networkStart != -1) {
                    int networkEnd = findMatchingBrace(json, networkStart + 10);
                    if (networkEnd != -1) {
                        String networkJson = json.substring(networkStart, networkEnd + 1);
                        networkJitter = extractDouble(networkJson, "\"jitter\":", 0.0);
                    }
                }
            }

            int srs = (int) extractDouble(json, "\"srs\":", 0.0);
            
            return new MetricsSnapshot(timestamp, tps, memoryUsed, memoryMax, memoryPercent, networkPing, networkJitter, srs);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при парсинге JSON снимка: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private int findMatchingBrace(String json, int start) {
        int depth = 1;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
    
    private long extractLong(String json, String pattern, long defaultValue) {
        try {
            int start = json.indexOf(pattern);
            if (start == -1) return defaultValue;
            start += pattern.length();

            while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                start++;
            }

            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            String value = json.substring(start, end);
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private double extractDouble(String json, String pattern, double defaultValue) {
        try {
            int start = json.indexOf(pattern);
            if (start == -1) return defaultValue;
            start += pattern.length();

            while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                start++;
            }

            int end = start;
            while (end < json.length() && 
                   (Character.isDigit(json.charAt(end)) || 
                    json.charAt(end) == '.' || 
                    json.charAt(end) == '-' ||
                    json.charAt(end) == 'E' ||
                    json.charAt(end) == 'e' ||
                    json.charAt(end) == '+')) {
                end++;
            }
            String value = json.substring(start, end);
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public MetricsSnapshot getLastSnapshot() {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(historyFile);
            
            if (!config.contains("snapshots")) {
                return null;
            }
            
            List<String> snapshots = new ArrayList<>(config.getConfigurationSection("snapshots").getKeys(false));
            if (snapshots.isEmpty()) {
                return null;
            }
            
            snapshots.sort((a, b) -> Long.compare(Long.parseLong(b), Long.parseLong(a)));
            String lastKey = snapshots.get(0);
            String key = "snapshots." + lastKey;
            
            return new MetricsSnapshot(
                Long.parseLong(lastKey),
                config.getDouble(key + ".tps", 20.0),
                config.getLong(key + ".memory.used", 0),
                config.getLong(key + ".memory.max", 0),
                config.getDouble(key + ".memory.percent", 0),
                config.getDouble(key + ".network.avgPing", 0),
                config.getDouble(key + ".network.jitter", 0),
                config.getInt(key + ".srs", 0)
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось загрузить историю метрик: " + e.getMessage());
            return null;
        }
    }

    public List<MetricsSnapshot> getSnapshots(int hours) {
        List<MetricsSnapshot> snapshots = new ArrayList<>();
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(historyFile);
            
            if (!config.contains("snapshots")) {
                return snapshots;
            }
            
            long cutoff = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);
            
            for (String timestamp : config.getConfigurationSection("snapshots").getKeys(false)) {
                long ts = Long.parseLong(timestamp);
                if (ts >= cutoff) {
                    String key = "snapshots." + timestamp;
                    snapshots.add(new MetricsSnapshot(
                        ts,
                        config.getDouble(key + ".tps", 20.0),
                        config.getLong(key + ".memory.used", 0),
                        config.getLong(key + ".memory.max", 0),
                        config.getDouble(key + ".memory.percent", 0),
                        config.getDouble(key + ".network.avgPing", 0),
                        config.getDouble(key + ".network.jitter", 0),
                        config.getInt(key + ".srs", 0)
                    ));
                }
            }
            
            snapshots.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось загрузить историю метрик: " + e.getMessage());
        }
        
        return snapshots;
    }
    
    public static class MetricsSnapshot {
        public final long timestamp;
        public final double tps;
        public final long memoryUsed;
        public final long memoryMax;
        public final double memoryPercent;
        public final double networkPing;
        public final double networkJitter;
        public final int srs;
        
        public MetricsSnapshot(long timestamp, double tps, long memoryUsed, long memoryMax, 
                              double memoryPercent, double networkPing, double networkJitter, int srs) {
            this.timestamp = timestamp;
            this.tps = tps;
            this.memoryUsed = memoryUsed;
            this.memoryMax = memoryMax;
            this.memoryPercent = memoryPercent;
            this.networkPing = networkPing;
            this.networkJitter = networkJitter;
            this.srs = srs;
        }
    }
}

