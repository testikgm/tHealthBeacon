package me.testikgm.analysis;

import me.testikgm.tHealthBeacon;
import me.testikgm.metrics.*;
import me.testikgm.util.FileUtil;
import me.testikgm.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecommendationEngine {
    
    private final tHealthBeacon plugin;
    
    public RecommendationEngine(tHealthBeacon plugin) {
        this.plugin = plugin;
    }

    public List<Recommendation> generateRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();

        TpsAnalyzer tpsAnalyzer = plugin.getTpsAnalyzer();
        TpsAnalyzer.TpsTrend tpsTrend = tpsAnalyzer.analyzeTrend(10);
        
        if (tpsTrend.average < 18.0) {
            StringBuilder actions = new StringBuilder();
            actions.append("Конкретные действия:\n");
            actions.append("1. В paper.yml установите: max-entity-collisions: 2\n");
            actions.append("2. Уменьшите лимит мобов: max-entities-per-chunk: 16\n");
            actions.append("3. Отключите тики неактивных мобов: tick-inactive-villagers: false\n");
            actions.append("4. Уменьшите частоту автосохранения: delay-chunk-unloads-by: 10s\n");
            actions.append("5. Проверьте плагины командой: /timings report");
            
            recommendations.add(new Recommendation(
                "Низкий TPS",
                actions.toString(),
                Recommendation.Priority.HIGH,
                "tps-low"
            ));
        }

        MemoryMonitor memoryMonitor = plugin.getMemoryMonitor();
        MemoryMonitor.MemoryLeakAnalysis memoryLeak = memoryMonitor.analyzeMemoryLeak();
        
        if (memoryLeak.possibleLeak) {
            StringBuilder actions = new StringBuilder();
            actions.append("Обнаружена утечка памяти!\n");
            actions.append("Действия:\n");
            actions.append("1. Используйте /timings report для поиска проблемных плагинов\n");
            actions.append("2. Проверьте плагины, работающие с сущностями (Citizens, MythicMobs)\n");
            actions.append("3. Увеличьте частоту GC: -XX:+UseG1GC -XX:MaxGCPauseMillis=200\n");
            actions.append("4. Перезапустите сервер для очистки памяти\n");
            actions.append("5. Если проблема сохраняется, отключите подозрительные плагины");
            
            recommendations.add(new Recommendation(
                "Утечка памяти",
                actions.toString(),
                Recommendation.Priority.HIGH,
                "memory-leak"
            ));
        }
        
        MemoryMonitor.MemorySnapshot currentMemory = memoryMonitor.getCurrentSnapshot();
        double memoryUsagePercent = (currentMemory.used * 100.0) / currentMemory.max;
        if (memoryUsagePercent > 80) {
            long recommendedMemory = (long) (currentMemory.used * 1.5);
            StringBuilder actions = new StringBuilder();
            actions.append("Использование памяти: ").append(String.format("%.1f%%", memoryUsagePercent)).append("\n");
            actions.append("Действия:\n");
            actions.append("1. Увеличьте выделенную память до минимум ").append(FileUtil.formatFileSize(recommendedMemory)).append("\n");
            actions.append("2. В запуске сервера добавьте: -Xmx").append(recommendedMemory / (1024 * 1024)).append("M\n");
            actions.append("3. Оптимизируйте плагины: /timings report\n");
            actions.append("4. Уменьшите view-distance в server.properties: view-distance=6");
            
            recommendations.add(new Recommendation(
                "Высокое использование памяти",
                actions.toString(),
                Recommendation.Priority.MEDIUM,
                "memory-high"
            ));
        }

        DiskProbe diskProbe = plugin.getDiskProbe();
        DiskProbe.DiskLatencyAnalysis diskLatency = diskProbe.analyzeLatency();
        
        if (diskLatency.hasIssue) {
            StringBuilder actions = new StringBuilder();
            actions.append("Задержка диска: ").append(String.format("%.0f мс", (double) diskLatency.avgLatency)).append(" (порог: 2000 мс)\n");
            actions.append("Действия:\n");
            actions.append("1. В paper.yml установите: chunk-loading.async: true\n");
            actions.append("2. Увеличьте интервал автосохранения: auto-save-interval: 6000\n");
            actions.append("3. Отключите синхронное сохранение: save-player-data: false\n");
            actions.append("4. Используйте SSD вместо HDD для лучшей производительности\n");
            actions.append("5. Уменьшите количество миров или используйте отдельный диск");
            
            recommendations.add(new Recommendation(
                "Высокая задержка диска",
                actions.toString(),
                Recommendation.Priority.MEDIUM,
                "disk-latency"
            ));
        }

        ChunkScanner chunkScanner = plugin.getChunkScanner();
        Map<String, List<ChunkScanner.HotZone>> allHotZones = chunkScanner.getAllHotZones();
        
        int totalHotZones = allHotZones.values().stream()
            .mapToInt(List::size)
            .sum();
        
        if (totalHotZones > 0) {
            StringBuilder actions = new StringBuilder();
            actions.append("Обнаружено горячих зон: ").append(totalHotZones).append("\n");
            actions.append("Действия:\n");
            actions.append("1. В paper.yml: tick-inactive-villagers: false\n");
            actions.append("2. Уменьшите лимит мобов: max-entities-per-chunk: 16\n");
            actions.append("3. Используйте команду: /kill @e[type=!player,distance=..100]\n");
            actions.append("4. Установите плагин для очистки мобов (ClearLag)\n");
            actions.append("5. Проверьте фермы игроков на избыточное количество мобов");
            
            recommendations.add(new Recommendation(
                "Горячие зоны с большим количеством сущностей",
                actions.toString(),
                Recommendation.Priority.MEDIUM,
                "hot-zones"
            ));
        }

        PluginProfiler pluginProfiler = plugin.getPluginProfiler();
        List<PluginProfiler.PluginLoadInfo> topPlugins = pluginProfiler.getTopLoadTimes(5);
        
        if (!topPlugins.isEmpty() && topPlugins.get(0).loadTime > 5000 && topPlugins.get(0).loadTime < 3600000) {

            StringBuilder actions = new StringBuilder();
            actions.append("Самый медленный плагин: ").append(topPlugins.get(0).name).append("\n");
            actions.append("Время загрузки: ").append(TimeUtil.formatDuration(topPlugins.get(0).loadTime)).append("\n");
            actions.append("Действия:\n");
            actions.append("1. Проверьте обновления плагина: ").append(topPlugins.get(0).name).append("\n");
            actions.append("2. Используйте /timings report для детального анализа\n");
            actions.append("3. Рассмотрите замену на более оптимизированный аналог\n");
            actions.append("4. Отключите неиспользуемые функции плагина\n");
            actions.append("5. Проверьте зависимости плагина на конфликты");
            
            recommendations.add(new Recommendation(
                "Медленная загрузка плагинов",
                actions.toString(),
                Recommendation.Priority.LOW,
                "slow-plugins"
            ));
        }
        
        return recommendations;
    }

    public static class Recommendation {
        public enum Priority {
            HIGH, MEDIUM, LOW
        }
        
        public final String title;
        public final String description;
        public final Priority priority;
        public final String id;
        
        public Recommendation(String title, String description, Priority priority) {
            this(title, description, priority, null);
        }
        
        public Recommendation(String title, String description, Priority priority, String id) {
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.id = id != null ? id : title.toLowerCase().replace(" ", "-");
        }
    }
}

