package me.testikgm.report;

import me.testikgm.analysis.*;
import me.testikgm.metrics.*;
import me.testikgm.util.FileUtil;
import me.testikgm.util.MetricsHistory;
import me.testikgm.util.TimeUtil;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ReportHTML {
    
    public static String generate(
        TpsAnalyzer tpsAnalyzer,
        MemoryMonitor memoryMonitor,
        PluginProfiler pluginProfiler,
        ChunkScanner chunkScanner,
        DiskProbe diskProbe,
        NetworkMonitor networkMonitor,
        DiagnosisEngine.Diagnosis diagnosis,
        List<RecommendationEngine.Recommendation> recommendations,
        HealthIndex.ServerResilienceScore srs,
        MetricsHistory.MetricsSnapshot lastSnapshot,
        MetricsHistory.MetricsSnapshot previousSnapshot,
        List<MetricsHistory.MetricsSnapshot> allSnapshots,
        MetricsHistory metricsHistory,
        java.util.Map<String, String> aiAnalysisMap,
        MsptMonitor msptMonitor,
        CpuMonitor cpuMonitor,
        org.bukkit.plugin.Plugin plugin
    ) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>tHealthBeacon Report</title>\n");
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n");
        html.append("    <style>\n");
        html.append(getCSS());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        html.append("    <nav class=\"sidebar\">\n");
        html.append("        <div class=\"sidebar-header\">\n");
        html.append("            <h2>🧠 tHealthBeacon</h2>\n");
        html.append("        </div>\n");
        html.append("        <ul class=\"sidebar-menu\">\n");
        html.append("            <li><a href=\"#overview\" class=\"nav-link active\">📊 Обзор</a></li>\n");
        html.append("            <li><a href=\"#srs\" class=\"nav-link\">📈 SRS</a></li>\n");
        html.append("            <li><a href=\"#tps\" class=\"nav-link\">🧩 TPS</a></li>\n");
        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("mspt")) {
            html.append("            <li><a href=\"#mspt\" class=\"nav-link\">⏱️ MSPT</a></li>\n");
        }
        html.append("            <li><a href=\"#memory\" class=\"nav-link\">🧠 Memory</a></li>\n");
        html.append("            <li><a href=\"#network\" class=\"nav-link\">🌐 Network</a></li>\n");
        html.append("            <li><a href=\"#plugins\" class=\"nav-link\">📦 Plugins</a></li>\n");
        html.append("            <li><a href=\"#disk\" class=\"nav-link\">💾 Disk</a></li>\n");
        html.append("            <li><a href=\"#chunks\" class=\"nav-link\">🔥 Chunks</a></li>\n");
        html.append("            <li><a href=\"#diagnosis\" class=\"nav-link\">✅ Diagnosis</a></li>\n");
        html.append("            <li><a href=\"#recommendations\" class=\"nav-link\">🩺 Recommendations</a></li>\n");
        if (lastSnapshot != null) {
            html.append("            <li><a href=\"#history\" class=\"nav-link\">📜 History</a></li>\n");
        }
        html.append("        </ul>\n");
        html.append("    </nav>\n");

        html.append("    <div class=\"main-content\">\n");

        html.append("        <div class=\"header\">\n");
        html.append("            <div class=\"header-content\">\n");
        html.append("                <h1>⚡ tHealthBeacon Viewer</h1>\n");
        html.append("                <p class=\"profile-info\">Profile created at ").append(TimeUtil.getCurrentTimeString()).append(", interval 5min</p>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        html.append(generateTopMetricsPanel(tpsAnalyzer, msptMonitor, cpuMonitor, memoryMonitor, diskProbe, networkMonitor, aiAnalysisMap));

        html.append(generateTabs(pluginProfiler, chunkScanner));

        html.append("        <div id=\"overview\" class=\"section\">\n");
        html.append("            <h2>📊 Обзор сервера</h2>\n");
        html.append("            <div class=\"info-grid\">\n");
        html.append("                <div class=\"info-card clickable\" data-modal=\"server-info\">\n");
        html.append("                    <div class=\"info-label\">Minecraft</div>\n");
        html.append("                    <div class=\"info-value\">").append(Bukkit.getVersion()).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"info-card clickable\" data-modal=\"plugins-info\">\n");
        html.append("                    <div class=\"info-label\">Плагины</div>\n");
        html.append("                    <div class=\"info-value\">").append(Bukkit.getPluginManager().getPlugins().length).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"info-card clickable\" data-modal=\"uptime-info\">\n");
        html.append("                    <div class=\"info-label\">Время работы</div>\n");
        html.append("                    <div class=\"info-value\">").append(TimeUtil.formatDuration(System.currentTimeMillis() - getServerStartTime())).append("</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        int reportSrs = lastSnapshot != null ? lastSnapshot.srs : srs.score;
        String srsColor = reportSrs >= 80 ? "#4ade80" : reportSrs >= 60 ? "#fbbf24" : "#f87171";
        html.append("        <div id=\"srs\" class=\"section\">\n");
        html.append("            <h2>📈 Индекс устойчивости сервера (SRS)</h2>\n");
        html.append("            <div class=\"srs-score clickable\" data-modal=\"srs-info\">\n");
        html.append("                <div class=\"score-value\" style=\"color: ").append(srsColor).append("\">").append(reportSrs).append("/100</div>\n");
        html.append("                <div class=\"score-bar\">\n");
        html.append("                    <div class=\"score-fill\" style=\"width: ").append(reportSrs).append("%; background: ").append(srsColor).append("\"></div>\n");
        html.append("                </div>\n");
        html.append("                <p class=\"score-interpretation\">").append(srs.interpretation.replace("\n", "<br>")).append("</p>\n");
        html.append("            </div>\n");

        if (previousSnapshot != null) {
            int srsDiff = reportSrs - previousSnapshot.srs;
            String srsDiffColor = srsDiff > 0 ? "#4ade80" : srsDiff < 0 ? "#f87171" : "#94a3b8";
            html.append("            <div class=\"comparison\">\n");
            html.append("                <p><strong>Изменение:</strong> <span style=\"color: ").append(srsDiffColor).append("\">");
            html.append(srsDiff > 0 ? "+" : "").append(srsDiff).append("</span> (было: ").append(previousSnapshot.srs).append(")</p>\n");
            html.append("            </div>\n");
        }
        html.append("        </div>\n");

        double avgTps = allSnapshots.stream().mapToDouble(s -> s.tps).average().orElse(20.0);
        double minTps = allSnapshots.stream().mapToDouble(s -> s.tps).min().orElse(20.0);
        double maxTps = allSnapshots.stream().mapToDouble(s -> s.tps).max().orElse(20.0);
        double currentTps = lastSnapshot != null ? lastSnapshot.tps : avgTps;

        double variance = allSnapshots.stream()
            .mapToDouble(s -> Math.pow(s.tps - avgTps, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        String stability = stdDev < 0.1 ? "Очень стабильный" : stdDev < 0.5 ? "Стабильный" : stdDev < 1.0 ? "Умеренно стабильный" : "Нестабильный";
        
        html.append("        <div id=\"tps\" class=\"section\">\n");
        html.append("            <h2>🧩 TPS Trend (на основе ").append(allSnapshots.size()).append(" снимков)</h2>\n");
        html.append("            <div class=\"metric-grid\">\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"tps-info\">\n");
        html.append("                    <div class=\"metric-label\">Текущий TPS</div>\n");
        html.append("                    <div class=\"metric-value\">").append(String.format("%.2f", currentTps)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"tps-info\">\n");
        html.append("                    <div class=\"metric-label\">Средний TPS</div>\n");
        html.append("                    <div class=\"metric-value\">").append(String.format("%.2f", avgTps)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"tps-info\">\n");
        html.append("                    <div class=\"metric-label\">Минимальный</div>\n");
        html.append("                    <div class=\"metric-value\">").append(String.format("%.2f", minTps)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"tps-info\">\n");
        html.append("                    <div class=\"metric-label\">Максимальный</div>\n");
        html.append("                    <div class=\"metric-value\">").append(String.format("%.2f", maxTps)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"tps-info\">\n");
        html.append("                    <div class=\"metric-label\">Стабильность</div>\n");
        html.append("                    <div class=\"metric-value\">").append(stability).append("</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        if (previousSnapshot != null) {
            double tpsDiff = currentTps - previousSnapshot.tps;
            String tpsDiffColor = tpsDiff > 0 ? "#4ade80" : tpsDiff < 0 ? "#f87171" : "#94a3b8";
            html.append("            <div class=\"comparison\">\n");
            html.append("                <p><strong>Изменение TPS:</strong> <span style=\"color: ").append(tpsDiffColor).append("\">");
            html.append(tpsDiff > 0 ? "+" : "").append(String.format("%.2f", tpsDiff)).append("</span> (было: ").append(String.format("%.2f", previousSnapshot.tps)).append(")</p>\n");
            html.append("            </div>\n");
        }

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("tps")) {
            String tpsAnalysis = aiAnalysisMap.get("tps");
            html.append("            <div class=\"metric-analysis\">\n");
            html.append("                <p class=\"analysis-text\">").append(escapeHtml(tpsAnalysis)).append("</p>\n");
            html.append("            </div>\n");
        }
        html.append("            <div class=\"chart-container\">\n");
        html.append("                <canvas id=\"tpsChart\"></canvas>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("mspt")) {
            MsptMonitor.MsptStats msptStats = msptMonitor.getStats();
            html.append("        <div id=\"mspt\" class=\"section\">\n");
            html.append("            <h2>⏱️ MSPT (Milliseconds Per Tick)</h2>\n");
            html.append("            <div class=\"metric-grid\">\n");
            html.append("                <div class=\"metric-card clickable\" data-modal=\"mspt-info\">\n");
            html.append("                    <div class=\"metric-label\">Минимальный</div>\n");
            html.append("                    <div class=\"metric-value\">").append(String.format("%.2f мс", msptStats.min)).append("</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"metric-card clickable\" data-modal=\"mspt-info\">\n");
            html.append("                    <div class=\"metric-label\">Медиана</div>\n");
            html.append("                    <div class=\"metric-value\">").append(String.format("%.2f мс", msptStats.median)).append("</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"metric-card clickable\" data-modal=\"mspt-info\">\n");
            html.append("                    <div class=\"metric-label\">95-й процентиль</div>\n");
            html.append("                    <div class=\"metric-value\">").append(String.format("%.2f мс", msptStats.p95)).append("</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"metric-card clickable\" data-modal=\"mspt-info\">\n");
            html.append("                    <div class=\"metric-label\">Максимальный</div>\n");
            html.append("                    <div class=\"metric-value\">").append(String.format("%.2f мс", msptStats.max)).append("</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");

            String msptAnalysis = aiAnalysisMap.get("mspt");
            html.append("            <div class=\"metric-analysis\">\n");
            html.append("                <p class=\"analysis-text\">").append(escapeHtml(msptAnalysis)).append("</p>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
        }

        long memoryUsed = lastSnapshot != null ? lastSnapshot.memoryUsed : memoryMonitor.getCurrentSnapshot().used;
        long memoryMax = lastSnapshot != null ? lastSnapshot.memoryMax : memoryMonitor.getCurrentSnapshot().max;
        double memoryPercent = lastSnapshot != null ? lastSnapshot.memoryPercent : ((memoryUsed * 100.0) / memoryMax);

        MemoryMonitor.MemoryLeakAnalysis memoryLeak = analyzeMemoryLeakFromSnapshots(allSnapshots);

        boolean diagnosisHasLeak = diagnosis.issues.stream()
            .anyMatch(issue -> issue.contains("утечка памяти") || issue.contains("Утечка памяти"));
        if (diagnosisHasLeak && !memoryLeak.possibleLeak) {

            String diagnosisLeakMessage = diagnosis.issues.stream()
                .filter(issue -> issue.contains("утечка памяти") || issue.contains("Утечка памяти"))
                .findFirst()
                .orElse("Обнаружена возможная утечка памяти (на основе текущих метрик)");
            memoryLeak = new MemoryMonitor.MemoryLeakAnalysis(true, 0.0, diagnosisLeakMessage);
        }
        
        html.append("        <div id=\"memory\" class=\"section\">\n");
        html.append("            <h2>🧠 Memory Trend (на основе ").append(allSnapshots.size()).append(" снимков)</h2>\n");
        html.append("            <div class=\"metric-grid\">\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"memory-info\">\n");
        html.append("                    <div class=\"metric-label\">Использовано</div>\n");
        html.append("                    <div class=\"metric-value\">").append(FileUtil.formatFileSize(memoryUsed)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"memory-info\">\n");
        html.append("                    <div class=\"metric-label\">Максимум</div>\n");
        html.append("                    <div class=\"metric-value\">").append(FileUtil.formatFileSize(memoryMax)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"memory-info\">\n");
        html.append("                    <div class=\"metric-label\">Использование</div>\n");
        html.append("                    <div class=\"metric-value\">").append(String.format("%.1f%%", memoryPercent)).append("</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        if (previousSnapshot != null) {
            double memoryDiff = memoryPercent - previousSnapshot.memoryPercent;
            String memoryDiffColor = memoryDiff < 0 ? "#4ade80" : memoryDiff > 5 ? "#f87171" : "#94a3b8";
            html.append("            <div class=\"comparison\">\n");
            html.append("                <p><strong>Изменение памяти:</strong> <span style=\"color: ").append(memoryDiffColor).append("\">");
            html.append(memoryDiff > 0 ? "+" : "").append(String.format("%.1f%%", memoryDiff)).append("</span> (было: ").append(String.format("%.1f%%", previousSnapshot.memoryPercent)).append(")</p>\n");
            html.append("            </div>\n");
        }

        if (memoryLeak.possibleLeak) {
            html.append("            <div class=\"alert alert-warning\" style=\"border-left: 4px solid #f87171; background: rgba(248, 113, 113, 0.1);\">\n");
            html.append("                <strong style=\"color: #f87171; font-size: 1.1em;\">⚠⚠⚠ ОБНАРУЖЕНА УТЕЧКА ПАМЯТИ ⚠⚠⚠</strong><br>\n");
            html.append("                <span style=\"margin-top: 8px; display: block;\">").append(memoryLeak.diagnosis).append("</span>\n");
            html.append("            </div>\n");
        } else {
            html.append("            <div class=\"alert alert-info\">\n");
            html.append("                <strong>📊 Анализ памяти:</strong> ").append(memoryLeak.diagnosis).append("\n");
            html.append("            </div>\n");
        }

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("memory")) {
            String memoryAnalysis = aiAnalysisMap.get("memory");
            html.append("            <div class=\"metric-analysis\">\n");
            html.append("                <p class=\"analysis-text\">").append(escapeHtml(memoryAnalysis)).append("</p>\n");
            html.append("            </div>\n");
        }
        html.append("            <div class=\"chart-container\">\n");
        html.append("                <canvas id=\"memoryChart\"></canvas>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        NetworkMonitor.NetworkStabilityAnalysis networkStability = networkMonitor.analyzeStability();
        html.append("        <div id=\"network\" class=\"section\">\n");
        html.append("            <h2>🌐 Network</h2>\n");
        html.append("            <div class=\"metric-grid\">\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"network-info\">\n");
        html.append("                    <div class=\"metric-label\">Средний пинг</div>\n");
        html.append("                    <div class=\"metric-value\">").append(String.format("%.0f мс", networkStability.avgPing)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"network-info\">\n");
        html.append("                    <div class=\"metric-label\">Джиттер</div>\n");
        html.append("                    <div class=\"metric-value\">±").append(String.format("%.0f%%", networkStability.jitterPercent)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"network-info\">\n");
        html.append("                    <div class=\"metric-label\">Статус</div>\n");
        html.append("                    <div class=\"metric-value\">").append(networkStability.hasIssue ? "⚠ Нестабильно" : "✓ Стабильно").append("</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("network")) {
            String networkAnalysis = aiAnalysisMap.get("network");
            html.append("            <div class=\"metric-analysis\">\n");
            html.append("                <p class=\"analysis-text\">").append(escapeHtml(networkAnalysis)).append("</p>\n");
            html.append("            </div>\n");
        }
        html.append("        </div>\n");

        List<PluginProfiler.PluginLoadInfo> topPlugins = pluginProfiler.getTopLoadTimes(10);
        List<PluginProfiler.PluginIssue> pluginIssues = pluginProfiler.analyzePluginIssues();
        html.append("        <div id=\"plugins\" class=\"section\">\n");
        html.append("            <h2>📦 Plugins Impact</h2>\n");
        if (topPlugins.isEmpty()) {
            html.append("            <div class=\"alert alert-info\">Информация о времени загрузки плагинов недоступна. Используйте Paper для детальной профилировки.</div>\n");
        } else {
            html.append("            <div class=\"plugins-list\">\n");
            for (int i = 0; i < topPlugins.size(); i++) {
                PluginProfiler.PluginLoadInfo pluginInfo = topPlugins.get(i);

                String loadTimeStr = pluginInfo.loadTime > 0 ? TimeUtil.formatDuration(pluginInfo.loadTime) : "N/A";
                html.append("                <div class=\"plugin-item\">\n");
                html.append("                    <div class=\"plugin-rank\">").append(i + 1).append("</div>\n");
                html.append("                    <div class=\"plugin-info\">\n");
                html.append("                        <div class=\"plugin-name\">").append(pluginInfo.name).append("</div>\n");
                html.append("                        <div class=\"plugin-time\">").append(loadTimeStr).append("</div>\n");
                html.append("                    </div>\n");
                html.append("                </div>\n");
            }
            html.append("            </div>\n");
        }
        if (!pluginIssues.isEmpty()) {
            html.append("            <div class=\"plugin-issues\">\n");
            html.append("                <h3>⚠ Проблемы плагинов</h3>\n");
            for (PluginProfiler.PluginIssue issue : pluginIssues) {
                String severityColor = issue.severity == PluginProfiler.PluginIssue.Severity.HIGH ? "#f87171" :
                    issue.severity == PluginProfiler.PluginIssue.Severity.MEDIUM ? "#fbbf24" : "#94a3b8";
                html.append("                <div class=\"plugin-issue-item\" style=\"border-left-color: ").append(severityColor).append("\">\n");
                html.append("                    <div class=\"plugin-issue-title\"><strong>").append(issue.pluginName).append("</strong> - ").append(issue.issueType).append("</div>\n");
                html.append("                    <div class=\"plugin-issue-description\">").append(issue.description).append("</div>\n");
                html.append("                </div>\n");
            }
            html.append("            </div>\n");
        }
        html.append("        </div>\n");

        DiskProbe.DiskSnapshot diskSnapshot = diskProbe.getCurrentSnapshot();
        DiskProbe.DiskLatencyAnalysis diskLatency = diskProbe.analyzeLatency();
        html.append("        <div id=\"disk\" class=\"section\">\n");
        html.append("            <h2>💾 Disk</h2>\n");
        html.append("            <div class=\"metric-grid\">\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"disk-info\">\n");
        html.append("                    <div class=\"metric-label\">Использовано</div>\n");
        html.append("                    <div class=\"metric-value\">").append(FileUtil.formatFileSize(diskSnapshot.usedSpace)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"disk-info\">\n");
        html.append("                    <div class=\"metric-label\">Свободно</div>\n");
        html.append("                    <div class=\"metric-value\">").append(FileUtil.formatFileSize(diskSnapshot.freeSpace)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"disk-info\">\n");
        html.append("                    <div class=\"metric-label\">Задержка</div>\n");
        html.append("                    <div class=\"metric-value\">").append(String.format("%.2f мс", (double) diskLatency.avgLatency)).append("</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        if (diskLatency.hasIssue) {
            html.append("            <div class=\"alert alert-warning\">\n");
            html.append("                <strong>⚠ Проблема:</strong> ").append(diskLatency.diagnosis).append("\n");
            html.append("            </div>\n");
        }

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("disk")) {
            String diskAnalysis = aiAnalysisMap.get("disk");
            html.append("            <div class=\"metric-analysis\">\n");
            html.append("                <p class=\"analysis-text\">").append(escapeHtml(diskAnalysis)).append("</p>\n");
            html.append("            </div>\n");
        }
        html.append("        </div>\n");

        ChunkScanner.WorldStats worldStats = chunkScanner.getWorldStats();
        Map<String, List<ChunkScanner.HotZone>> allHotZones = chunkScanner.getAllHotZones();
        int totalChunks = worldStats.chunkCountsByWorld.values().stream().mapToInt(Integer::intValue).sum();
        int totalEntities = worldStats.entityCountsByType.values().stream().mapToInt(Integer::intValue).sum();
        
        html.append("        <div id=\"chunks\" class=\"section\">\n");
        html.append("            <h2>🔥 Chunks & Hot Zones</h2>\n");

        html.append("            <div class=\"metric-grid\">\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"chunks-info\">\n");
        html.append("                    <div class=\"metric-label\">Всего чанков</div>\n");
        html.append("                    <div class=\"metric-value\">").append(totalChunks).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"chunks-info\">\n");
        html.append("                    <div class=\"metric-label\">Всего сущностей</div>\n");
        html.append("                    <div class=\"metric-value\">").append(totalEntities).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"chunks-info\">\n");
        html.append("                    <div class=\"metric-label\">Горячих зон</div>\n");
        int hotZonesCount = allHotZones.values().stream().mapToInt(List::size).sum();
        html.append("                    <div class=\"metric-value\">").append(hotZonesCount).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"metric-card clickable\" data-modal=\"chunks-info\">\n");
        html.append("                    <div class=\"metric-label\">Миров</div>\n");
        html.append("                    <div class=\"metric-value\">").append(worldStats.chunkCountsByWorld.size()).append("</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        if (!worldStats.chunkCountsByWorld.isEmpty()) {
            html.append("            <h3>Статистика по мирам</h3>\n");
            html.append("            <div class=\"chunks-by-world\">\n");
            for (Map.Entry<String, Integer> entry : worldStats.chunkCountsByWorld.entrySet()) {
                String worldName = entry.getKey();
                int chunks = entry.getValue();
                int entities = worldStats.entitiesByWorld.getOrDefault(worldName, new java.util.ArrayList<>()).size();
                html.append("                <div class=\"world-stat-item\">\n");
                html.append("                    <div class=\"world-name\">").append(worldName).append("</div>\n");
                html.append("                    <div class=\"world-stats\">").append(chunks).append(" чанков, ").append(entities).append(" сущностей</div>\n");
                html.append("                </div>\n");
            }
            html.append("            </div>\n");
        }

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("chunks")) {
            String chunksAnalysis = aiAnalysisMap.get("chunks");
            html.append("            <div class=\"metric-analysis\">\n");
            html.append("                <p class=\"analysis-text\">").append(escapeHtml(chunksAnalysis)).append("</p>\n");
            html.append("            </div>\n");
        }

        if (!allHotZones.isEmpty() && !allHotZones.values().stream().allMatch(List::isEmpty)) {
            html.append("            <h3>🔥 Проблемные зоны (Hot Zones)</h3>\n");
            html.append("            <div class=\"alert alert-warning\">\n");

            html.append("                <strong>⚠ Внимание:</strong> Обнаружены чанки с большим количеством сущностей (>500), что может вызывать лаги.\n");
            html.append("            </div>\n");
            html.append("            <div class=\"hot-zones-list\">\n");
            for (Map.Entry<String, List<ChunkScanner.HotZone>> entry : allHotZones.entrySet()) {
                String worldName = entry.getKey();
                for (ChunkScanner.HotZone zone : entry.getValue()) {
                    html.append("                <div class=\"hot-zone-item\">\n");
                    html.append("                    <div class=\"hot-zone-location\"><strong>").append(worldName).append("</strong> ").append(zone.getLocationString()).append("</div>\n");
                    html.append("                    <div class=\"hot-zone-entities\">").append(zone.entityCount).append(" сущностей</div>\n");
                    html.append("                </div>\n");
                }
            }
            html.append("            </div>\n");
        } else {
            html.append("            <div class=\"alert alert-info\">Горячих зон не обнаружено — все чанки в норме</div>\n");
        }
        html.append("        </div>\n");

        html.append("        <div id=\"diagnosis\" class=\"section\">\n");
        html.append("            <h2>✅ Diagnosis</h2>\n");
        html.append("            <div class=\"diagnosis-box\">\n");
        html.append("                <p class=\"diagnosis-summary\">").append(diagnosis.summary).append("</p>\n");
        if (!diagnosis.issues.isEmpty()) {
            html.append("                <div class=\"issues-section\">\n");
            html.append("                    <h3>❌ Проблемы</h3>\n");
            html.append("                    <ul class=\"issues-list\">\n");
            for (String issue : diagnosis.issues) {
                html.append("                        <li class=\"issue-item\">").append(issue).append("</li>\n");
            }
            html.append("                    </ul>\n");
            html.append("                </div>\n");
        }
        if (!diagnosis.warnings.isEmpty()) {
            html.append("                <div class=\"warnings-section\">\n");
            html.append("                    <h3>⚠ Предупреждения</h3>\n");
            html.append("                    <ul class=\"warnings-list\">\n");
            for (String warning : diagnosis.warnings) {
                html.append("                        <li class=\"warning-item\">").append(warning).append("</li>\n");
            }
            html.append("                    </ul>\n");
            html.append("                </div>\n");
        }
        html.append("            </div>\n");
        html.append("        </div>\n");

        html.append("        <div id=\"recommendations\" class=\"section\">\n");
        html.append("            <h2>🩺 Recommendations</h2>\n");
        html.append("            <div class=\"recommendations-box\">\n");
        if (recommendations.isEmpty()) {
            html.append("                <div class=\"alert alert-success\">Рекомендаций нет, сервер работает оптимально.</div>\n");
        } else {
            for (RecommendationEngine.Recommendation rec : recommendations) {
                String priorityClass = "priority-" + rec.priority.name().toLowerCase();
                html.append("                <div class=\"recommendation-item ").append(priorityClass).append("\" id=\"").append(rec.id).append("\">\n");
                html.append("                    <div class=\"recommendation-title\">").append(rec.title).append("</div>\n");
                html.append("                    <div class=\"recommendation-description\">").append(rec.description.replace("\n", "<br>")).append("</div>\n");
                html.append("                </div>\n");
            }
        }
        html.append("            </div>\n");
        html.append("        </div>\n");

        if (!allSnapshots.isEmpty() && metricsHistory != null) {
            html.append("        <div id=\"history\" class=\"section\">\n");
            html.append("            <h2>📜 История метрик (").append(allSnapshots.size()).append(" снимков)</h2>\n");
            html.append("            <div class=\"history-chart-container\">\n");
            html.append("                <canvas id=\"historyChart\"></canvas>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
        }
        
        html.append("    </div>\n");

        Queue<Double> tpsHistoryForChart = new java.util.LinkedList<>();
        for (MetricsHistory.MetricsSnapshot snapshot : allSnapshots) {
            tpsHistoryForChart.offer(snapshot.tps);
        }

        MemoryMonitor.MemorySnapshot memorySnapshot = new MemoryMonitor.MemorySnapshot(
            lastSnapshot != null ? lastSnapshot.timestamp : System.currentTimeMillis(),
            memoryUsed,
            memoryMax,
            memoryMax,
            0
        );

        TpsAnalyzer.TpsTrend tpsTrendForModals = new TpsAnalyzer.TpsTrend(
            avgTps, minTps, maxTps, stdDev, stability
        );
        html.append(getModals(tpsTrendForModals, memorySnapshot, memoryLeak, memoryPercent, networkStability, 
            diskSnapshot, diskLatency, srs, diagnosis, recommendations, pluginProfiler, chunkScanner));

        html.append("    <script>\n");
        html.append(getChartScript(tpsHistoryForChart, memorySnapshot, memoryPercent, lastSnapshot, allSnapshots, metricsHistory));
        html.append(getAnimationScript());
        html.append(getModalScript());
        html.append(getNavigationScript());
        html.append(getTabScript());
        html.append("    </script>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
    
    private static String getChartScript(Queue<Double> tpsHistory, MemoryMonitor.MemorySnapshot memory, 
                                         double memoryPercent, MetricsHistory.MetricsSnapshot lastSnapshot,
                                         List<MetricsHistory.MetricsSnapshot> allSnapshots,
                                         MetricsHistory metricsHistory) {
        StringBuilder script = new StringBuilder();

        script.append("        const tpsData = [");
        if (!tpsHistory.isEmpty()) {
            List<Double> tpsList = new java.util.ArrayList<>(tpsHistory);
            for (int i = 0; i < tpsList.size(); i++) {
                if (i > 0) script.append(", ");
                script.append(tpsList.get(i));
            }
        } else {
            script.append("20.0, 20.0, 20.0, 20.0, 20.0");
        }
        script.append("];\n");
        script.append("        new Chart(document.getElementById('tpsChart'), {\n");
        script.append("            type: 'line',\n");
        script.append("            data: {\n");
        script.append("                labels: tpsData.map((_, i) => i),\n");
        script.append("                datasets: [{\n");
        script.append("                    label: 'TPS',\n");
        script.append("                    data: tpsData,\n");
        script.append("                    borderColor: '#8b5cf6',\n");
        script.append("                    backgroundColor: 'rgba(139, 92, 246, 0.1)',\n");
        script.append("                    tension: 0.4,\n");
        script.append("                    fill: true\n");
        script.append("                }]\n");
        script.append("            },\n");
        script.append("            options: {\n");
        script.append("                responsive: true,\n");
        script.append("                maintainAspectRatio: false,\n");
        script.append("                animation: {\n");
        script.append("                    duration: 2000,\n");
        script.append("                    easing: 'easeInOutQuart'\n");
        script.append("                },\n");
        script.append("                plugins: {\n");
        script.append("                    legend: { display: false },\n");
        script.append("                    tooltip: { \n");
        script.append("                        mode: 'index', \n");
        script.append("                        intersect: false,\n");
        script.append("                        backgroundColor: 'rgba(30, 41, 59, 0.9)',\n");
        script.append("                        titleColor: '#8b5cf6',\n");
        script.append("                        bodyColor: '#e2e8f0',\n");
        script.append("                        borderColor: '#8b5cf6',\n");
        script.append("                        borderWidth: 1\n");
        script.append("                    }\n");
        script.append("                },\n");
        script.append("                scales: {\n");
        script.append("                    y: { beginAtZero: false, min: 0, max: 20, ticks: { color: '#9ca3af' }, grid: { color: '#374151' } },\n");
        script.append("                    x: { ticks: { color: '#9ca3af' }, grid: { color: '#374151' } }\n");
        script.append("                }\n");
        script.append("            }\n");
        script.append("        });\n");

        script.append("        new Chart(document.getElementById('memoryChart'), {\n");
        script.append("            type: 'doughnut',\n");
        script.append("            data: {\n");
        script.append("                labels: ['Использовано', 'Свободно'],\n");
        script.append("                datasets: [{\n");
        script.append("                    data: [").append(memoryPercent).append(", ").append(100 - memoryPercent).append("],\n");
        script.append("                    backgroundColor: ['#8b5cf6', '#374151'],\n");
        script.append("                    borderWidth: 0\n");
        script.append("                }]\n");
        script.append("            },\n");
        script.append("            options: {\n");
        script.append("                responsive: true,\n");
        script.append("                maintainAspectRatio: false,\n");
        script.append("                animation: {\n");
        script.append("                    duration: 2000,\n");
        script.append("                    easing: 'easeInOutQuart',\n");
        script.append("                    animateRotate: true,\n");
        script.append("                    animateScale: true\n");
        script.append("                },\n");
        script.append("                plugins: {\n");
        script.append("                    legend: { \n");
        script.append("                        position: 'bottom', \n");
        script.append("                        labels: { \n");
        script.append("                            color: '#9ca3af', \n");
        script.append("                            padding: 15,\n");
        script.append("                            font: { size: 14 }\n");
        script.append("                        } \n");
        script.append("                    },\n");
        script.append("                    tooltip: { \n");
        script.append("                        backgroundColor: 'rgba(30, 41, 59, 0.9)',\n");
        script.append("                        titleColor: '#8b5cf6',\n");
        script.append("                        bodyColor: '#e2e8f0',\n");
        script.append("                        borderColor: '#8b5cf6',\n");
        script.append("                        borderWidth: 1,\n");
        script.append("                        callbacks: { \n");
        script.append("                            label: function(context) { \n");
        script.append("                                return context.label + ': ' + context.parsed + '%'; \n");
        script.append("                            } \n");
        script.append("                        } \n");
        script.append("                    }\n");
        script.append("                }\n");
        script.append("            }\n");
        script.append("        });\n");

        if (!allSnapshots.isEmpty()) {
            script.append("        const historyData = {\n");
            script.append("            labels: [");
            for (int i = 0; i < allSnapshots.size(); i++) {
                if (i > 0) script.append(", ");

                long timestamp = allSnapshots.get(i).timestamp;
                java.util.Date date = new java.util.Date(timestamp);
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
                script.append("'").append(timeFormat.format(date)).append("'");
            }
            script.append("],\n");
            script.append("            tps: [");
            for (int i = 0; i < allSnapshots.size(); i++) {
                if (i > 0) script.append(", ");
                script.append(allSnapshots.get(i).tps);
            }
            script.append("],\n");
            script.append("            memory: [");
            for (int i = 0; i < allSnapshots.size(); i++) {
                if (i > 0) script.append(", ");
                script.append(allSnapshots.get(i).memoryPercent);
            }
            script.append("],\n");
            script.append("            srs: [");
            for (int i = 0; i < allSnapshots.size(); i++) {
                if (i > 0) script.append(", ");
                script.append(allSnapshots.get(i).srs);
            }
            script.append("]\n");
            script.append("        };\n");
                
                script.append("        new Chart(document.getElementById('historyChart'), {\n");
                script.append("            type: 'line',\n");
                script.append("            data: {\n");
                script.append("                labels: historyData.labels,\n");
                script.append("                datasets: [\n");
                script.append("                    {\n");
                script.append("                        label: 'TPS',\n");
                script.append("                        data: historyData.tps,\n");
                script.append("                        borderColor: '#8b5cf6',\n");
                script.append("                        backgroundColor: 'rgba(139, 92, 246, 0.1)',\n");
                script.append("                        yAxisID: 'y',\n");
                script.append("                        tension: 0.4\n");
                script.append("                    },\n");
                script.append("                    {\n");
                script.append("                        label: 'Memory %',\n");
                script.append("                        data: historyData.memory,\n");
                script.append("                        borderColor: '#f87171',\n");
                script.append("                        backgroundColor: 'rgba(248, 113, 113, 0.1)',\n");
                script.append("                        yAxisID: 'y1',\n");
                script.append("                        tension: 0.4\n");
                script.append("                    },\n");
                script.append("                    {\n");
                script.append("                        label: 'SRS',\n");
                script.append("                        data: historyData.srs,\n");
                script.append("                        borderColor: '#4ade80',\n");
                script.append("                        backgroundColor: 'rgba(74, 222, 128, 0.1)',\n");
                script.append("                        yAxisID: 'y2',\n");
                script.append("                        tension: 0.4\n");
                script.append("                    }\n");
                script.append("                ]\n");
                script.append("            },\n");
                script.append("            options: {\n");
                script.append("                responsive: true,\n");
                script.append("                maintainAspectRatio: false,\n");
                script.append("                animation: { duration: 2000, easing: 'easeInOutQuart' },\n");
                script.append("                interaction: { mode: 'index', intersect: false },\n");
                script.append("                plugins: {\n");
                script.append("                    legend: { position: 'top', labels: { color: '#9ca3af' } },\n");
                script.append("                    tooltip: { \n");
                script.append("                        backgroundColor: 'rgba(30, 41, 59, 0.9)',\n");
                script.append("                        titleColor: '#8b5cf6',\n");
                script.append("                        bodyColor: '#e2e8f0',\n");
                script.append("                        borderColor: '#8b5cf6',\n");
                script.append("                        borderWidth: 1\n");
                script.append("                    }\n");
                script.append("                },\n");
                script.append("                scales: {\n");
                script.append("                    y: { type: 'linear', position: 'left', min: 0, max: 20, ticks: { color: '#9ca3af' }, grid: { color: '#374151' } },\n");
                script.append("                    y1: { type: 'linear', position: 'right', min: 0, max: 100, ticks: { color: '#9ca3af' }, grid: { drawOnChartArea: false } },\n");
                script.append("                    y2: { type: 'linear', position: 'right', min: 0, max: 100, ticks: { color: '#9ca3af' }, grid: { drawOnChartArea: false } },\n");
                script.append("                    x: { ticks: { color: '#9ca3af' }, grid: { color: '#374151' } }\n");
                script.append("                }\n");
                script.append("            }\n");
                script.append("        });\n");
        }
        
        return script.toString();
    }
    
    private static String getNavigationScript() {
        return """

            document.addEventListener('DOMContentLoaded', function() {
                const navLinks = document.querySelectorAll('.nav-link');
                const sections = document.querySelectorAll('.section');

                navLinks.forEach(link => {
                    link.addEventListener('click', function(e) {
                        e.preventDefault();
                        const targetId = this.getAttribute('href').substring(1);
                        const targetSection = document.getElementById(targetId);
                        
                        if (targetSection) {

                            navLinks.forEach(l => l.classList.remove('active'));

                            this.classList.add('active');

                            targetSection.scrollIntoView({
                                behavior: 'smooth',
                                block: 'start'
                            });
                        }
                    });
                });

                function updateActiveNav() {
                    let current = '';
                    sections.forEach(section => {
                        const sectionTop = section.offsetTop;
                        const sectionHeight = section.clientHeight;
                        if (window.pageYOffset >= sectionTop - 200) {
                            current = section.getAttribute('id');
                        }
                    });
                    
                    navLinks.forEach(link => {
                        link.classList.remove('active');
                        if (link.getAttribute('href') === '#' + current) {
                            link.classList.add('active');
                        }
                    });
                }
                
                window.addEventListener('scroll', updateActiveNav);
                updateActiveNav();
            });
            """;
    }
    
    private static String getTabScript() {
        return """

            function showTab(tabName) {

                const tabContents = document.querySelectorAll('.tab-content');
                tabContents.forEach(tab => {
                    tab.classList.remove('active');
                });

                const tabButtons = document.querySelectorAll('.tab-button');
                tabButtons.forEach(button => {
                    button.classList.remove('active');
                });

                const selectedTab = document.getElementById(tabName + '-tab');
                if (selectedTab) {
                    selectedTab.classList.add('active');
                }

                tabButtons.forEach(button => {
                    if (button.textContent.toLowerCase().replace(' ', '-') === tabName || 
                        button.getAttribute('onclick').includes(tabName)) {
                        button.classList.add('active');
                    }
                });
            }

            document.addEventListener('DOMContentLoaded', function() {
                const tabButtons = document.querySelectorAll('.tab-button');
                tabButtons.forEach(button => {
                    button.addEventListener('click', function() {
                        const onclick = this.getAttribute('onclick');
                        const match = onclick.match(/'([^']+)'/);
                        if (match) {
                            showTab(match[1]);
                        }
                    });
                });
            });
            """;
    }
    
    private static String getAnimationScript() {
        return """

            document.addEventListener('DOMContentLoaded', function() {

                let sectionIndex = 0;
                const sections = document.querySelectorAll('.section');
                
                function animateSection() {
                    if (sectionIndex < sections.length) {
                        const section = sections[sectionIndex];
                        section.style.opacity = '0';
                        section.style.transform = 'translateY(20px)';
                        requestAnimationFrame(() => {
                            section.style.transition = 'all 0.4s ease-out';
                            section.style.opacity = '1';
                            section.style.transform = 'translateY(0)';
                        });
                        sectionIndex++;
                        setTimeout(animateSection, 50);
                    }
                }
                animateSection();

                const scoreFill = document.querySelector('.score-fill');
                if (scoreFill) {
                    const width = scoreFill.style.width;
                    scoreFill.style.width = '0%';
                    setTimeout(() => {
                        scoreFill.style.transition = 'width 1.5s cubic-bezier(0.4, 0, 0.2, 1)';
                        scoreFill.style.width = width;
                    }, 500);
                }

                const animateValue = (element, start, end, duration) => {
                    let startTimestamp = null;
                    const step = (timestamp) => {
                        if (!startTimestamp) startTimestamp = timestamp;
                        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
                        const current = Math.floor(progress * (end - start) + start);
                        element.textContent = current;
                        if (progress < 1) {
                            window.requestAnimationFrame(step);
                        }
                    };
                    window.requestAnimationFrame(step);
                };

                const srsValue = document.querySelector('.score-value');
                if (srsValue) {
                    const text = srsValue.textContent;
                    const score = parseInt(text.split('/')[0]);
                    srsValue.textContent = '0/100';
                    setTimeout(() => {
                        animateValue(srsValue, 0, score, 1500);
                        setTimeout(() => {
                            srsValue.textContent = text;
                        }, 1500);
                    }, 800);
                }

                const metricCards = document.querySelectorAll('.metric-card, .info-card');
                metricCards.forEach((card, index) => {
                    card.style.opacity = '0';
                    card.style.transform = 'scale(0.9)';
                    setTimeout(() => {
                        card.style.transition = 'all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
                        card.style.opacity = '1';
                        card.style.transform = 'scale(1)';
                    }, 1000 + index * 50);
                });

                const importantMetrics = document.querySelectorAll('.metric-value');
                importantMetrics.forEach(metric => {
                    metric.addEventListener('mouseenter', function() {
                        this.style.transform = 'scale(1.1)';
                        this.style.transition = 'transform 0.3s ease';
                    });
                    metric.addEventListener('mouseleave', function() {
                        this.style.transform = 'scale(1)';
                    });
                });

                const charts = document.querySelectorAll('canvas');
                charts.forEach((chart, index) => {
                    chart.style.opacity = '0';
                    setTimeout(() => {
                        chart.style.transition = 'opacity 0.8s ease-in';
                        chart.style.opacity = '1';
                    }, 1500 + index * 300);
                });

                const listItems = document.querySelectorAll('.issue-item, .warning-item, .recommendation-item');
                listItems.forEach((item, index) => {
                    item.style.opacity = '0';
                    item.style.transform = 'translateX(-20px)';
                    setTimeout(() => {
                        item.style.transition = 'all 0.4s ease-out';
                        item.style.opacity = '1';
                        item.style.transform = 'translateX(0)';
                    }, 2000 + index * 100);
                });

                document.querySelectorAll('a[href^="#"]').forEach(anchor => {
                    anchor.addEventListener('click', function (e) {
                        e.preventDefault();
                        const target = document.querySelector(this.getAttribute('href'));
                        if (target) {
                            target.scrollIntoView({
                                behavior: 'smooth',
                                block: 'start'
                            });
                        }
                    });
                });
            });

            if ('IntersectionObserver' in window) {
                const observerOptions = {
                    threshold: 0.1,
                    rootMargin: '0px 0px -100px 0px'
                };
                
                const observer = new IntersectionObserver((entries) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            entry.target.style.opacity = '1';
                            entry.target.style.transform = 'translateY(0)';
                            observer.unobserve(entry.target);
                        }
                    });
                }, observerOptions);

                document.querySelectorAll('.section').forEach(section => {
                    section.style.opacity = '0.7';
                    section.style.transform = 'translateY(10px)';
                    section.style.transition = 'opacity 0.4s ease-out, transform 0.4s ease-out';
                    observer.observe(section);
                });
            }
            """;
    }
    
    private static String getModals(
        TpsAnalyzer.TpsTrend tpsTrend,
        MemoryMonitor.MemorySnapshot currentMemory,
        MemoryMonitor.MemoryLeakAnalysis memoryLeak,
        double memoryPercent,
        NetworkMonitor.NetworkStabilityAnalysis networkStability,
        DiskProbe.DiskSnapshot diskSnapshot,
        DiskProbe.DiskLatencyAnalysis diskLatency,
        HealthIndex.ServerResilienceScore srs,
        DiagnosisEngine.Diagnosis diagnosis,
        List<RecommendationEngine.Recommendation> recommendations,
        PluginProfiler pluginProfiler,
        ChunkScanner chunkScanner
    ) {
        StringBuilder modals = new StringBuilder();

        modals.append("    <div id=\"server-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>📊 Информация о сервере</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Версия Minecraft:</strong> ").append(org.bukkit.Bukkit.getVersion()).append("</p>\n");
        modals.append("                <p><strong>Версия сервера:</strong> ").append(org.bukkit.Bukkit.getBukkitVersion()).append("</p>\n");
        modals.append("                <p><strong>Тип сервера:</strong> ").append(org.bukkit.Bukkit.getServer().getClass().getSimpleName()).append("</p>\n");
        modals.append("                <p><strong>Онлайн игроков:</strong> ").append(org.bukkit.Bukkit.getOnlinePlayers().size()).append(" / ").append(org.bukkit.Bukkit.getMaxPlayers()).append("</p>\n");
        modals.append("                <p><strong>Загружено миров:</strong> ").append(org.bukkit.Bukkit.getWorlds().size()).append("</p>\n");
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");

        modals.append("    <div id=\"plugins-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>📦 Информация о плагинах</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Всего плагинов:</strong> ").append(org.bukkit.Bukkit.getPluginManager().getPlugins().length).append("</p>\n");
        modals.append("                <p><strong>Активных плагинов:</strong> ").append(
            java.util.Arrays.stream(org.bukkit.Bukkit.getPluginManager().getPlugins())
                .filter(org.bukkit.plugin.Plugin::isEnabled)
                .count()
        ).append("</p>\n");
        modals.append("                <p><strong>Отключенных плагинов:</strong> ").append(
            java.util.Arrays.stream(org.bukkit.Bukkit.getPluginManager().getPlugins())
                .filter(p -> !p.isEnabled())
                .count()
        ).append("</p>\n");
        modals.append("                <h3>Список плагинов:</h3>\n");
        modals.append("                <ul class=\"plugin-list\">\n");
        for (org.bukkit.plugin.Plugin plugin : org.bukkit.Bukkit.getPluginManager().getPlugins()) {
            modals.append("                    <li>").append(plugin.getName())
                .append(" <span class=\"plugin-status\">(").append(plugin.isEnabled() ? "✓ Включен" : "✗ Отключен").append(")</span></li>\n");
        }
        modals.append("                </ul>\n");
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");

        modals.append("    <div id=\"uptime-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>⏱️ Время работы сервера</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Время работы:</strong> ").append(TimeUtil.formatDuration(System.currentTimeMillis() - getServerStartTime())).append("</p>\n");
        modals.append("                <p><strong>Время запуска:</strong> ").append(TimeUtil.getCurrentTimeString()).append("</p>\n");
        modals.append("                <p>Сервер работает стабильно и обрабатывает запросы игроков.</p>\n");
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");

        modals.append("    <div id=\"srs-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>📈 Индекс устойчивости сервера (SRS)</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Общий балл:</strong> ").append(srs.score).append("/100</p>\n");
        modals.append("                <p>").append(srs.interpretation.replace("\n", "<br>")).append("</p>\n");
        modals.append("                <h3>Компоненты оценки:</h3>\n");
        modals.append("                <ul class=\"component-list\">\n");
        for (HealthIndex.ScoreComponent component : srs.components) {
            double percent = (component.score / component.maxScore) * 100;
            String color = percent >= 80 ? "#4ade80" : percent >= 60 ? "#fbbf24" : "#f87171";
            modals.append("                    <li>\n");
            modals.append("                        <strong>").append(component.name).append(":</strong> ");
            modals.append("<span style=\"color: ").append(color).append("\">").append(String.format("%.1f", component.score)).append(" / ").append(String.format("%.1f", component.maxScore)).append("</span> ");
            modals.append("(").append(String.format("%.0f%%", percent)).append(")\n");
            modals.append("                    </li>\n");
        }
        modals.append("                </ul>\n");
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");

        modals.append("    <div id=\"tps-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>🧩 Детальная информация о TPS</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Средний TPS:</strong> ").append(String.format("%.2f", tpsTrend.average)).append(" (целевой: 20.00)</p>\n");
        modals.append("                <p><strong>Минимальный TPS:</strong> ").append(String.format("%.2f", tpsTrend.min)).append("</p>\n");
        modals.append("                <p><strong>Максимальный TPS:</strong> ").append(String.format("%.2f", tpsTrend.max)).append("</p>\n");
        modals.append("                <p><strong>Стандартное отклонение:</strong> ").append(String.format("%.2f", tpsTrend.stdDev)).append("</p>\n");
        modals.append("                <p><strong>Стабильность:</strong> ").append(tpsTrend.stability).append("</p>\n");
        modals.append("                <h3>Что такое TPS?</h3>\n");
        modals.append("                <p>TPS (Ticks Per Second) - это количество тиков, которые сервер обрабатывает за секунду. Идеальное значение - 20 TPS, что означает, что сервер обрабатывает 20 тиков в секунду.</p>\n");
        modals.append("                <h3>Интерпретация:</h3>\n");
        if (tpsTrend.average >= 19.5) {
            modals.append("                <p class=\"info-good\">✓ Отличный TPS! Сервер работает оптимально.</p>\n");
        } else if (tpsTrend.average >= 18.0) {
            modals.append("                <p class=\"info-warning\">⚠ TPS немного ниже нормы. Рекомендуется оптимизация.</p>\n");
        } else {
            modals.append("                <p class=\"info-error\">✗ Низкий TPS! Сервер испытывает проблемы с производительностью.</p>\n");
        }
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");

        modals.append("    <div id=\"memory-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>🧠 Детальная информация о памяти</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Использовано:</strong> ").append(FileUtil.formatFileSize(currentMemory.used)).append("</p>\n");
        modals.append("                <p><strong>Максимум:</strong> ").append(FileUtil.formatFileSize(currentMemory.max)).append("</p>\n");
        modals.append("                <p><strong>Выделено:</strong> ").append(FileUtil.formatFileSize(currentMemory.committed)).append("</p>\n");
        modals.append("                <p><strong>Использование:</strong> ").append(String.format("%.1f%%", memoryPercent)).append("</p>\n");
        modals.append("                <p><strong>Свободно:</strong> ").append(FileUtil.formatFileSize(currentMemory.max - currentMemory.used)).append("</p>\n");
        if (memoryLeak.possibleLeak) {
            modals.append("                <div class=\"alert alert-warning\">\n");
            modals.append("                    <strong>⚠ Возможная утечка памяти:</strong> ").append(memoryLeak.diagnosis).append("\n");
            modals.append("                </div>\n");
        }
        modals.append("                <h3>Что такое память?</h3>\n");
        modals.append("                <p>Память (Heap) - это область памяти, где Java хранит объекты. Высокое использование памяти может привести к замедлению работы сервера и даже к его падению.</p>\n");
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");

        modals.append("    <div id=\"network-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>🌐 Детальная информация о сети</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Средний пинг:</strong> ").append(String.format("%.0f мс", networkStability.avgPing)).append("</p>\n");
        modals.append("                <p><strong>Джиттер:</strong> ±").append(String.format("%.0f%%", networkStability.jitterPercent)).append("</p>\n");
        modals.append("                <p><strong>Статус:</strong> ").append(networkStability.hasIssue ? "⚠ Нестабильно" : "✓ Стабильно").append("</p>\n");
        modals.append("                <h3>Что такое пинг и джиттер?</h3>\n");
        modals.append("                <p><strong>Пинг</strong> - это время отклика между клиентом и сервером. Низкий пинг означает быструю связь.</p>\n");
        modals.append("                <p><strong>Джиттер</strong> - это вариация пинга. Высокий джиттер означает нестабильное соединение.</p>\n");
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");

        modals.append("    <div id=\"disk-info\" class=\"modal\">\n");
        modals.append("        <div class=\"modal-content\">\n");
        modals.append("            <span class=\"modal-close\">&times;</span>\n");
        modals.append("            <h2>💾 Детальная информация о диске</h2>\n");
        modals.append("            <div class=\"modal-body\">\n");
        modals.append("                <p><strong>Использовано:</strong> ").append(FileUtil.formatFileSize(diskSnapshot.usedSpace)).append("</p>\n");
        modals.append("                <p><strong>Свободно:</strong> ").append(FileUtil.formatFileSize(diskSnapshot.freeSpace)).append("</p>\n");
        modals.append("                <p><strong>Всего:</strong> ").append(FileUtil.formatFileSize(diskSnapshot.totalSpace)).append("</p>\n");
        modals.append("                <p><strong>Задержка диска:</strong> ").append(String.format("%.2f мс", (double) diskLatency.avgLatency)).append("</p>\n");
        if (diskLatency.hasIssue) {
            modals.append("                <div class=\"alert alert-warning\">\n");
            modals.append("                    <strong>⚠ Проблема:</strong> ").append(diskLatency.diagnosis).append("\n");
            modals.append("                </div>\n");
        }
        modals.append("                <h3>Что такое задержка диска?</h3>\n");
        modals.append("                <p>Задержка диска - это время, необходимое для записи данных на диск. Высокая задержка может замедлить сохранение мира и автосохранения.</p>\n");
        modals.append("            </div>\n");
        modals.append("        </div>\n");
        modals.append("    </div>\n");
        
        return modals.toString();
    }
    
    private static String getModalScript() {
        return """

            document.addEventListener('DOMContentLoaded', function() {

                document.querySelectorAll('.clickable').forEach(card => {
                    card.addEventListener('click', function() {
                        const modalId = this.getAttribute('data-modal');
                        if (modalId) {
                            openModal(modalId);
                        }
                    });
                });

                document.querySelectorAll('.modal-close').forEach(closeBtn => {
                    closeBtn.addEventListener('click', function(e) {
                        e.stopPropagation();
                        const modal = this.closest('.modal');
                        if (modal) {
                            closeModal(modal.id);
                        }
                    });
                });

                document.querySelectorAll('.modal').forEach(modal => {
                    modal.addEventListener('click', function(e) {

                        if (e.target === this) {
                            closeModal(this.id);
                        }
                    });

                    const modalContent = modal.querySelector('.modal-content');
                    if (modalContent) {
                        modalContent.addEventListener('click', function(e) {
                            e.stopPropagation();
                        });
                    }
                });

                document.addEventListener('keydown', function(e) {
                    if (e.key === 'Escape') {

                        document.querySelectorAll('.modal.show').forEach(modal => {
                            closeModal(modal.id);
                        });
                    }
                });
            });
            
            function openModal(modalId) {

                document.querySelectorAll('.modal.show').forEach(modal => {
                    modal.classList.remove('show');
                });

                setTimeout(() => {
                    const modal = document.getElementById(modalId);
                    if (modal) {
                        modal.classList.add('show');
                        document.body.style.overflow = 'hidden';

                        const modalContent = modal.querySelector('.modal-content');
                        if (modalContent) {
                            modalContent.scrollTop = 0;
                        }

                        requestAnimationFrame(() => {
                            modal.scrollIntoView({ behavior: 'smooth', block: 'center' });

                            if (modal.scrollTop > 0) {
                                modal.scrollTop = 0;
                            }
                        });
                    }
                }, 100);
            }
            
            function closeModal(modalId) {
                const modal = document.getElementById(modalId);
                if (modal) {
                    modal.classList.remove('show');

                    const openModals = document.querySelectorAll('.modal.show');
                    if (openModals.length === 0) {
                        document.body.style.overflow = 'auto';
                    }
                }
            }
            """;
    }
    
    private static String getCSS() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            @keyframes fadeIn {
                from {
                    opacity: 0;
                }
                to {
                    opacity: 1;
                }
            }
            @keyframes fadeInUp {
                from {
                    opacity: 0;
                    transform: translateY(30px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }
            @keyframes slideInLeft {
                from {
                    opacity: 0;
                    transform: translateX(-30px);
                }
                to {
                    opacity: 1;
                    transform: translateX(0);
                }
            }
            @keyframes slideInRight {
                from {
                    opacity: 0;
                    transform: translateX(30px);
                }
                to {
                    opacity: 1;
                    transform: translateX(0);
                }
            }
            @keyframes scaleIn {
                from {
                    opacity: 0;
                    transform: scale(0.95) translateY(-20px);
                }
                to {
                    opacity: 1;
                    transform: scale(1) translateY(0);
                }
            }
            @keyframes pulse {
                0%, 100% {
                    transform: scale(1);
                    box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.4);
                }
                50% {
                    transform: scale(1.05);
                    box-shadow: 0 0 0 10px rgba(139, 92, 246, 0);
                }
            }
            @keyframes glow {
                0%, 100% {
                    text-shadow: 0 0 10px rgba(139, 92, 246, 0.5);
                }
                50% {
                    text-shadow: 0 0 20px rgba(139, 92, 246, 0.8), 0 0 30px rgba(139, 92, 246, 0.4);
                }
            }
            @keyframes shimmer {
                0% {
                    background-position: -1000px 0;
                }
                100% {
                    background-position: 1000px 0;
                }
            }
            @keyframes rotate {
                from {
                    transform: rotate(0deg);
                }
                to {
                    transform: rotate(360deg);
                }
            }
            @keyframes fadeOut {
                from {
                    opacity: 1;
                }
                to {
                    opacity: 0;
                }
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                background: #0f172a;
                color: #e2e8f0;
                margin: 0;
                padding: 0;
                line-height: 1.6;
                animation: fadeIn 0.5s ease-in;
                display: flex;
            }
            
            .sidebar {
                position: fixed;
                left: 0;
                top: 0;
                width: 250px;
                height: 100vh;
                background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
                border-right: 1px solid #334155;
                padding: 20px 0;
                overflow-y: auto;
                z-index: 100;
                box-shadow: 2px 0 10px rgba(0, 0, 0, 0.3);
            }
            .sidebar-header {
                padding: 20px;
                text-align: center;
                border-bottom: 1px solid #334155;
                margin-bottom: 20px;
            }
            .sidebar-header h2 {
                color: #8b5cf6;
                font-size: 1.5em;
                margin: 0;
            }
            .sidebar-menu {
                list-style: none;
                padding: 0;
                margin: 0;
            }
            .sidebar-menu li {
                margin: 0;
            }
            .nav-link {
                display: block;
                padding: 15px 25px;
                color: #94a3b8;
                text-decoration: none;
                transition: all 0.3s ease;
                border-left: 3px solid transparent;
            }
            .nav-link:hover {
                background: rgba(139, 92, 246, 0.1);
                color: #8b5cf6;
                border-left-color: #8b5cf6;
            }
            .nav-link.active {
                background: rgba(139, 92, 246, 0.2);
                color: #8b5cf6;
                border-left-color: #8b5cf6;
                font-weight: bold;
            }
            
            .main-content {
                margin-left: 250px;
                padding: 20px;
                width: calc(100% - 250px);
                min-height: 100vh;
            }
            .header {
                background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                padding: 30px;
                border-radius: 16px;
                margin-bottom: 30px;
                box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
                border: 1px solid #334155;
                animation: fadeInUp 0.8s ease-out;
                position: relative;
                overflow: hidden;
            }
            .last-snapshot {
                color: #94a3b8;
                font-size: 14px;
                margin-top: 10px;
            }
            .header::before {
                content: '';
                position: absolute;
                top: 0;
                left: -100%;
                width: 100%;
                height: 100%;
                background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.1), transparent);
                animation: shimmer 3s infinite;
            }
            .header-content {
                text-align: center;
            }
            .header h1 {
                color: #8b5cf6;
                font-size: 2.5em;
                margin-bottom: 10px;
                text-shadow: 0 2px 10px rgba(139, 92, 246, 0.3);
                animation: glow 2s ease-in-out infinite;
                position: relative;
                z-index: 1;
            }
            .timestamp {
                color: #94a3b8;
                font-size: 14px;
            }
            .section {
                background: #1e293b;
                padding: 30px;
                border-radius: 16px;
                margin-bottom: 30px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
                border: 1px solid #334155;
                transition: all 0.3s ease;
                position: relative;
                scroll-margin-top: 20px;
            }
            .section:hover {
                transform: translateY(-3px);
                box-shadow: 0 8px 30px rgba(139, 92, 246, 0.2);
                border-color: #8b5cf6;
            }
            
            .section, .metric-card, .info-card {
                will-change: transform;
            }
            
            @media (max-width: 768px) {
                .section {
                    transition: none;
                }
                .section:hover {
                    transform: none;
                }
            }
            .section h2 {
                color: #8b5cf6;
                margin-bottom: 20px;
                font-size: 1.8em;
                border-bottom: 2px solid #334155;
                padding-bottom: 15px;
                transition: all 0.3s ease;
            }
            .section:hover h2 {
                color: #a78bfa;
                border-bottom-color: #8b5cf6;
            }
            .info-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 20px;
            }
            .info-card {
                background: #0f172a;
                padding: 20px;
                border-radius: 12px;
                border: 1px solid #334155;
                text-align: center;
                transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            }
            .clickable {
                cursor: pointer;
            }
            .clickable:hover {
                transform: translateY(-5px) scale(1.02);
            }
            .info-card:hover {
                transform: translateY(-8px) scale(1.05);
                border-color: #8b5cf6;
                box-shadow: 0 10px 25px rgba(139, 92, 246, 0.3);
            }
            .info-label {
                color: #94a3b8;
                font-size: 14px;
                margin-bottom: 8px;
            }
            .info-value {
                color: #e2e8f0;
                font-size: 18px;
                font-weight: bold;
                transition: all 0.3s ease;
            }
            .info-card:hover .info-value {
                color: #8b5cf6;
                transform: scale(1.1);
            }
            .srs-score {
                text-align: center;
                padding: 30px;
            }
            .score-value {
                font-size: 64px;
                font-weight: bold;
                margin-bottom: 20px;
                text-shadow: 0 4px 20px rgba(139, 92, 246, 0.4);
                transition: all 0.3s ease;
                display: inline-block;
            }
            .score-value:hover {
                transform: scale(1.1);
                animation: glow 1s ease-in-out infinite;
            }
            .score-bar {
                width: 100%;
                height: 40px;
                background: #0f172a;
                border-radius: 20px;
                overflow: hidden;
                margin-bottom: 20px;
                border: 1px solid #334155;
            }
            .score-fill {
                height: 100%;
                transition: width 1.5s cubic-bezier(0.4, 0, 0.2, 1);
                border-radius: 20px;
                position: relative;
                overflow: hidden;
            }
            .score-fill::after {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
                animation: shimmer 2s infinite;
            }
            .score-interpretation {
                color: #94a3b8;
                font-size: 16px;
                line-height: 1.8;
            }
            .metric-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 20px;
                margin-bottom: 30px;
            }
            .metric-card {
                background: #0f172a;
                padding: 20px;
                border-radius: 12px;
                border: 1px solid #334155;
                text-align: center;
                transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
                cursor: pointer;
                position: relative;
                overflow: hidden;
            }
            .metric-card::before {
                content: '';
                position: absolute;
                top: 0;
                left: -100%;
                width: 100%;
                height: 100%;
                background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.1), transparent);
                transition: left 0.5s;
            }
            .metric-card:hover::before {
                left: 100%;
            }
            .metric-card:hover {
                transform: translateY(-8px) scale(1.05);
                border-color: #8b5cf6;
                box-shadow: 0 10px 25px rgba(139, 92, 246, 0.3);
            }
            .metric-label {
                color: #94a3b8;
                font-size: 14px;
                margin-bottom: 10px;
            }
            .metric-value {
                color: #8b5cf6;
                font-size: 24px;
                font-weight: bold;
                transition: all 0.3s ease;
                display: inline-block;
            }
            .metric-card:hover .metric-value {
                color: #a78bfa;
                transform: scale(1.15);
                text-shadow: 0 0 15px rgba(139, 92, 246, 0.5);
            }
            .chart-container {
                height: 300px;
                margin-top: 20px;
                background: #0f172a;
                padding: 20px;
                border-radius: 12px;
                border: 1px solid #334155;
                transition: all 0.3s ease;
                position: relative;
                overflow: hidden;
            }
            .chart-container::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: linear-gradient(45deg, transparent 30%, rgba(139, 92, 246, 0.05) 50%, transparent 70%);
                animation: shimmer 3s infinite;
                pointer-events: none;
            }
            .chart-container:hover {
                border-color: #8b5cf6;
                box-shadow: 0 0 20px rgba(139, 92, 246, 0.2);
            }
            .alert {
                padding: 15px 20px;
                border-radius: 8px;
                margin-top: 15px;
                border-left: 4px solid;
                animation: slideInLeft 0.5s ease-out;
                transition: all 0.3s ease;
            }
            .alert:hover {
                transform: translateX(5px);
                box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
            }
            .alert-warning {
                background: rgba(251, 191, 36, 0.1);
                border-color: #fbbf24;
                color: #fbbf24;
            }
            .alert-info {
                background: rgba(59, 130, 246, 0.1);
                border-color: #3b82f6;
                color: #3b82f6;
            }
            .alert-success {
                background: rgba(74, 222, 128, 0.1);
                border-color: #4ade80;
                color: #4ade80;
            }
            .plugins-list {
                display: flex;
                flex-direction: column;
                gap: 15px;
            }
            .plugin-item {
                background: #0f172a;
                padding: 20px;
                border-radius: 12px;
                border: 1px solid #334155;
                display: flex;
                align-items: center;
                gap: 20px;
                transition: all 0.3s ease;
                cursor: pointer;
            }
            .plugin-item:hover {
                transform: translateX(10px);
                border-color: #8b5cf6;
                box-shadow: 0 5px 20px rgba(139, 92, 246, 0.2);
            }
            .plugin-rank {
                background: #8b5cf6;
                color: white;
                width: 40px;
                height: 40px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: bold;
                font-size: 18px;
                transition: all 0.3s ease;
            }
            .plugin-item:hover .plugin-rank {
                transform: scale(1.2) rotate(360deg);
                box-shadow: 0 0 20px rgba(139, 92, 246, 0.5);
            }
            .plugin-info {
                flex: 1;
            }
            .plugin-name {
                color: #e2e8f0;
                font-size: 18px;
                font-weight: bold;
                margin-bottom: 5px;
            }
            .plugin-time {
                color: #94a3b8;
                font-size: 14px;
            }
            .plugin-issues {
                margin-top: 30px;
                padding: 20px;
                background: rgba(15, 23, 42, 0.5);
                border-radius: 12px;
                border: 1px solid #334155;
            }
            .plugin-issues h3 {
                color: #f87171;
                margin-bottom: 15px;
                font-size: 1.3em;
            }
            .plugin-issue-item {
                padding: 15px;
                margin-bottom: 15px;
                background: rgba(248, 113, 113, 0.05);
                border-radius: 8px;
                border-left: 4px solid;
            }
            .plugin-issue-title {
                color: #e2e8f0;
                font-size: 16px;
                margin-bottom: 8px;
            }
            .plugin-issue-description {
                color: #94a3b8;
                font-size: 14px;
                line-height: 1.6;
            }
            .hot-zones-list {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 15px;
            }
            .hot-zone-item {
                background: #0f172a;
                padding: 15px;
                border-radius: 8px;
                border: 1px solid #334155;
                transition: all 0.3s ease;
                cursor: pointer;
                animation: pulse 2s infinite;
            }
            .hot-zone-item:hover {
                transform: scale(1.05);
                border-color: #f87171;
                box-shadow: 0 5px 20px rgba(248, 113, 113, 0.3);
            }
            .hot-zone-location {
                color: #e2e8f0;
                font-weight: bold;
                margin-bottom: 5px;
            }
            .hot-zone-entities {
                color: #f87171;
                font-size: 14px;
            }
            .diagnosis-box {
                background: #0f172a;
                padding: 25px;
                border-radius: 12px;
                border: 1px solid #334155;
            }
            .diagnosis-summary {
                color: #e2e8f0;
                font-size: 18px;
                margin-bottom: 25px;
                padding-bottom: 20px;
                border-bottom: 1px solid #334155;
            }
            .issues-section, .warnings-section {
                margin-top: 20px;
            }
            .issues-section h3, .warnings-section h3 {
                color: #f87171;
                margin-bottom: 15px;
            }
            .issues-list, .warnings-list {
                list-style: none;
                padding-left: 0;
            }
            .issue-item {
                color: #f87171;
                padding: 10px;
                margin-bottom: 10px;
                background: rgba(248, 113, 113, 0.1);
                border-radius: 8px;
                border-left: 4px solid #f87171;
                animation: slideInLeft 0.5s ease-out;
                transition: all 0.3s ease;
            }
            .issue-item:hover {
                transform: translateX(5px);
                background: rgba(248, 113, 113, 0.2);
                box-shadow: 0 3px 10px rgba(248, 113, 113, 0.2);
            }
            .warning-item {
                color: #fbbf24;
                padding: 10px;
                margin-bottom: 10px;
                background: rgba(251, 191, 36, 0.1);
                border-radius: 8px;
                border-left: 4px solid #fbbf24;
                animation: slideInLeft 0.5s ease-out;
                transition: all 0.3s ease;
            }
            .warning-item:hover {
                transform: translateX(5px);
                background: rgba(251, 191, 36, 0.2);
                box-shadow: 0 3px 10px rgba(251, 191, 36, 0.2);
            }
            .recommendations-box {
                display: flex;
                flex-direction: column;
                gap: 15px;
            }
            .recommendation-item {
                padding: 20px;
                border-radius: 12px;
                border-left: 4px solid;
                transition: all 0.3s ease;
                cursor: pointer;
                animation: slideInRight 0.5s ease-out;
            }
            .recommendation-item:hover {
                transform: translateX(10px);
                box-shadow: 0 5px 20px rgba(0, 0, 0, 0.3);
            }
            .recommendation-item.priority-high {
                background: rgba(248, 113, 113, 0.1);
                border-color: #f87171;
            }
            .recommendation-item.priority-medium {
                background: rgba(251, 191, 36, 0.1);
                border-color: #fbbf24;
            }
            .recommendation-item.priority-low {
                background: rgba(74, 222, 128, 0.1);
                border-color: #4ade80;
            }
            .recommendation-title {
                color: #e2e8f0;
                font-size: 18px;
                font-weight: bold;
                margin-bottom: 10px;
            }
            .recommendation-description {
                color: #94a3b8;
                line-height: 1.8;
                white-space: pre-line;
                font-family: 'Courier New', monospace;
                background: rgba(15, 23, 42, 0.5);
                padding: 15px;
                border-radius: 8px;
                margin-top: 10px;
                border-left: 3px solid #8b5cf6;
            }
            
            .comparison {
                background: rgba(15, 23, 42, 0.5);
                padding: 15px;
                border-radius: 8px;
                margin-top: 15px;
                border-left: 3px solid #8b5cf6;
            }
            .comparison p {
                margin: 0;
                font-size: 14px;
            }
            
            .history-chart-container {
                height: 400px;
                margin-top: 20px;
                background: #0f172a;
                padding: 20px;
                border-radius: 12px;
                border: 1px solid #334155;
            }
            
            .sidebar::-webkit-scrollbar {
                width: 6px;
            }
            .sidebar::-webkit-scrollbar-track {
                background: #0f172a;
            }
            .sidebar::-webkit-scrollbar-thumb {
                background: #8b5cf6;
                border-radius: 3px;
            }
            .sidebar::-webkit-scrollbar-thumb:hover {
                background: #a78bfa;
            }
            
            .modal {
                display: none;
                position: fixed;
                z-index: 10000;
                left: 0;
                top: 0;
                width: 100%;
                height: 100%;
                background-color: rgba(0, 0, 0, 0.8);
                backdrop-filter: blur(5px);
                overflow-y: auto;
                overscroll-behavior: contain;
            }
            .modal.show {
                display: flex;
                align-items: center;
                justify-content: center;
                animation: fadeIn 0.2s ease-in;
                padding: 20px;
                box-sizing: border-box;
            }
            .modal-content {
                background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                margin: 0;
                padding: 30px;
                border: 1px solid #8b5cf6;
                border-radius: 16px;
                width: 90%;
                max-width: 700px;
                max-height: calc(100vh - 40px);
                overflow-y: auto;
                box-shadow: 0 20px 60px rgba(139, 92, 246, 0.3);
                position: relative;
                animation: scaleIn 0.2s ease-out;
                transform-origin: center center;
                flex-shrink: 0;
            }
            .modal-content h2 {
                color: #8b5cf6;
                margin-bottom: 20px;
                font-size: 1.8em;
                border-bottom: 2px solid #334155;
                padding-bottom: 15px;
            }
            .modal-close {
                color: #94a3b8;
                float: right;
                font-size: 32px;
                font-weight: bold;
                cursor: pointer;
                transition: all 0.3s ease;
                position: absolute;
                top: 15px;
                right: 20px;
                line-height: 1;
            }
            .modal-close:hover {
                color: #f87171;
                transform: rotate(90deg) scale(1.2);
            }
            .modal-body {
                color: #e2e8f0;
                line-height: 1.8;
            }
            .modal-body p {
                margin-bottom: 15px;
            }
            .modal-body h3 {
                color: #8b5cf6;
                margin-top: 25px;
                margin-bottom: 15px;
                font-size: 1.3em;
            }
            .modal-body ul {
                margin-left: 20px;
                margin-bottom: 20px;
            }
            .modal-body li {
                margin-bottom: 10px;
            }
            .component-list li {
                padding: 10px;
                background: rgba(15, 23, 42, 0.5);
                border-radius: 8px;
                margin-bottom: 10px;
                border-left: 3px solid #8b5cf6;
            }
            .plugin-list {
                max-height: 300px;
                overflow-y: auto;
                background: rgba(15, 23, 42, 0.5);
                padding: 15px;
                border-radius: 8px;
            }
            .plugin-list li {
                padding: 8px;
                border-bottom: 1px solid #334155;
            }
            .plugin-list li:last-child {
                border-bottom: none;
            }
            .plugin-status {
                color: #94a3b8;
                font-size: 0.9em;
            }
            .info-good {
                color: #4ade80;
                padding: 10px;
                background: rgba(74, 222, 128, 0.1);
                border-radius: 8px;
                border-left: 4px solid #4ade80;
            }
            .info-warning {
                color: #fbbf24;
                padding: 10px;
                background: rgba(251, 191, 36, 0.1);
                border-radius: 8px;
                border-left: 4px solid #fbbf24;
            }
            .info-error {
                color: #f87171;
                padding: 10px;
                background: rgba(248, 113, 113, 0.1);
                border-radius: 8px;
                border-left: 4px solid #f87171;
            }
            
            .modal-content::-webkit-scrollbar {
                width: 8px;
            }
            .modal-content::-webkit-scrollbar-track {
                background: #0f172a;
                border-radius: 4px;
            }
            .modal-content::-webkit-scrollbar-thumb {
                background: #8b5cf6;
                border-radius: 4px;
            }
            .modal-content::-webkit-scrollbar-thumb:hover {
                background: #a78bfa;
            }
            
            .top-metrics-panel {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 15px;
                margin: 20px 0;
                padding: 20px;
                background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                border-radius: 12px;
                border: 1px solid #475569;
            }
            .metric-card-top {
                background: rgba(15, 23, 42, 0.8);
                padding: 15px;
                border-radius: 8px;
                border: 1px solid #334155;
                transition: all 0.3s ease;
            }
            .metric-card-top:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
                border-color: #8b5cf6;
            }
            .metric-label-top {
                font-size: 0.85em;
                color: #94a3b8;
                margin-bottom: 8px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }
            .metric-values-top {
                display: flex;
                flex-direction: column;
                gap: 4px;
            }
            .metric-value {
                font-size: 1.1em;
                font-weight: 600;
                color: #e2e8f0;
            }
            .metric-value small {
                font-size: 0.75em;
                color: #64748b;
                font-weight: normal;
            }
            .metric-value-large {
                font-size: 1.2em;
                font-weight: 600;
                color: #e2e8f0;
            }
            .metric-value-large small {
                font-size: 0.8em;
                color: #64748b;
                font-weight: normal;
            }
            
            .tabs-container {
                margin: 20px 0;
                background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                border-radius: 12px;
                border: 1px solid #475569;
                overflow: hidden;
            }
            .tabs {
                display: flex;
                background: rgba(15, 23, 42, 0.6);
                border-bottom: 1px solid #334155;
            }
            .tab-button {
                flex: 1;
                padding: 15px 20px;
                background: transparent;
                border: none;
                color: #94a3b8;
                font-size: 1em;
                cursor: pointer;
                transition: all 0.3s ease;
                border-bottom: 3px solid transparent;
            }
            .tab-button:hover {
                background: rgba(139, 92, 246, 0.1);
                color: #e2e8f0;
            }
            .tab-button.active {
                color: #8b5cf6;
                border-bottom-color: #8b5cf6;
                background: rgba(139, 92, 246, 0.1);
            }
            .tab-content {
                display: none;
                padding: 20px;
                animation: fadeIn 0.3s ease;
            }
            .tab-content.active {
                display: block;
            }
            .tab-section {
                margin-bottom: 20px;
                padding: 15px;
                background: rgba(15, 23, 42, 0.5);
                border-radius: 8px;
                border-left: 3px solid #8b5cf6;
            }
            .tab-section h3 {
                color: #8b5cf6;
                margin-bottom: 10px;
                font-size: 1.1em;
            }
            .tab-section p {
                color: #e2e8f0;
                margin: 5px 0;
            }
            .tab-section ul {
                list-style: none;
                padding: 0;
            }
            .tab-section li {
                padding: 8px;
                margin: 5px 0;
                background: rgba(30, 41, 59, 0.5);
                border-radius: 4px;
                color: #cbd5e1;
            }
            
            .world-stats-grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 20px;
            }
            .world-stat-column {
                background: rgba(15, 23, 42, 0.5);
                padding: 15px;
                border-radius: 8px;
            }
            .world-stat-column h3 {
                color: #8b5cf6;
                margin-bottom: 10px;
            }
            .world-stat-column h4 {
                color: #cbd5e1;
                margin-top: 15px;
                margin-bottom: 10px;
            }
            .entity-list {
                max-height: 300px;
                overflow-y: auto;
                list-style: none;
                padding: 0;
            }
            .entity-list li {
                padding: 6px;
                margin: 3px 0;
                background: rgba(30, 41, 59, 0.5);
                border-radius: 4px;
                color: #cbd5e1;
                font-size: 0.9em;
            }
            .region-section {
                margin-top: 20px;
                padding: 15px;
                background: rgba(15, 23, 42, 0.5);
                border-radius: 8px;
            }
            .region-section h3 {
                color: #8b5cf6;
                margin-bottom: 15px;
            }
            .region-item {
                margin: 15px 0;
                padding: 15px;
                background: rgba(30, 41, 59, 0.5);
                border-radius: 8px;
                border-left: 3px solid #8b5cf6;
            }
            .region-item h4 {
                color: #cbd5e1;
                margin-bottom: 10px;
            }
            .region-item p {
                color: #94a3b8;
                margin: 5px 0;
            }
            .region-item details {
                margin-top: 10px;
                color: #cbd5e1;
            }
            .region-item details summary {
                cursor: pointer;
                color: #8b5cf6;
                padding: 5px;
            }
            .region-item details summary:hover {
                color: #a78bfa;
            }
            .profile-info {
                color: #94a3b8;
                font-size: 0.9em;
                margin-top: 5px;
            }
            
            .metric-analysis {
                margin-top: 20px;
                padding: 15px;
                background: rgba(139, 92, 246, 0.1);
                border-left: 3px solid #8b5cf6;
                border-radius: 8px;
            }
            .analysis-text {
                color: #cbd5e1;
                line-height: 1.6;
                font-size: 0.95em;
                margin: 0;
            }
            .metric-analysis-text {
                color: #e2e8f0;
                line-height: 1.6;
                font-size: 0.9em;
                margin-top: 8px;
            }
            """;
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   .replace("\n", "<br>");
    }
    
    private static long getServerStartTime() {
        return System.currentTimeMillis() - (1000 * 60 * 60 * 3);
    }

    private static String generateTopMetricsPanel(
        TpsAnalyzer tpsAnalyzer,
        MsptMonitor msptMonitor,
        CpuMonitor cpuMonitor,
        MemoryMonitor memoryMonitor,
        DiskProbe diskProbe,
        NetworkMonitor networkMonitor,
        java.util.Map<String, String> aiAnalysisMap
    ) {
        StringBuilder html = new StringBuilder();
        html.append("        <div class=\"top-metrics-panel\">\n");

        TpsAnalyzer.TpsTrend tps1m = tpsAnalyzer.analyzeTrend(1);
        TpsAnalyzer.TpsTrend tps5m = tpsAnalyzer.analyzeTrend(5);
        TpsAnalyzer.TpsTrend tps15m = tpsAnalyzer.analyzeTrend(15);
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">TPS</div>\n");
        html.append("                <div class=\"metric-values-top\">\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f", tps1m.average)).append(" <small>(1m)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f", tps5m.average)).append(" <small>(5m)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f", tps15m.average)).append(" <small>(15m)</small></span>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        MsptMonitor.MsptStats msptStats = msptMonitor.getStats();
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">MSPT</div>\n");
        html.append("                <div class=\"metric-values-top\">\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f", msptStats.min)).append(" <small>(min)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f", msptStats.median)).append(" <small>(med)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f", msptStats.p95)).append(" <small>(95%)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f", msptStats.max)).append(" <small>(max)</small></span>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        CpuMonitor.CpuStats cpuStats1m = cpuMonitor.getStats(1);
        CpuMonitor.CpuStats cpuStats15m = cpuMonitor.getStats(15);
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">CPU (process)</div>\n");
        html.append("                <div class=\"metric-values-top\">\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f%%", cpuStats1m.processAvg)).append(" <small>(1m)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f%%", cpuStats15m.processAvg)).append(" <small>(15m)</small></span>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">CPU (system)</div>\n");
        html.append("                <div class=\"metric-values-top\">\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f%%", cpuStats1m.systemAvg)).append(" <small>(1m)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.2f%%", cpuStats15m.systemAvg)).append(" <small>(15m)</small></span>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        MemoryMonitor.PhysicalMemoryInfo physicalMemory = memoryMonitor.getPhysicalMemoryInfo();
        MemoryMonitor.MemorySnapshot processMemory = memoryMonitor.getCurrentSnapshot();
        double physicalPercent = (physicalMemory.usedPhysical * 100.0) / physicalMemory.totalPhysical;
        double swapPercent = physicalMemory.totalSwap > 0 ? (physicalMemory.usedSwap * 100.0) / physicalMemory.totalSwap : 0.0;
        double processPercent = (processMemory.used * 100.0) / processMemory.max;
        
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">Memory (physical)</div>\n");
        html.append("                <div class=\"metric-value-large\">").append(FileUtil.formatFileSize(physicalMemory.usedPhysical))
            .append(" / ").append(FileUtil.formatFileSize(physicalMemory.totalPhysical))
            .append(" <small>(").append(String.format("%.2f%%", physicalPercent)).append(")</small></div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">Memory (swap)</div>\n");
        html.append("                <div class=\"metric-value-large\">").append(FileUtil.formatFileSize(physicalMemory.usedSwap))
            .append(" / ").append(FileUtil.formatFileSize(physicalMemory.totalSwap))
            .append(" <small>(").append(String.format("%.2f%%", swapPercent)).append(")</small></div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">Memory (process)</div>\n");
        html.append("                <div class=\"metric-value-large\">").append(FileUtil.formatFileSize(processMemory.used))
            .append(" / ").append(FileUtil.formatFileSize(processMemory.max))
            .append(" <small>(").append(String.format("%.2f%%", processPercent)).append(")</small></div>\n");
        html.append("            </div>\n");

        try {
            DiskProbe.DiskSnapshot disk = diskProbe.getCurrentSnapshot();
            double diskPercent = ((disk.usedSpace * 100.0) / (disk.usedSpace + disk.freeSpace));
            html.append("            <div class=\"metric-card-top\">\n");
            html.append("                <div class=\"metric-label-top\">Disk</div>\n");
            html.append("                <div class=\"metric-value-large\">").append(FileUtil.formatFileSize(disk.usedSpace))
                .append(" / ").append(FileUtil.formatFileSize(disk.usedSpace + disk.freeSpace))
                .append(" <small>(").append(String.format("%.2f%%", diskPercent)).append(")</small></div>\n");
            html.append("            </div>\n");
        } catch (Exception e) {

        }

        MemoryMonitor.GcStats gcStats = memoryMonitor.getGcStats();
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">GC (G1 Young, during)</div>\n");
        html.append("                <div class=\"metric-values-top\">\n");
        html.append("                    <span class=\"metric-value\">").append(gcStats.youngGc.total).append(" <small>(total)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.0fms", gcStats.youngGc.avgTime)).append(" <small>(avg time)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.1fs", gcStats.youngGc.avgFreq)).append(" <small>(avg freq)</small></span>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric-card-top\">\n");
        html.append("                <div class=\"metric-label-top\">GC (G1 Old, during)</div>\n");
        html.append("                <div class=\"metric-values-top\">\n");
        html.append("                    <span class=\"metric-value\">").append(gcStats.oldGc.total).append(" <small>(total)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.0fms", gcStats.oldGc.avgTime)).append(" <small>(avg time)</small></span>\n");
        html.append("                    <span class=\"metric-value\">").append(String.format("%.1fs", gcStats.oldGc.avgFreq)).append(" <small>(avg freq)</small></span>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("cpu")) {
            String cpuAnalysis = aiAnalysisMap.get("cpu");
            html.append("            <div class=\"metric-card-top\" style=\"grid-column: 1 / -1;\">\n");
            html.append("                <div class=\"metric-label-top\">Анализ CPU</div>\n");
            html.append("                <div class=\"metric-analysis-text\">").append(escapeHtml(cpuAnalysis)).append("</div>\n");
            html.append("            </div>\n");
        }

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("gc")) {
            String gcAnalysis = aiAnalysisMap.get("gc");
            html.append("            <div class=\"metric-card-top\" style=\"grid-column: 1 / -1;\">\n");
            html.append("                <div class=\"metric-label-top\">Анализ GC</div>\n");
            html.append("                <div class=\"metric-analysis-text\">").append(escapeHtml(gcAnalysis)).append("</div>\n");
            html.append("            </div>\n");
        }

        if (aiAnalysisMap != null && aiAnalysisMap.containsKey("mspt")) {
            String msptAnalysis = aiAnalysisMap.get("mspt");
            html.append("            <div class=\"metric-card-top\" style=\"grid-column: 1 / -1;\">\n");
            html.append("                <div class=\"metric-label-top\">Анализ MSPT</div>\n");
            html.append("                <div class=\"metric-analysis-text\">").append(escapeHtml(msptAnalysis)).append("</div>\n");
            html.append("            </div>\n");
        }
        
        html.append("        </div>\n");
        return html.toString();
    }

    private static String generateTabs(PluginProfiler pluginProfiler, ChunkScanner chunkScanner) {
        StringBuilder html = new StringBuilder();
        html.append("        <div class=\"tabs-container\">\n");
        html.append("            <div class=\"tabs\">\n");
        html.append("                <button class=\"tab-button active\" onclick=\"showTab('platform')\">Platform</button>\n");
        html.append("                <button class=\"tab-button\" onclick=\"showTab('jvm-flags')\">JVM Flags</button>\n");
        html.append("                <button class=\"tab-button\" onclick=\"showTab('configurations')\">Configurations</button>\n");
        html.append("                <button class=\"tab-button\" onclick=\"showTab('world')\">World</button>\n");
        html.append("            </div>\n");

        html.append("            <div id=\"platform-tab\" class=\"tab-content active\">\n");
        me.testikgm.util.SystemInfo.PlatformInfo platform = me.testikgm.util.SystemInfo.getPlatformInfo();
        html.append("                <div class=\"tab-section\">\n");
        html.append("                    <h3>Java Version</h3>\n");
        html.append("                    <p>").append(platform.javaVersion).append(" (").append(platform.javaVendor).append(")</p>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"tab-section\">\n");
        html.append("                    <h3>Operating System</h3>\n");
        html.append("                    <p>").append(platform.osName).append(" ").append(platform.osVersion).append(" (").append(platform.osArch).append(")</p>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"tab-section\">\n");
        html.append("                    <h3>Server</h3>\n");
        html.append("                    <p>").append(platform.serverVersion).append("</p>\n");
        html.append("                    <p>").append(platform.serverImplementation).append(" ").append(platform.serverImplementationVersion != null ? platform.serverImplementationVersion : "").append("</p>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        html.append("            <div id=\"jvm-flags-tab\" class=\"tab-content\">\n");
        me.testikgm.util.SystemInfo.JvmFlagsInfo jvmFlags = me.testikgm.util.SystemInfo.getJvmFlags();
        html.append("                <div class=\"tab-section\">\n");
        html.append("                    <h3>GC Flags</h3>\n");
        html.append("                    <ul>\n");
        for (String flag : jvmFlags.gcFlags) {
            html.append("                        <li>").append(flag).append("</li>\n");
        }
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"tab-section\">\n");
        html.append("                    <h3>Memory Flags</h3>\n");
        html.append("                    <ul>\n");
        for (String flag : jvmFlags.memoryFlags) {
            html.append("                        <li>").append(flag).append("</li>\n");
        }
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"tab-section\">\n");
        html.append("                    <h3>Other Flags</h3>\n");
        html.append("                    <ul>\n");
        for (String flag : jvmFlags.otherFlags) {
            html.append("                        <li>").append(flag).append("</li>\n");
        }
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        html.append("            <div id=\"configurations-tab\" class=\"tab-content\">\n");
        me.testikgm.util.SystemInfo.ConfigInfo configInfo = me.testikgm.util.SystemInfo.getConfigInfo();
        html.append("                <div class=\"tab-section\">\n");
        html.append("                    <h3>Server Configuration</h3>\n");
        html.append("                    <ul>\n");
        for (String config : configInfo.configs) {
            html.append("                        <li>").append(config).append("</li>\n");
        }
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");

        html.append("            <div id=\"world-tab\" class=\"tab-content\">\n");
        ChunkScanner.WorldStats worldStats = chunkScanner.getWorldStats();
        html.append("                <div class=\"world-stats-grid\">\n");
        html.append("                    <div class=\"world-stat-column\">\n");
        int totalEntities = worldStats.entityCountsByType.values().stream().mapToInt(Integer::intValue).sum();
        html.append("                        <h3>Entities (total: ").append(totalEntities).append(")</h3>\n");
        for (Map.Entry<String, List<org.bukkit.entity.Entity>> entry : worldStats.entitiesByWorld.entrySet()) {
            html.append("                        <p>").append(entry.getKey()).append(": ").append(entry.getValue().size()).append("</p>\n");
        }
        html.append("                        <h4>Entity Counts</h4>\n");
        html.append("                        <ul class=\"entity-list\">\n");
        List<Map.Entry<String, Integer>> sortedEntities = new java.util.ArrayList<>(worldStats.entityCountsByType.entrySet());
        sortedEntities.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(20, sortedEntities.size()); i++) {
            Map.Entry<String, Integer> entry = sortedEntities.get(i);
            html.append("                            <li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>\n");
        }
        html.append("                        </ul>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"world-stat-column\">\n");
        html.append("                        <h3>Chunks (total: ").append(worldStats.chunkCountsByWorld.values().stream().mapToInt(Integer::intValue).sum()).append(")</h3>\n");
        for (Map.Entry<String, Integer> entry : worldStats.chunkCountsByWorld.entrySet()) {
            html.append("                        <p>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</p>\n");
        }
        html.append("                    </div>\n");
        html.append("                </div>\n");

        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            List<ChunkScanner.RegionData> regions = chunkScanner.getRegionData(world.getName());
            if (!regions.isEmpty()) {
                html.append("                <div class=\"region-section\">\n");
                html.append("                    <h3>").append(world.getName()).append(" - Regions</h3>\n");
                for (int i = 0; i < Math.min(regions.size(), 10); i++) {
                    ChunkScanner.RegionData region = regions.get(i);
                    html.append("                    <div class=\"region-item\">\n");
                    html.append("                        <h4>").append(region.getRegionString()).append("</h4>\n");
                    html.append("                        <p>Entities: ").append(region.totalEntities).append("</p>\n");
                    html.append("                        <p>Chunks: ").append(region.totalChunks).append("</p>\n");
                    html.append("                        <ul class=\"entity-list\">\n");
                    List<Map.Entry<String, Integer>> sortedRegionEntities = new java.util.ArrayList<>(region.entityCounts.entrySet());
                    sortedRegionEntities.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                    for (int j = 0; j < Math.min(10, sortedRegionEntities.size()); j++) {
                        Map.Entry<String, Integer> entry = sortedRegionEntities.get(j);
                        html.append("                            <li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>\n");
                    }
                    html.append("                        </ul>\n");
                    html.append("                        <details>\n");
                    html.append("                            <summary>Chunks</summary>\n");
                    html.append("                            <ul>\n");
                    for (int j = 0; j < Math.min(20, region.chunks.size()); j++) {
                        ChunkScanner.ChunkInfo chunk = region.chunks.get(j);
                        html.append("                                <li>").append(chunk.getLocationString()).append("</li>\n");
                    }
                    html.append("                            </ul>\n");
                    html.append("                        </details>\n");
                    html.append("                    </div>\n");
                }
                html.append("                </div>\n");
            }
        }
        
        html.append("            </div>\n");
        html.append("        </div>\n");
        return html.toString();
    }

    private static MemoryMonitor.MemoryLeakAnalysis analyzeMemoryLeakFromSnapshots(List<MetricsHistory.MetricsSnapshot> allSnapshots) {
        if (allSnapshots.size() < 10) {
            return new MemoryMonitor.MemoryLeakAnalysis(false, 0.0, 
                "Недостаточно данных для анализа (нужно минимум 10 снимков, доступно " + allSnapshots.size() + ")");
        }

        int startIndex = Math.max(0, allSnapshots.size() - Math.max(10, allSnapshots.size() / 2));
        List<MetricsHistory.MetricsSnapshot> recentSnapshots = allSnapshots.subList(startIndex, allSnapshots.size());

        double totalGrowth = 0.0;
        int growthCount = 0;
        for (int i = 1; i < recentSnapshots.size(); i++) {
            double prev = recentSnapshots.get(i - 1).memoryPercent;
            double curr = recentSnapshots.get(i).memoryPercent;
            if (curr > prev) {
                totalGrowth += (curr - prev);
                growthCount++;
            }
        }
        
        double avgGrowthPercent = growthCount > 0 ? totalGrowth / growthCount : 0.0;

        double firstPercent = recentSnapshots.get(0).memoryPercent;
        double lastPercent = recentSnapshots.get(recentSnapshots.size() - 1).memoryPercent;
        double totalChange = lastPercent - firstPercent;

        int growingSnapshots = 0;
        for (int i = 1; i < recentSnapshots.size(); i++) {
            if (recentSnapshots.get(i).memoryPercent > recentSnapshots.get(i - 1).memoryPercent) {
                growingSnapshots++;
            }
        }
        double growthRatio = (double) growingSnapshots / (recentSnapshots.size() - 1);

        boolean possibleLeak = (totalChange > 5.0 && avgGrowthPercent > 0.5 && growthRatio > 0.6) ||
                               (totalChange > 3.0 && growthRatio > 0.7) ||
                               (growthRatio > 0.8 && totalChange > 1.0);
        
        String diagnosis;
        if (possibleLeak) {
            diagnosis = String.format("⚠ Обнаружена возможная утечка памяти! " +
                "Рост: %.1f%% → %.1f%% (+%.1f%%) за %d снимков. " +
                "Средний рост: %.2f%% за снимок. " +
                "Рост в %.0f%% снимков.",
                firstPercent, lastPercent, totalChange, recentSnapshots.size() - 1,
                avgGrowthPercent, growthRatio * 100);
        } else if (totalChange > 2.0) {
            diagnosis = String.format("Память растёт умеренно: %.1f%% → %.1f%% (+%.1f%%)", 
                firstPercent, lastPercent, totalChange);
        } else {
            diagnosis = "Память стабильна: " + String.format("%.1f%% → %.1f%% (изменение: %.1f%%)", 
                firstPercent, lastPercent, totalChange);
        }
        
        return new MemoryMonitor.MemoryLeakAnalysis(possibleLeak, avgGrowthPercent, diagnosis);
    }
}
