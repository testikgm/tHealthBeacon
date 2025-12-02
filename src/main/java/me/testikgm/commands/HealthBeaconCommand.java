package me.testikgm.commands;

import me.testikgm.tHealthBeacon;
import me.testikgm.analysis.DiagnosisEngine;
import me.testikgm.analysis.HealthIndex;
import me.testikgm.analysis.RecommendationEngine;
import me.testikgm.metrics.MemoryMonitor;
import me.testikgm.metrics.NetworkMonitor;
import me.testikgm.report.ReportBuilder;
import me.testikgm.util.FileUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

public class HealthBeaconCommand implements CommandExecutor {
    
    private final tHealthBeacon plugin;
    
    public HealthBeaconCommand(tHealthBeacon plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String commandName = command.getName().toLowerCase();

        if (commandName.equals("hbreport") || commandName.equals("report")) {
            if (!sender.hasPermission("healthbeacon.report")) {
                sender.sendMessage("§cУ вас нет разрешения на генерацию отчётов.");
                return true;
            }
            generateReport(sender);
            return true;
        }

        if (!sender.hasPermission("healthbeacon.use")) {
            sender.sendMessage("§cУ вас нет разрешения на использование этой команды.");
            return true;
        }
        
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showStatus(sender);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("diagnosis")) {
            showDiagnosis(sender);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("report")) {
            if (!sender.hasPermission("healthbeacon.report")) {
                sender.sendMessage("§cУ вас нет разрешения на генерацию отчётов.");
                return true;
            }
            generateReport(sender);
            return true;
        }
        
