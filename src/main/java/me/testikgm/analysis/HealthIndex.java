package me.testikgm.analysis;

import me.testikgm.tHealthBeacon;
import me.testikgm.metrics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HealthIndex {
    
    private final tHealthBeacon plugin;
    
    public HealthIndex(tHealthBeacon plugin) {
        this.plugin = plugin;
    }

    public ServerResilienceScore calculateScore() {
        List<ScoreComponent> components = new ArrayList<>();
        double totalScore = 0.0;
        double maxScore = 0.0;

        TpsAnalyzer tpsAnalyzer = plugin.getTpsAnalyzer();
        TpsAnalyzer.TpsTrend tpsTrend = tpsAnalyzer.analyzeTrend(10);
        double tpsScore = calculateTpsScore(tpsTrend);
        components.add(new ScoreComponent("Стабильность TPS", tpsScore, 25.0));
        totalScore += tpsScore;
        maxScore += 25.0;

        MemoryMonitor memoryMonitor = plugin.getMemoryMonitor();
        MemoryMonitor.MemoryLeakAnalysis memoryLeak = memoryMonitor.analyzeMemoryLeak();
        MemoryMonitor.MemorySnapshot currentMemory = memoryMonitor.getCurrentSnapshot();
        double memoryScore = calculateMemoryScore(memoryLeak, currentMemory);
        components.add(new ScoreComponent("Использование памяти", memoryScore, 20.0));
        totalScore += memoryScore;
        maxScore += 20.0;

        DiskProbe diskProbe = plugin.getDiskProbe();
        DiskProbe.DiskLatencyAnalysis diskLatency = diskProbe.analyzeLatency();
        DiskProbe.DiskSnapshot diskSnapshot = diskProbe.getCurrentSnapshot();
        double diskScore = calculateDiskScore(diskLatency, diskSnapshot);
        components.add(new ScoreComponent("Производительность диска", diskScore, 15.0));
        totalScore += diskScore;
        maxScore += 15.0;

        NetworkMonitor networkMonitor = plugin.getNetworkMonitor();
        NetworkMonitor.NetworkStabilityAnalysis networkStability = networkMonitor.analyzeStability();
        double networkScore = calculateNetworkScore(networkStability);
        components.add(new ScoreComponent("Стабильность сети", networkScore, 15.0));
        totalScore += networkScore;
        maxScore += 15.0;

        ChunkScanner chunkScanner = plugin.getChunkScanner();
        Map<String, List<ChunkScanner.HotZone>> allHotZones = chunkScanner.getAllHotZones();
        int totalHotZones = allHotZones.values().stream().mapToInt(List::size).sum();
        double chunkScore = calculateChunkScore(totalHotZones);
        components.add(new ScoreComponent("Оптимизация чанков", chunkScore, 15.0));
        totalScore += chunkScore;
        maxScore += 15.0;

        int pluginCount = plugin.getServer().getPluginManager().getPlugins().length;
        double pluginScore = calculatePluginScore(pluginCount);
        components.add(new ScoreComponent("Количество плагинов", pluginScore, 10.0));
        totalScore += pluginScore;
        maxScore += 10.0;

        int finalScore = (int) Math.round((totalScore / maxScore) * 100);
        finalScore = Math.max(0, Math.min(100, finalScore));

        String interpretation = generateInterpretation(finalScore, components);
        
        return new ServerResilienceScore(finalScore, interpretation, components);
    }
    
    private double calculateTpsScore(TpsAnalyzer.TpsTrend trend) {
        if (trend.average >= 20.0 && trend.stdDev < 0.5) {
            return 25.0;
        } else if (trend.average >= 19.0 && trend.stdDev < 1.0) {
            return 20.0;
        } else if (trend.average >= 18.0 && trend.stdDev < 2.0) {
            return 15.0;
        } else if (trend.average >= 15.0) {
            return 10.0;
        } else {
            return 5.0;
        }
    }
    
    private double calculateMemoryScore(MemoryMonitor.MemoryLeakAnalysis leak, MemoryMonitor.MemorySnapshot snapshot) {
        double score = 20.0;

        if (leak.possibleLeak) {
            score -= 10.0;
        }

        double usagePercent = (snapshot.used * 100.0) / snapshot.max;
        if (usagePercent > 90) {
            score -= 10.0;
        } else if (usagePercent > 80) {
            score -= 5.0;
        }
        
        return Math.max(0, score);
    }
    
    private double calculateDiskScore(DiskProbe.DiskLatencyAnalysis latency, DiskProbe.DiskSnapshot snapshot) {
        double score = 15.0;

        if (latency.hasIssue) {
            score -= 7.0;
        }

        double freePercent = (snapshot.freeSpace * 100.0) / snapshot.totalSpace;
        if (freePercent < 10) {
            score -= 5.0;
        } else if (freePercent < 20) {
            score -= 2.0;
        }
        
        return Math.max(0, score);
    }
    
    private double calculateNetworkScore(NetworkMonitor.NetworkStabilityAnalysis stability) {
        double score = 15.0;

        if (stability.hasIssue) {
            score -= 7.0;
        }

        if (stability.avgPing > 200) {
            score -= 5.0;
        } else if (stability.avgPing > 100) {
            score -= 2.0;
        }
        
        return Math.max(0, score);
    }
    
    private double calculateChunkScore(int hotZones) {
        if (hotZones == 0) {
            return 15.0;
        } else if (hotZones <= 2) {
            return 12.0;
        } else if (hotZones <= 5) {
            return 8.0;
        } else {
            return 4.0;
        }
    }
    
    private double calculatePluginScore(int pluginCount) {
        if (pluginCount <= 20) {
            return 10.0;
        } else if (pluginCount <= 50) {
            return 7.0;
        } else if (pluginCount <= 100) {
            return 4.0;
        } else {
            return 2.0;
        }
    }
    
    private String generateInterpretation(int score, List<ScoreComponent> components) {
        List<String> mainIssues = new ArrayList<>();

        for (ScoreComponent component : components) {
            double percent = (component.score / component.maxScore) * 100;
            if (percent < 50) {
                mainIssues.add(component.name.toLowerCase());
            }
        }
        
        String interpretation;
        if (score >= 80) {
            interpretation = "Сервер работает отлично, проблем не обнаружено.";
        } else if (score >= 60) {
            interpretation = "Сервер работает нормально, но есть несколько проблем.";
        } else if (score >= 40) {
            interpretation = "Сервер показывает признаки усталости производительности.";
        } else {
            interpretation = "Сервер имеет серьёзные проблемы с производительностью.";
        }
        
        if (!mainIssues.isEmpty()) {
            interpretation += "\n> Основные проблемы: " + String.join(", ", mainIssues) + ".";
        }
        
        return interpretation;
    }

    public static class ScoreComponent {
        public final String name;
        public final double score;
        public final double maxScore;
        
        public ScoreComponent(String name, double score, double maxScore) {
            this.name = name;
            this.score = score;
            this.maxScore = maxScore;
        }
    }

    public static class ServerResilienceScore {
        public final int score;
        public final String interpretation;
        public final List<ScoreComponent> components;
        
        public ServerResilienceScore(int score, String interpretation, List<ScoreComponent> components) {
            this.score = score;
            this.interpretation = interpretation;
            this.components = new ArrayList<>(components);
        }
    }
}

