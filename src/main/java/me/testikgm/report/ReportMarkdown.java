package me.testikgm.report;

import me.testikgm.analysis.*;
import me.testikgm.metrics.*;
import me.testikgm.util.FileUtil;
import me.testikgm.util.TimeUtil;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;

public class ReportMarkdown {
    
    public static String generate(
        TpsAnalyzer tpsAnalyzer,
        MemoryMonitor memoryMonitor,
        PluginProfiler pluginProfiler,
        ChunkScanner chunkScanner,
        DiskProbe diskProbe,
        NetworkMonitor networkMonitor,
        DiagnosisEngine.Diagnosis diagnosis,
        List<RecommendationEngine.Recommendation> recommendations,
        HealthIndex.ServerResilienceScore srs
    ) {
        StringBuilder md = new StringBuilder();

        md.append("# 🧠 tHealthBeacon Report\n\n");
        md.append("**Дата:** ").append(TimeUtil.getCurrentTimeString()).append("\n\n");
        md.append("━━━━━━━━━━━━━━━━━━\n\n");

        md.append("## 📊 Информация о сервере\n\n");
        md.append("- **Minecraft:** ").append(Bukkit.getVersion()).append("\n");
        md.append("- **Плагины:** ").append(Bukkit.getPluginManager().getPlugins().length).append("\n");
        md.append("- **Время работы:** ").append(TimeUtil.formatDuration(System.currentTimeMillis() - getServerStartTime())).append("\n\n");

        md.append("## 📈 Индекс устойчивости сервера (SRS)\n\n");
        md.append("**SRS:** ").append(srs.score).append("/100\n\n");
        md.append("> ").append(srs.interpretation.replace("\n", "\n> ")).append("\n\n");

        TpsAnalyzer.TpsTrend tpsTrend = tpsAnalyzer.analyzeTrend(10);
        md.append("## 🧩 TPS Trend\n\n");
        md.append("- **Средний TPS:** ").append(String.format("%.2f", tpsTrend.average)).append("\n");
        md.append("- **Минимальный:** ").append(String.format("%.2f", tpsTrend.min)).append("\n");
        md.append("- **Максимальный:** ").append(String.format("%.2f", tpsTrend.max)).append("\n");
        md.append("- **Стабильность:** ").append(tpsTrend.stability).append("\n\n");

        MemoryMonitor.MemorySnapshot currentMemory = memoryMonitor.getCurrentSnapshot();
        MemoryMonitor.MemoryLeakAnalysis memoryLeak = memoryMonitor.analyzeMemoryLeak();
        md.append("## 🧠 Memory Trend\n\n");
        md.append("- **Использовано:** ").append(FileUtil.formatFileSize(currentMemory.used)).append("\n");
        md.append("- **Максимум:** ").append(FileUtil.formatFileSize(currentMemory.max)).append("\n");
        md.append("- **Использование:** ").append(String.format("%.1f%%", (currentMemory.used * 100.0) / currentMemory.max)).append("\n");
        if (memoryLeak.possibleLeak) {
            md.append("- **⚠ Возможная утечка памяти:** ").append(memoryLeak.diagnosis).append("\n");
        }
        md.append("\n");

        NetworkMonitor.NetworkStabilityAnalysis networkStability = networkMonitor.analyzeStability();
        md.append("## 🌐 Network\n\n");
        md.append("- **Средний пинг:** ").append(String.format("%.0f мс", networkStability.avgPing)).append("\n");
        md.append("- **Джиттер:** ±").append(String.format("%.0f%%", networkStability.jitterPercent)).append("\n");
        md.append("- **Статус:** ").append(networkStability.hasIssue ? "⚠ Нестабильно" : "✓ Стабильно").append("\n\n");

        List<PluginProfiler.PluginLoadInfo> topPlugins = pluginProfiler.getTopLoadTimes(5);
        md.append("## 📦 Plugins Impact (Top 5)\n\n");
        for (int i = 0; i < topPlugins.size(); i++) {
            PluginProfiler.PluginLoadInfo plugin = topPlugins.get(i);
            md.append(i + 1).append(". **").append(plugin.name).append("** – ").append(TimeUtil.formatDuration(plugin.loadTime)).append("\n");
        }
        md.append("\n");

        DiskProbe.DiskSnapshot diskSnapshot = diskProbe.getCurrentSnapshot();
        DiskProbe.DiskLatencyAnalysis diskLatency = diskProbe.analyzeLatency();
        md.append("## 💾 Disk\n\n");
        md.append("- **Использовано:** ").append(FileUtil.formatFileSize(diskSnapshot.usedSpace)).append("\n");
        md.append("- **Свободно:** ").append(FileUtil.formatFileSize(diskSnapshot.freeSpace)).append("\n");
        md.append("- **Задержка:** ").append(String.format("%.2f мс", (double) diskLatency.avgLatency)).append("\n");
        if (diskLatency.hasIssue) {
            md.append("- **⚠ Проблема:** ").append(diskLatency.diagnosis).append("\n");
        }
        md.append("\n");

        Map<String, List<ChunkScanner.HotZone>> allHotZones = chunkScanner.getAllHotZones();
        md.append("## 🔥 Hot Zones\n\n");
        if (allHotZones.isEmpty() || allHotZones.values().stream().allMatch(List::isEmpty)) {
            md.append("Горячих зон не обнаружено\n\n");
        } else {
            for (Map.Entry<String, List<ChunkScanner.HotZone>> entry : allHotZones.entrySet()) {
                for (ChunkScanner.HotZone zone : entry.getValue()) {
                    md.append("- **").append(zone.getLocationString()).append("** – ").append(zone.entityCount).append(" сущностей\n");
                }
            }
            md.append("\n");
        }

        md.append("## ✅ Diagnosis\n\n");
        md.append("**Итог:** ").append(diagnosis.summary).append("\n\n");
        if (!diagnosis.issues.isEmpty()) {
            md.append("### Проблемы:\n\n");
            for (String issue : diagnosis.issues) {
                md.append("- ❌ ").append(issue).append("\n");
            }
            md.append("\n");
        }
        if (!diagnosis.warnings.isEmpty()) {
            md.append("### Предупреждения:\n\n");
            for (String warning : diagnosis.warnings) {
                md.append("- ⚠ ").append(warning).append("\n");
            }
            md.append("\n");
        }

        md.append("## 🩺 Recommendation\n\n");
        if (recommendations.isEmpty()) {
            md.append("Рекомендаций нет, сервер работает оптимально.\n\n");
        } else {
            for (RecommendationEngine.Recommendation rec : recommendations) {
                md.append("### ").append(rec.title).append(" (").append(rec.priority.name()).append(")\n\n");
                md.append(rec.description).append("\n\n");
            }
        }
        
        md.append("━━━━━━━━━━━━━━━━━━\n");
        
        return md.toString();
    }
    
    private static long getServerStartTime() {
        return System.currentTimeMillis() - (1000 * 60 * 60 * 3);
    }
}

