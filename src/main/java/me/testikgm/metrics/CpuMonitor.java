package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CpuMonitor {
    
    private final tHealthBeacon plugin;
    private final OperatingSystemMXBean osBean;
    private final Queue<CpuSnapshot> cpuHistory;
    private boolean running;
    private long lastProcessCpuTime;
    private long lastSystemTime;
    
    public CpuMonitor(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.cpuHistory = new LinkedList<>();
        this.running = false;
        this.lastProcessCpuTime = getProcessCpuTime();
        this.lastSystemTime = System.nanoTime();
    }
    
    public void start() {
        running = true;
    }
    
    public void stop() {
        running = false;
    }
    
    public void collect() {
        if (!running) return;
        
        long currentProcessCpuTime = getProcessCpuTime();
        long currentSystemTime = System.nanoTime();
        
        long processCpuDelta = currentProcessCpuTime - lastProcessCpuTime;
        long systemTimeDelta = currentSystemTime - lastSystemTime;
        
        double processCpuPercent = 0.0;
        if (systemTimeDelta > 0) {
            processCpuPercent = (processCpuDelta * 100.0) / systemTimeDelta;
            processCpuPercent = Math.min(100.0, processCpuPercent);
        }
        
        double systemCpuPercent = 0.0;
        try {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            @SuppressWarnings("deprecation")
            double systemLoad = sunOsBean.getSystemCpuLoad();
            systemCpuPercent = systemLoad * 100.0;
            if (systemCpuPercent < 0) {
                systemCpuPercent = osBean.getSystemLoadAverage();
                if (systemCpuPercent < 0) {
                    systemCpuPercent = 0.0;
                }
            }
        } catch (Exception e) {

            systemCpuPercent = osBean.getSystemLoadAverage();
            if (systemCpuPercent < 0) {
                systemCpuPercent = 0.0;
            }
        }
        
        cpuHistory.offer(new CpuSnapshot(
            System.currentTimeMillis(),
            systemCpuPercent,
            processCpuPercent
        ));
        
        lastProcessCpuTime = currentProcessCpuTime;
        lastSystemTime = currentSystemTime;

        while (cpuHistory.size() > 1000) {
            cpuHistory.poll();
        }
    }

    private long getProcessCpuTime() {
        try {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            return sunOsBean.getProcessCpuTime();
        } catch (Exception e) {

            return System.nanoTime();
        }
    }

    public CpuStats getStats(int windowMinutes) {
        if (cpuHistory.isEmpty()) {
            return new CpuStats(0.0, 0.0, 0.0, 0.0);
        }
        
        long windowMs = windowMinutes * 60 * 1000L;
        long currentTime = System.currentTimeMillis();
        
        List<CpuSnapshot> windowData = new ArrayList<>();
        for (CpuSnapshot snapshot : cpuHistory) {
            if (currentTime - snapshot.timestamp <= windowMs) {
                windowData.add(snapshot);
            }
        }
        
        if (windowData.isEmpty()) {
            return new CpuStats(0.0, 0.0, 0.0, 0.0);
        }
        
        double systemAvg = windowData.stream()
            .mapToDouble(s -> s.systemCpuPercent)
            .average()
            .orElse(0.0);
        
        double processAvg = windowData.stream()
            .mapToDouble(s -> s.processCpuPercent)
            .average()
            .orElse(0.0);
        
        double systemMax = windowData.stream()
            .mapToDouble(s -> s.systemCpuPercent)
            .max()
            .orElse(0.0);
        
        double processMax = windowData.stream()
            .mapToDouble(s -> s.processCpuPercent)
            .max()
            .orElse(0.0);
        
        return new CpuStats(systemAvg, processAvg, systemMax, processMax);
    }
    
    public CpuSnapshot getCurrentSnapshot() {
        if (cpuHistory.isEmpty()) {
            return new CpuSnapshot(System.currentTimeMillis(), 0.0, 0.0);
        }
        return cpuHistory.peek();
    }
    
    public Queue<CpuSnapshot> getCpuHistory() {
        return new LinkedList<>(cpuHistory);
    }
    
    public static class CpuSnapshot {
        public final long timestamp;
        public final double systemCpuPercent;
        public final double processCpuPercent;
        
        public CpuSnapshot(long timestamp, double systemCpuPercent, double processCpuPercent) {
            this.timestamp = timestamp;
            this.systemCpuPercent = systemCpuPercent;
            this.processCpuPercent = processCpuPercent;
        }
    }
    
    public static class CpuStats {
        public final double systemAvg;
        public final double processAvg;
        public final double systemMax;
        public final double processMax;
        
        public CpuStats(double systemAvg, double processAvg, double systemMax, double processMax) {
            this.systemAvg = systemAvg;
            this.processAvg = processAvg;
            this.systemMax = systemMax;
            this.processMax = processMax;
        }
    }
}