        sender.sendMessage("§cИспользование: /healthbeacon [status|diagnosis|report]");
        return true;
    }
    
    private void showStatus(CommandSender sender) {
        sender.sendMessage("§8╔════════════════════════════════════════════════════════╗");
        sender.sendMessage("§8║ §6🧠 §ltHealthBeacon §r§8- §7Статус сервера §8                    ║");
        sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");

        double currentTps = plugin.getTpsAnalyzer().getCurrentTpsValue();
        String tpsColor = currentTps >= 19.5 ? "§a" : currentTps >= 18.0 ? "§e" : "§c";
        sender.sendMessage("§8║ §e🧩 TPS: " + tpsColor + String.format("%.2f", currentTps) + " §8/ §720.00 §8                      ║");

        MemoryMonitor.MemorySnapshot memory = plugin.getMemoryMonitor().getCurrentSnapshot();
        double memoryPercent = (memory.used * 100.0) / memory.max;
        String memoryColor = memoryPercent < 70 ? "§a" : memoryPercent < 85 ? "§e" : "§c";
        sender.sendMessage("§8║ §e🧠 Memory: " + memoryColor + String.format("%.1f%%", memoryPercent) + " §8(" + 
            FileUtil.formatFileSize(memory.used) + " / " + FileUtil.formatFileSize(memory.max) + ") §8   ║");

        NetworkMonitor.NetworkStabilityAnalysis network = plugin.getNetworkMonitor().analyzeStability();
        String networkColor = network.avgPing < 50 ? "§a" : network.avgPing < 100 ? "§e" : "§c";
        sender.sendMessage("§8║ §e🌐 Network: " + networkColor + String.format("%.0f мс", network.avgPing) + 
            " §7(джиттер: ±" + String.format("%.0f%%", network.jitterPercent) + ") §8              ║");

        HealthIndex.ServerResilienceScore srs = plugin.getHealthIndex().calculateScore();
        String srsColor = srs.score >= 80 ? "§a" : srs.score >= 60 ? "§e" : "§c";
        sender.sendMessage("§8║ §e📈 SRS: " + srsColor + srs.score + "/100 §8                              ║");
        
        sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
        sender.sendMessage("§8║ §7Используйте §e/healthbeacon diagnosis §7для подробного анализа §8║");
        sender.sendMessage("§8║ §7Используйте §e/hbreport §7для генерации HTML отчёта §8        ║");
        sender.sendMessage("§8╚════════════════════════════════════════════════════════╝");
    }
    
    private void showDiagnosis(CommandSender sender) {
        sender.sendMessage("§8╔════════════════════════════════════════════════════════╗");
        sender.sendMessage("§8║ §6✅ §ltHealthBeacon Diagnosis §r§8- §7Анализ сервера §8            ║");
        sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
        
        DiagnosisEngine.Diagnosis diagnosis = plugin.getDiagnosisEngine().analyze();
        sender.sendMessage("§8║ §f" + diagnosis.summary + " §8                              ║");
        sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
        
        if (!diagnosis.issues.isEmpty()) {
            sender.sendMessage("§8║ §c❌ §lПроблемы: §r§8                                      ║");
            for (String issue : diagnosis.issues) {
                String wrapped = wrapText(issue, 50);
                sender.sendMessage("§8║ §c  • " + wrapped + " §8                              ║");
            }
            sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
        }
        
        if (!diagnosis.warnings.isEmpty()) {
            sender.sendMessage("§8║ §e⚠ §lПредупреждения: §r§8                                ║");
            for (String warning : diagnosis.warnings) {
                String wrapped = wrapText(warning, 50);
                sender.sendMessage("§8║ §e  • " + wrapped + " §8                              ║");
            }
            sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
        }
        
        List<RecommendationEngine.Recommendation> recommendations = plugin.getRecommendationEngine().generateRecommendations();
        if (!recommendations.isEmpty()) {
            sender.sendMessage("§8║ §a💡 §lРекомендации: §r§8                                  ║");
            for (RecommendationEngine.Recommendation rec : recommendations) {
                String priorityColor = rec.priority == RecommendationEngine.Recommendation.Priority.HIGH ? "§c" :
                    rec.priority == RecommendationEngine.Recommendation.Priority.MEDIUM ? "§e" : "§a";
                sender.sendMessage("§8║ " + priorityColor + "  → " + rec.title + " §8                    ║");
                String wrapped = wrapText(rec.description, 48);
                sender.sendMessage("§8║ §7    " + wrapped + " §8                              ║");
            }
        }
        
        sender.sendMessage("§8╚════════════════════════════════════════════════════════╝");
    }
    
    private String wrapText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, Math.min(maxLength - 3, text.length())) + "...";
    }
    
    private void generateReport(CommandSender sender) {

        int snapshotCount = plugin.getMetricsHistory().getSnapshotCount();
        int minForReport = plugin.getConfig().getInt("collector.min_for_report", 10);
        
        if (snapshotCount < minForReport) {
            sender.sendMessage("§8╔════════════════════════════════════════════════════════╗");
            sender.sendMessage("§8║ §c⚠ §lНедостаточно данных для анализа §r§8                      ║");
            sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
            sender.sendMessage("§8║ §7Снимков: §f" + snapshotCount + "/" + minForReport + " §8                        ║");
            sender.sendMessage("§8║ §7Подождите ещё немного... §8                             ║");
            sender.sendMessage("§8╚════════════════════════════════════════════════════════╝");
            return;
        }
        
        sender.sendMessage("§8╔════════════════════════════════════════════════════════╗");
        sender.sendMessage("§8║ §6📊 §lГенерация AI отчёта... §r§8                            ║");
        sender.sendMessage("§8╚════════════════════════════════════════════════════════╝");
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                int hoursToLoad = 168;
                List<String> jsonSnapshots = plugin.getMetricsHistory().getJsonSnapshotStrings(hoursToLoad);

                if (jsonSnapshots.isEmpty()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§cНе удалось получить снимки для анализа");
                    });
                    return;
                }

                java.util.Map<String, String> aiAnalysisMap = new java.util.HashMap<>();

                if (!plugin.getOpenRouterClient().isEnabled()) {
                    plugin.getLogger().warning("OpenRouter AI анализ отключен или API ключ не настроен.");
                    plugin.getLogger().warning("Для использования AI анализа:");
                    plugin.getLogger().warning("1. Получите API ключ на https://openrouter.ai/");
                    plugin.getLogger().warning("2. Установите openrouter.api_key в config.yml");
                    plugin.getLogger().warning("3. Перезапустите сервер");
                } else if (!jsonSnapshots.isEmpty()) {
                    sender.sendMessage("§7[HealthBeacon] Анализ метрик с помощью AI (текущие + история)...");

                    java.util.List<String> recentSnapshots = jsonSnapshots;

                    java.util.Map<String, Object> currentMetrics = new java.util.HashMap<>();

                    double currentTps = plugin.getTpsAnalyzer().getCurrentTpsValue();
                    java.util.Map<String, Double> tpsTrends = new java.util.HashMap<>();
                    tpsTrends.put("1m", plugin.getTpsAnalyzer().analyzeTrend(1).average);
                    tpsTrends.put("5m", plugin.getTpsAnalyzer().analyzeTrend(5).average);
                    tpsTrends.put("15m", plugin.getTpsAnalyzer().analyzeTrend(15).average);
                    currentMetrics.put("tps", currentTps);
                    currentMetrics.put("tps_trends", tpsTrends);

                    me.testikgm.metrics.MsptMonitor.MsptStats msptStats = plugin.getMsptMonitor().getStats();
                    java.util.Map<String, Double> mspt = new java.util.HashMap<>();
                    mspt.put("min", msptStats.min);
                    mspt.put("median", msptStats.median);
                    mspt.put("p95", msptStats.p95);
                    mspt.put("max", msptStats.max);
                    currentMetrics.put("mspt", mspt);

                    me.testikgm.metrics.MemoryMonitor.MemorySnapshot memory = plugin.getMemoryMonitor().getCurrentSnapshot();
                    me.testikgm.metrics.MemoryMonitor.PhysicalMemoryInfo physicalMemory = plugin.getMemoryMonitor().getPhysicalMemoryInfo();
                    java.util.Map<String, Object> memoryData = new java.util.HashMap<>();
                    memoryData.put("used", memory.used);
                    memoryData.put("max", memory.max);
                    memoryData.put("percent", (memory.used * 100.0) / memory.max);
                    
                    java.util.Map<String, Object> physicalMemoryData = new java.util.HashMap<>();
                    physicalMemoryData.put("total", physicalMemory.totalPhysical);
                    physicalMemoryData.put("used", physicalMemory.usedPhysical);
                    physicalMemoryData.put("free", physicalMemory.freePhysical);
                    physicalMemoryData.put("percent", (physicalMemory.usedPhysical * 100.0) / physicalMemory.totalPhysical);
                    memoryData.put("physical", physicalMemoryData);
                    
                    java.util.Map<String, Object> swapData = new java.util.HashMap<>();
                    swapData.put("total", physicalMemory.totalSwap);
                    swapData.put("used", physicalMemory.usedSwap);
                    swapData.put("free", physicalMemory.freeSwap);
                    swapData.put("percent", physicalMemory.totalSwap > 0 ? 
                        (physicalMemory.usedSwap * 100.0) / physicalMemory.totalSwap : 0.0);
                    memoryData.put("swap", swapData);
                    currentMetrics.put("memory", memoryData);

                    me.testikgm.metrics.CpuMonitor.CpuStats cpuStats1m = plugin.getCpuMonitor().getStats(1);
                    me.testikgm.metrics.CpuMonitor.CpuStats cpuStats15m = plugin.getCpuMonitor().getStats(15);
                    java.util.Map<String, Object> cpuData = new java.util.HashMap<>();
                    cpuData.put("system_1m", cpuStats1m.systemAvg);
                    cpuData.put("system_15m", cpuStats15m.systemAvg);
                    cpuData.put("process_1m", cpuStats1m.processAvg);
                    cpuData.put("process_15m", cpuStats15m.processAvg);
                    currentMetrics.put("cpu", cpuData);

                    me.testikgm.metrics.MemoryMonitor.GcStats gcStats = plugin.getMemoryMonitor().getGcStats();
                    java.util.Map<String, Object> gcData = new java.util.HashMap<>();
                    java.util.Map<String, Object> youngGc = new java.util.HashMap<>();
                    youngGc.put("total", gcStats.youngGc.total);
                    youngGc.put("avg_time", gcStats.youngGc.avgTime);
                    youngGc.put("avg_freq", gcStats.youngGc.avgFreq);
                    java.util.Map<String, Object> oldGc = new java.util.HashMap<>();
                    oldGc.put("total", gcStats.oldGc.total);
                    oldGc.put("avg_time", gcStats.oldGc.avgTime);
                    oldGc.put("avg_freq", gcStats.oldGc.avgFreq);
                    gcData.put("young", youngGc);
                    gcData.put("old", oldGc);
                    currentMetrics.put("gc", gcData);

                    me.testikgm.metrics.NetworkMonitor.NetworkStabilityAnalysis network = plugin.getNetworkMonitor().analyzeStability();
                    java.util.Map<String, Object> networkData = new java.util.HashMap<>();
                    networkData.put("avgPing", network.avgPing);
                    networkData.put("jitter", network.jitterPercent);
                    currentMetrics.put("network", networkData);

                    try {
                        me.testikgm.metrics.DiskProbe.DiskSnapshot disk = plugin.getDiskProbe().getCurrentSnapshot();
                        java.util.Map<String, Object> diskData = new java.util.HashMap<>();
                        diskData.put("used", disk.usedSpace);
                        diskData.put("free", disk.freeSpace);
                        diskData.put("total", disk.usedSpace + disk.freeSpace);
                        diskData.put("percent", ((disk.usedSpace * 100.0) / (disk.usedSpace + disk.freeSpace)));
                        currentMetrics.put("disk", diskData);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Ошибка при получении данных диска для AI: " + e.getMessage());
                    }

                    java.util.List<java.util.Map<String, Object>> tpsHistory = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> memoryHistory = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> diskHistory = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> networkHistory = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> cpuHistory = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> gcHistory = new java.util.ArrayList<>();
                    
                    for (String snapshotJson : recentSnapshots) {

                        String tpsDataHist = extractMetricData(snapshotJson, "tps");
                        if (tpsDataHist != null) {
                            java.util.Map<String, Object> tpsPoint = new java.util.HashMap<>();
                            tpsPoint.put("data", tpsDataHist);
                            tpsHistory.add(tpsPoint);
                        }

                        String memoryDataHist = extractMetricData(snapshotJson, "memory");
                        if (memoryDataHist != null) {
                            java.util.Map<String, Object> memoryPoint = new java.util.HashMap<>();
                            memoryPoint.put("data", memoryDataHist);
                            memoryHistory.add(memoryPoint);
                        }

                        String diskDataHist = extractMetricData(snapshotJson, "disk");
                        if (diskDataHist != null) {
                            java.util.Map<String, Object> diskPoint = new java.util.HashMap<>();
                            diskPoint.put("data", diskDataHist);
                            diskHistory.add(diskPoint);
                        }

                        String networkDataHist = extractMetricData(snapshotJson, "network");
                        if (networkDataHist != null) {
                            java.util.Map<String, Object> networkPoint = new java.util.HashMap<>();
                            networkPoint.put("data", networkDataHist);
                            networkHistory.add(networkPoint);
                        }

                        String cpuDataHist = extractMetricData(snapshotJson, "cpu");
                        if (cpuDataHist != null) {
                            java.util.Map<String, Object> cpuPoint = new java.util.HashMap<>();
                            cpuPoint.put("data", cpuDataHist);
                            cpuHistory.add(cpuPoint);
                        }

                        String gcDataHist = extractMetricData(snapshotJson, "gc");
                        if (gcDataHist != null) {
                            java.util.Map<String, Object> gcPoint = new java.util.HashMap<>();
                            gcPoint.put("data", gcDataHist);
                            gcHistory.add(gcPoint);
                        }
                    }

                    java.util.List<java.util.Map<String, Object>> msptHistory = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> chunksHistory = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> pluginsHistory = new java.util.ArrayList<>();
                    
                    for (String snapshotJson : recentSnapshots) {
                        String msptDataHist = extractMetricData(snapshotJson, "mspt");
                        if (msptDataHist != null) {
                            java.util.Map<String, Object> msptPoint = new java.util.HashMap<>();
                            msptPoint.put("data", msptDataHist);
                            msptHistory.add(msptPoint);
                        }
                        
                        String chunksDataHist = extractMetricData(snapshotJson, "chunks");
                        if (chunksDataHist != null) {
                            java.util.Map<String, Object> chunksPoint = new java.util.HashMap<>();
                            chunksPoint.put("data", chunksDataHist);
                            chunksHistory.add(chunksPoint);
                        }
                        
                        String pluginsDataHist = extractMetricData(snapshotJson, "plugins");
                        if (pluginsDataHist != null) {
                            java.util.Map<String, Object> pluginsPoint = new java.util.HashMap<>();
                            pluginsPoint.put("data", pluginsDataHist);
                            pluginsHistory.add(pluginsPoint);
                        }
                    }

                    me.testikgm.metrics.ChunkScanner.WorldStats worldStats = plugin.getChunkScanner().getWorldStats();
                    java.util.Map<String, Object> chunksData = new java.util.HashMap<>();
                    chunksData.put("totalChunks", worldStats.chunkCountsByWorld.values().stream().mapToInt(Integer::intValue).sum());
                    chunksData.put("totalEntities", worldStats.entityCountsByType.values().stream().mapToInt(Integer::intValue).sum());
                    chunksData.put("chunksByWorld", worldStats.chunkCountsByWorld);
                    chunksData.put("entitiesByType", worldStats.entityCountsByType);
                    currentMetrics.put("chunks", chunksData);
                    
                    java.util.List<me.testikgm.metrics.PluginProfiler.PluginLoadInfo> topPlugins = plugin.getPluginProfiler().getTopLoadTimes(10);
                    java.util.List<me.testikgm.metrics.PluginProfiler.PluginIssue> pluginIssues = plugin.getPluginProfiler().analyzePluginIssues();
                    java.util.Map<String, Object> pluginsData = new java.util.HashMap<>();
                    java.util.List<java.util.Map<String, Object>> topPluginsList = new java.util.ArrayList<>();
                    for (me.testikgm.metrics.PluginProfiler.PluginLoadInfo pluginInfo : topPlugins) {
                        java.util.Map<String, Object> pluginMap = new java.util.HashMap<>();
                        pluginMap.put("name", pluginInfo.name);
                        pluginMap.put("loadTime", pluginInfo.loadTime);
                        topPluginsList.add(pluginMap);
                    }
                    pluginsData.put("topPlugins", topPluginsList);
                    java.util.List<java.util.Map<String, Object>> issuesList = new java.util.ArrayList<>();
                    for (me.testikgm.metrics.PluginProfiler.PluginIssue issue : pluginIssues) {
                        java.util.Map<String, Object> issueMap = new java.util.HashMap<>();
                        issueMap.put("pluginName", issue.pluginName);
                        issueMap.put("issueType", issue.issueType.toString());
                        issueMap.put("description", issue.description);
                        issuesList.add(issueMap);
                    }
                    pluginsData.put("issues", issuesList);
                    currentMetrics.put("plugins", pluginsData);

                    java.util.Map<String, java.util.List<java.util.Map<String, Object>>> historyMap = new java.util.HashMap<>();
                    historyMap.put("tps", tpsHistory);
                    historyMap.put("memory", memoryHistory);
                    historyMap.put("disk", diskHistory);
                    historyMap.put("network", networkHistory);
                    historyMap.put("cpu", cpuHistory);
                    historyMap.put("gc", gcHistory);
                    historyMap.put("mspt", msptHistory);
                    historyMap.put("chunks", chunksHistory);
                    historyMap.put("plugins", pluginsHistory);

                    try {
                        java.util.Map<String, String> allMetricsAnalysis = plugin.getOpenRouterClient().analyzeAllMetrics(currentMetrics, historyMap);
                        
                        if (allMetricsAnalysis != null && !allMetricsAnalysis.isEmpty()) {

                            for (java.util.Map.Entry<String, String> entry : allMetricsAnalysis.entrySet()) {
                                String metricName = entry.getKey();
                                String analysis = entry.getValue();
                                if (analysis != null && !analysis.trim().isEmpty()) {
                                    aiAnalysisMap.put(metricName, analysis.trim());
                                }
                            }
                        }
                    } catch (Exception e) {
                        String errorMsg = e.getMessage();
                        plugin.getLogger().warning("Ошибка при анализе всех метрик: " + errorMsg);
                        
                        if (errorMsg != null && (errorMsg.contains("429") || errorMsg.contains("Rate Limit"))) {
                            plugin.getLogger().warning("Получена ошибка 429 (Rate Limit). Рекомендации:");
                            plugin.getLogger().warning("1. Проверьте API ключ в config.yml (openrouter.api_key)");
                            plugin.getLogger().warning("2. Увеличьте задержку: openrouter.request_delay_seconds");
                            plugin.getLogger().warning("3. Проверьте лимиты вашего API ключа на https://openrouter.ai/");
                            plugin.getLogger().warning("4. Попробуйте позже или используйте платный API ключ");
                        }
                    }
                }

                ReportBuilder reportBuilder = plugin.getReportBuilder();
                File reportFile = reportBuilder.saveReport(aiAnalysisMap);
                
                final int analyzedMetrics = aiAnalysisMap.size();
                final int totalSnapshots = jsonSnapshots.size();
                plugin.getServer().getScheduler().runTask(plugin, () -> {

                    java.util.List<String> metricNames = new java.util.ArrayList<>();
                    if (aiAnalysisMap.containsKey("tps")) metricNames.add("TPS");
                    if (aiAnalysisMap.containsKey("memory")) metricNames.add("Memory");
                    if (aiAnalysisMap.containsKey("disk")) metricNames.add("Disk");
                    if (aiAnalysisMap.containsKey("network")) metricNames.add("Network");
                    if (aiAnalysisMap.containsKey("cpu")) metricNames.add("CPU");
                    if (aiAnalysisMap.containsKey("gc")) metricNames.add("GC");
                    if (aiAnalysisMap.containsKey("mspt")) metricNames.add("MSPT");
                    if (aiAnalysisMap.containsKey("chunks")) metricNames.add("Chunks");
                    if (aiAnalysisMap.containsKey("plugins")) metricNames.add("Plugins");
                    String metricsList = metricNames.isEmpty() ? "нет" : String.join(", ", metricNames);
                    
                    sender.sendMessage("§8╔════════════════════════════════════════════════════════╗");
                    sender.sendMessage("§8║ §a✓ §lОтчёт успешно создан! §r§8                              ║");
                    sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
                    sender.sendMessage("§8║ §7Файл: §f" + reportFile.getName() + " §8                    ║");
                    sender.sendMessage("§8║ §7Путь: §f" + reportFile.getParent() + " §8                ║");
                    sender.sendMessage("§8║ §7Использовано снимков: §f" + totalSnapshots + " §8                    ║");
                    if (analyzedMetrics > 0) {
                        sender.sendMessage("§8║ §a✓ AI анализ выполнен для " + analyzedMetrics + " метрик §8       ║");
                        sender.sendMessage("§8║ §7Метрики: §f" + metricsList + " §8                         ║");
                    } else {
                        sender.sendMessage("§8║ §e⚠ AI анализ недоступен (API ошибка или отключен) §8     ║");
                    }
                    if (sender instanceof Player) {
                        sender.sendMessage("§8║ §7Вы можете открыть файл в браузере для просмотра §8    ║");
                    }
                    sender.sendMessage("§8╚════════════════════════════════════════════════════════╝");
                });
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§8╔════════════════════════════════════════════════════════╗");
                    sender.sendMessage("§8║ §c✗ §lОшибка при создании отчёта §r§8                        ║");
                    sender.sendMessage("§8╠════════════════════════════════════════════════════════╣");
                    sender.sendMessage("§8║ §c" + e.getMessage() + " §8                              ║");
                    sender.sendMessage("§8╚════════════════════════════════════════════════════════╝");
                    plugin.getLogger().severe("Ошибка при создании отчёта: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }

    private String extractMetricData(String jsonSnapshot, String metricName) {
        try {
            int startIndex = jsonSnapshot.indexOf("\"" + metricName + "\":");
            if (startIndex == -1) {
                return null;
            }
            
            startIndex = jsonSnapshot.indexOf("{", startIndex);
            if (startIndex == -1) {
                return null;
            }

            int depth = 0;
            int endIndex = startIndex;
            for (int i = startIndex; i < jsonSnapshot.length(); i++) {
                char c = jsonSnapshot.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        endIndex = i + 1;
                        break;
                    }
                }
            }
            
            if (endIndex > startIndex) {
                return jsonSnapshot.substring(startIndex, endIndex);
            }
        } catch (Exception e) {

        }
        return null;
    }
}

