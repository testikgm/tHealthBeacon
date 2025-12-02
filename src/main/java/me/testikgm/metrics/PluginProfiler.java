package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluginProfiler {
    
    private final tHealthBeacon plugin;
    private final Map<String, PluginMetrics> pluginMetrics;
    private final Map<String, Long> pluginLoadTimes;
    private boolean running;
    
    public PluginProfiler(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.pluginMetrics = new ConcurrentHashMap<>();
        this.pluginLoadTimes = new HashMap<>();
        this.running = false;

        recordLoadTimes();
    }
    
    public void collect() {
        if (!running) return;

        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            String name = p.getName();
            PluginMetrics metrics = pluginMetrics.computeIfAbsent(name, k -> new PluginMetrics(name));

            metrics.isEnabled = p.isEnabled();

        }
    }
    
    public void start() {
        running = true;
    }
    
    public void stop() {
        running = false;
    }

    private void recordLoadTimes() {

        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {

            pluginLoadTimes.put(p.getName(), 0L);
        }
    }

    public List<PluginLoadInfo> getTopLoadTimes(int limit) {
        List<PluginLoadInfo> result = new ArrayList<>();
        
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            long loadTime = pluginLoadTimes.getOrDefault(p.getName(), 0L);

            if (loadTime > 3600000) {
                loadTime = 0L;
            }
            result.add(new PluginLoadInfo(p.getName(), loadTime, p.isEnabled()));
        }
        
        result.sort((a, b) -> Long.compare(b.loadTime, a.loadTime));

        result = result.stream()
            .filter(info -> info.loadTime > 0)
            .collect(java.util.stream.Collectors.toList());
        
        return result.subList(0, Math.min(limit, result.size()));
    }

    public PluginMetrics getPluginMetrics(String pluginName) {
        return pluginMetrics.get(pluginName);
    }

    public Map<String, PluginMetrics> getAllMetrics() {
        return new HashMap<>(pluginMetrics);
    }

    public List<PluginIssue> analyzePluginIssues() {
        List<PluginIssue> issues = new ArrayList<>();
        
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (!p.isEnabled()) continue;
            
            String name = p.getName();
            long loadTime = pluginLoadTimes.getOrDefault(name, 0L);

            if (loadTime > 5000) {
                issues.add(new PluginIssue(
                    name,
                    "Медленная загрузка",
                    "Плагин загружается более 5 секунд (" + (loadTime / 1000) + " сек)",
                    PluginIssue.Severity.MEDIUM
                ));
            }

            String version = p.getDescription().getVersion();
            if (version == null || version.isEmpty()) {
                issues.add(new PluginIssue(
                    name,
                    "Отсутствует версия",
                    "Плагин не имеет указанной версии, что может указывать на проблемы",
                    PluginIssue.Severity.LOW
                ));
            }

            List<String> depend = p.getDescription().getDepend();
            if (depend != null && !depend.isEmpty()) {
                for (String dep : depend) {
                    if (Bukkit.getPluginManager().getPlugin(dep) == null) {
                        issues.add(new PluginIssue(
                            name,
                            "Отсутствует зависимость",
                            "Плагин требует " + dep + ", но он не установлен",
                            PluginIssue.Severity.HIGH
                        ));
                    }
                }
            }
        }
        
        return issues;
    }

    public static class PluginMetrics {
        public final String name;
        public boolean isEnabled;
        public double tickUsage;
        public long totalTicks;
        
        public PluginMetrics(String name) {
            this.name = name;
            this.isEnabled = true;
            this.tickUsage = 0.0;
            this.totalTicks = 0;
        }
    }
    
    public static class PluginLoadInfo {
        public final String name;
        public final long loadTime;
        public final boolean enabled;
        
        public PluginLoadInfo(String name, long loadTime, boolean enabled) {
            this.name = name;
            this.loadTime = loadTime;
            this.enabled = enabled;
        }
    }

    public static class PluginIssue {
        public enum Severity {
            HIGH, MEDIUM, LOW
        }
        
        public final String pluginName;
        public final String issueType;
        public final String description;
        public final Severity severity;
        
        public PluginIssue(String pluginName, String issueType, String description, Severity severity) {
            this.pluginName = pluginName;
            this.issueType = issueType;
            this.description = description;
            this.severity = severity;
        }
    }
}

