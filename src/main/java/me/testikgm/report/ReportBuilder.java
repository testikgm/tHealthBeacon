package me.testikgm.report;

import me.testikgm.tHealthBeacon;
import me.testikgm.analysis.DiagnosisEngine;
import me.testikgm.analysis.HealthIndex;
import me.testikgm.analysis.RecommendationEngine;
import me.testikgm.metrics.*;
import me.testikgm.util.FileUtil;
import me.testikgm.util.MetricsHistory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ReportBuilder {
    
    private final tHealthBeacon plugin;
    
    public ReportBuilder(tHealthBeacon plugin) {
        this.plugin = plugin;
    }

    public String buildReport(java.util.Map<String, String> aiAnalysisMap) {

        MetricsHistory metricsHistory = plugin.getMetricsHistory();
        if (metricsHistory == null) {
            return "Ошибка: MetricsHistory не инициализирован";
        }

        List<MetricsHistory.MetricsSnapshot> allSnapshots = metricsHistory.loadAllJsonSnapshots();

        int minForReport = plugin.getConfig().getInt("collector.min_for_report", 10);
        if (allSnapshots.isEmpty()) {
            return "Ошибка: Нет доступных снимков для генерации отчёта";
        }
        
        if (allSnapshots.size() < minForReport) {
            return "Ошибка: Недостаточно снимков для генерации отчёта. Требуется минимум " + minForReport + ", доступно " + allSnapshots.size();
        }

        MetricsHistory.MetricsSnapshot lastSnapshot = allSnapshots.get(allSnapshots.size() - 1);

        MetricsHistory.MetricsSnapshot previousSnapshot = allSnapshots.size() > 1 ? 
            allSnapshots.get(allSnapshots.size() - 2) : null;

        TpsAnalyzer tpsAnalyzer = plugin.getTpsAnalyzer();
        MemoryMonitor memoryMonitor = plugin.getMemoryMonitor();
        PluginProfiler pluginProfiler = plugin.getPluginProfiler();
        ChunkScanner chunkScanner = plugin.getChunkScanner();
        DiskProbe diskProbe = plugin.getDiskProbe();
        NetworkMonitor networkMonitor = plugin.getNetworkMonitor();

        DiagnosisEngine diagnosisEngine = plugin.getDiagnosisEngine();
        RecommendationEngine recommendationEngine = plugin.getRecommendationEngine();
        HealthIndex healthIndex = plugin.getHealthIndex();
        
        DiagnosisEngine.Diagnosis diagnosis = diagnosisEngine.analyze();
        List<RecommendationEngine.Recommendation> recommendations = recommendationEngine.generateRecommendations();

        HealthIndex.ServerResilienceScore srs = healthIndex.calculateScore();
        if (lastSnapshot.srs > 0) {

            srs = new HealthIndex.ServerResilienceScore(lastSnapshot.srs, srs.interpretation, srs.components);
        }

        String format = plugin.getConfig().getString("report-format", "html");
        
        if ("html".equalsIgnoreCase(format)) {
            return ReportHTML.generate(tpsAnalyzer, memoryMonitor, pluginProfiler, 
                chunkScanner, diskProbe, networkMonitor, diagnosis, recommendations, srs, 
                lastSnapshot, previousSnapshot, allSnapshots, metricsHistory, aiAnalysisMap,
                plugin.getMsptMonitor(), plugin.getCpuMonitor(), plugin);
        } else {
            return ReportMarkdown.generate(tpsAnalyzer, memoryMonitor, pluginProfiler, 
                chunkScanner, diskProbe, networkMonitor, diagnosis, recommendations, srs);
        }
    }

    public File saveReport(java.util.Map<String, String> aiAnalysisMap) throws IOException {
        String report = buildReport(aiAnalysisMap);
        String format = plugin.getConfig().getString("report-format", "html");
        String extension = "html".equalsIgnoreCase(format) ? ".html" : ".md";

        String reportDir = plugin.getConfig().getString("report.folder", "plugins/tHealthBeacon/reports/");
        File reportDirectory = new File(reportDir);
        FileUtil.ensureDirectoryExists(reportDirectory.getAbsolutePath());

        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String fileName = dateFormat.format(new java.util.Date()) + extension;
        File reportFile = new File(reportDirectory, fileName);

        Files.write(Paths.get(reportFile.getAbsolutePath()), report.getBytes("UTF-8"));
        
        return reportFile;
    }

    public File saveReport() throws IOException {
        return saveReport(new java.util.HashMap<>());
    }

    @Deprecated
    public File saveReport(String aiAnalysis) throws IOException {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
            map.put("general", aiAnalysis);
        }
        return saveReport(map);
    }
}

