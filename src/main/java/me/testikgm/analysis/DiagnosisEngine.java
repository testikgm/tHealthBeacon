package me.testikgm.analysis;

import me.testikgm.tHealthBeacon;
import me.testikgm.metrics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiagnosisEngine {
    
    private final tHealthBeacon plugin;
    
    public DiagnosisEngine(tHealthBeacon plugin) {
        this.plugin = plugin;
    }

    public Diagnosis analyze() {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> info = new ArrayList<>();

        TpsAnalyzer tpsAnalyzer = plugin.getTpsAnalyzer();
        TpsAnalyzer.TpsTrend tpsTrend = tpsAnalyzer.analyzeTrend(10);
        
        if (tpsTrend.average < 18.0) {
            issues.add(String.format("Низкий TPS: %.2f (целевой: 20.0)", tpsTrend.average));
        } else if (tpsTrend.average < 19.0) {
            warnings.add(String.format("TPS немного ниже нормы: %.2f", tpsTrend.average));
        }
        
        if (tpsTrend.stdDev > 2.0) {
            issues.add(String.format("Нестабильный TPS: отклонение %.2f", tpsTrend.stdDev));
        }

        List<TpsAnalyzer.TpsDrop> drops = tpsAnalyzer.detectDrops(18.0);
        if (!drops.isEmpty()) {
            warnings.add(String.format("Обнаружено %d просадок TPS ниже 18.0", drops.size()));
        }

        MemoryMonitor memoryMonitor = plugin.getMemoryMonitor();
        MemoryMonitor.MemoryLeakAnalysis memoryLeak = memoryMonitor.analyzeMemoryLeak();
        
        if (memoryLeak.possibleLeak) {
            issues.add("⚠ Возможная утечка памяти: " + memoryLeak.diagnosis);
        }
        
        MemoryMonitor.MemorySnapshot currentMemory = memoryMonitor.getCurrentSnapshot();
        double memoryUsagePercent = (currentMemory.used * 100.0) / currentMemory.max;
        if (memoryUsagePercent > 90) {
            issues.add(String.format("Критическое использование памяти: %.1f%%", memoryUsagePercent));
        } else if (memoryUsagePercent > 80) {
            warnings.add(String.format("Высокое использование памяти: %.1f%%", memoryUsagePercent));
        }

        DiskProbe diskProbe = plugin.getDiskProbe();
        DiskProbe.DiskLatencyAnalysis diskLatency = diskProbe.analyzeLatency();
        
        if (diskLatency.hasIssue) {
            issues.add("💾 " + diskLatency.diagnosis);
        }
        
        DiskProbe.DiskSnapshot diskSnapshot = diskProbe.getCurrentSnapshot();
        double diskUsagePercent = (diskSnapshot.usedSpace * 100.0) / diskSnapshot.totalSpace;
        if (diskUsagePercent > 90) {
            warnings.add(String.format("Мало свободного места на диске: %.1f%% свободно", 
                100.0 - diskUsagePercent));
        }

        NetworkMonitor networkMonitor = plugin.getNetworkMonitor();
        NetworkMonitor.NetworkStabilityAnalysis networkStability = networkMonitor.analyzeStability();
        
        if (networkStability.hasIssue) {
            warnings.add("🌐 " + networkStability.diagnosis);
        }
        
        List<NetworkMonitor.PlayerNetworkData> badConnections = networkMonitor.findBadConnections();
        if (!badConnections.isEmpty()) {
            info.add(String.format("Игроки с плохим соединением: %d", badConnections.size()));
        }

        ChunkScanner chunkScanner = plugin.getChunkScanner();
        Map<String, List<ChunkScanner.HotZone>> allHotZones = chunkScanner.getAllHotZones();
        
        int totalHotZones = allHotZones.values().stream()
            .mapToInt(List::size)
            .sum();
        
        if (totalHotZones > 0) {
            warnings.add(String.format("Обнаружено %d горячих зон с большим количеством сущностей", totalHotZones));
        }

        String summary;
        if (issues.isEmpty() && warnings.isEmpty()) {
            summary = "Сервер работает стабильно, проблем не обнаружено.";
        } else if (issues.isEmpty()) {
            summary = "Сервер работает нормально, но есть несколько предупреждений.";
        } else {
            summary = String.format("Обнаружено %d проблем и %d предупреждений.", 
                issues.size(), warnings.size());
        }
        
        return new Diagnosis(summary, issues, warnings, info);
    }

    public static class Diagnosis {
        public final String summary;
        public final List<String> issues;
        public final List<String> warnings;
        public final List<String> info;
        
        public Diagnosis(String summary, List<String> issues, List<String> warnings, List<String> info) {
            this.summary = summary;
            this.issues = new ArrayList<>(issues);
            this.warnings = new ArrayList<>(warnings);
            this.info = new ArrayList<>(info);
        }
    }
}

