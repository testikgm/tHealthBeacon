package me.testikgm;

import me.testikgm.ai.OpenRouterClient;
import me.testikgm.analysis.DiagnosisEngine;
import me.testikgm.analysis.HealthIndex;
import me.testikgm.analysis.RecommendationEngine;
import me.testikgm.commands.HealthBeaconCommand;
import me.testikgm.metrics.*;
import me.testikgm.report.ReportBuilder;
import me.testikgm.util.MetricsHistory;
import org.bukkit.plugin.java.JavaPlugin;

public class tHealthBeacon extends JavaPlugin {
    
    private static tHealthBeacon instance;

    private TpsAnalyzer tpsAnalyzer;
    private MemoryMonitor memoryMonitor;
    private PluginProfiler pluginProfiler;
    private ChunkScanner chunkScanner;
    private DiskProbe diskProbe;
    private NetworkMonitor networkMonitor;
    private MsptMonitor msptMonitor;
    private CpuMonitor cpuMonitor;

    private DiagnosisEngine diagnosisEngine;
    private RecommendationEngine recommendationEngine;
    private HealthIndex healthIndex;

    private ReportBuilder reportBuilder;

    private MetricsHistory metricsHistory;

    private OpenRouterClient openRouterClient;
    
    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();

        initializeMetrics();

        initializeAnalysis();

        reportBuilder = new ReportBuilder(this);

        metricsHistory = new MetricsHistory(this);

        openRouterClient = new OpenRouterClient(this);

        getCommand("healthbeacon").setExecutor(new HealthBeaconCommand(this));
        getCommand("hbreport").setExecutor(new HealthBeaconCommand(this));

        startMetricsCollection();
        
        getLogger().info("tHealthBeacon v" + getDescription().getVersion() + " успешно загружен!");
        getLogger().info("Используйте /healthbeacon для анализа производительности сервера");
    }
    
    @Override
    public void onDisable() {

        if (tpsAnalyzer != null) tpsAnalyzer.stop();
        if (memoryMonitor != null) memoryMonitor.stop();
        if (networkMonitor != null) networkMonitor.stop();
        if (msptMonitor != null) msptMonitor.stop();
        if (cpuMonitor != null) cpuMonitor.stop();
        
        getLogger().info("tHealthBeacon отключен.");
    }
    
    private void initializeMetrics() {
        tpsAnalyzer = new TpsAnalyzer(this);
        memoryMonitor = new MemoryMonitor(this);
        pluginProfiler = new PluginProfiler(this);
        chunkScanner = new ChunkScanner(this);
        diskProbe = new DiskProbe(this);
        networkMonitor = new NetworkMonitor(this);
        msptMonitor = new MsptMonitor(this);
        cpuMonitor = new CpuMonitor(this);
    }
    
    private void initializeAnalysis() {
        diagnosisEngine = new DiagnosisEngine(this);
        recommendationEngine = new RecommendationEngine(this);
        healthIndex = new HealthIndex(this);
    }
    
    private void startMetricsCollection() {

        tpsAnalyzer.start();
        memoryMonitor.start();
        pluginProfiler.start();
        chunkScanner.start();
        diskProbe.start();
        networkMonitor.start();
        msptMonitor.start();
        cpuMonitor.start();
        
        int interval = getConfig().getInt("metrics-interval", 100);

        int intervalMinutes = getConfig().getInt("collector.interval_minutes", 5);
        int saveInterval = intervalMinutes * 60 * 20;
        getServer().getScheduler().runTaskTimer(this, () -> {
            metricsHistory.saveSnapshot();
        }, saveInterval, saveInterval);
        
        getServer().getScheduler().runTaskTimer(this, () -> {
            tpsAnalyzer.collect();
            memoryMonitor.collect();
            pluginProfiler.collect();
            chunkScanner.scan();
            diskProbe.probe();
            networkMonitor.collect();
            cpuMonitor.collect();

        }, interval, interval);
    }
    
    public static tHealthBeacon getInstance() {
        return instance;
    }

    public TpsAnalyzer getTpsAnalyzer() { return tpsAnalyzer; }
    public MemoryMonitor getMemoryMonitor() { return memoryMonitor; }
    public PluginProfiler getPluginProfiler() { return pluginProfiler; }
    public ChunkScanner getChunkScanner() { return chunkScanner; }
    public DiskProbe getDiskProbe() { return diskProbe; }
    public NetworkMonitor getNetworkMonitor() { return networkMonitor; }
    public MsptMonitor getMsptMonitor() { return msptMonitor; }
    public CpuMonitor getCpuMonitor() { return cpuMonitor; }

    public DiagnosisEngine getDiagnosisEngine() { return diagnosisEngine; }
    public RecommendationEngine getRecommendationEngine() { return recommendationEngine; }
    public HealthIndex getHealthIndex() { return healthIndex; }

    public ReportBuilder getReportBuilder() { return reportBuilder; }

    public MetricsHistory getMetricsHistory() { return metricsHistory; }

    public OpenRouterClient getOpenRouterClient() { return openRouterClient; }
}

