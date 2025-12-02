package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MemoryMonitor {
    
    private final tHealthBeacon plugin;
    private final MemoryMXBean memoryBean;
    private final Queue<MemorySnapshot> memoryHistory;
    private long lastGcTime;
    private int gcCount;
    private boolean running;
    
    public MemoryMonitor(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.memoryHistory = new LinkedList<>();
        this.lastGcTime = System.currentTimeMillis();
        this.gcCount = 0;
        this.running = false;
    }
    
    public void collect() {
        if (!running) return;
        
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long currentTime = System.currentTimeMillis();
        
        int currentGcCount = getGcCount();
        if (currentGcCount > gcCount) {
            gcCount = currentGcCount;
            lastGcTime = currentTime;
        }
        
        MemorySnapshot snapshot = new MemorySnapshot(
            currentTime,
            heapUsage.getUsed(),
            heapUsage.getMax(),
            heapUsage.getCommitted(),
            currentGcCount
        );
        
        memoryHistory.offer(snapshot);
        
        while (memoryHistory.size() > 1000) {
            memoryHistory.poll();
        }
    }
    
    public void start() {
        running = true;
        gcCount = getGcCount();
    }
    
    public void stop() {
        running = false;
    }
    
    private int getGcCount() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToInt(gc -> (int) gc.getCollectionCount())
            .sum();
    }
    
    public MemoryLeakAnalysis analyzeMemoryLeak() {
        if (memoryHistory.size() < 10) {
            return new MemoryLeakAnalysis(false, 0.0, "Недостаточно данных");
        }
        
        List<MemorySnapshot> recent = new ArrayList<>(memoryHistory);
        int size = recent.size();
        
        int startIndex = (int) (size * 0.9);
        List<MemorySnapshot> recentSnapshots = recent.subList(startIndex, size);
        
        double growthRate = 0.0;
        for (int i = 1; i < recentSnapshots.size(); i++) {
            long prev = recentSnapshots.get(i - 1).used;
            long curr = recentSnapshots.get(i).used;
            if (curr > prev) {
                growthRate += (curr - prev);
            }
        }
        
        growthRate = growthRate / recentSnapshots.size();
        
        boolean gcRelief = false;
        long timeSinceLastGc = System.currentTimeMillis() - lastGcTime;
        
        if (timeSinceLastGc < 60000) {
            MemorySnapshot beforeGc = recentSnapshots.get(0);
            MemorySnapshot afterGc = recentSnapshots.get(recentSnapshots.size() - 1);
            
            if (afterGc.used < beforeGc.used * 0.9) {
                gcRelief = true;
            }
        }
        
        boolean possibleLeak = growthRate > 0 && !gcRelief && timeSinceLastGc > 300000;
        
        String diagnosis;
        if (possibleLeak) {
            diagnosis = String.format("Возможная утечка памяти: рост %.2f MB/снимок, GC не освобождает память", 
                growthRate / (1024.0 * 1024.0));
        } else if (growthRate > 0) {
            diagnosis = String.format("Память растёт, но GC работает нормально: %.2f MB/снимок", 
                growthRate / (1024.0 * 1024.0));
        } else {
            diagnosis = "Память стабильна";
        }
        
        return new MemoryLeakAnalysis(possibleLeak, growthRate, diagnosis);
    }
    
    public MemorySnapshot getCurrentSnapshot() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return new MemorySnapshot(
            System.currentTimeMillis(),
            heapUsage.getUsed(),
            heapUsage.getMax(),
            heapUsage.getCommitted(),
            getGcCount()
        );
    }
    
    public Queue<MemorySnapshot> getMemoryHistory() {
        return new LinkedList<>(memoryHistory);
    }
    
    public GcStats getGcStats() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        GcStats.GcInfo youngGc = null;
        GcStats.GcInfo oldGc = null;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName();
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            
            boolean isYoung = name.contains("Young") || name.contains("Eden") || 
                             name.contains("ParNew") || name.contains("Copy");
            boolean isOld = name.contains("Old") || name.contains("Tenured") || 
                           name.contains("MarkSweep") || name.contains("Full");
            
            if (isYoung) {
                double avgTime = count > 0 ? (double) time / count : 0.0;
                double avgFreq = count > 0 ? (double) (System.currentTimeMillis() - lastGcTime) / (count * 1000.0) : 0.0;
                youngGc = new GcStats.GcInfo(count, avgTime, avgFreq);
            } else if (isOld) {
                double avgTime = count > 0 ? (double) time / count : 0.0;
                double avgFreq = count > 0 ? (double) (System.currentTimeMillis() - lastGcTime) / (count * 1000.0) : 0.0;
                oldGc = new GcStats.GcInfo(count, avgTime, avgFreq);
            }
        }
        
        if (youngGc == null) {
            youngGc = new GcStats.GcInfo(0, 0.0, 0.0);
        }
        if (oldGc == null) {
            oldGc = new GcStats.GcInfo(0, 0.0, 0.0);
        }
        
        return new GcStats(youngGc, oldGc);
    }
    
    public PhysicalMemoryInfo getPhysicalMemoryInfo() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            
            long totalPhysical = osBean.getTotalPhysicalMemorySize();
            long freePhysical = osBean.getFreePhysicalMemorySize();
            long usedPhysical = totalPhysical - freePhysical;
            
            long totalSwap = osBean.getTotalSwapSpaceSize();
            long freeSwap = osBean.getFreeSwapSpaceSize();
            long usedSwap = totalSwap - freeSwap;
            
            return new PhysicalMemoryInfo(totalPhysical, usedPhysical, freePhysical, 
                                         totalSwap, usedSwap, freeSwap);
        } catch (Exception e) {
            Runtime runtime = Runtime.getRuntime();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            long used = total - free;
            
            return new PhysicalMemoryInfo(total, used, free, 0, 0, 0);
        }
    }
    
    public static class MemorySnapshot {
        public final long timestamp;
        public final long used;
        public final long max;
        public final long committed;
        public final int gcCount;
        
        public MemorySnapshot(long timestamp, long used, long max, long committed, int gcCount) {
            this.timestamp = timestamp;
            this.used = used;
            this.max = max;
            this.committed = committed;
            this.gcCount = gcCount;
        }
    }
    
    public static class MemoryLeakAnalysis {
        public final boolean possibleLeak;
        public final double growthRate;
        public final String diagnosis;
        
        public MemoryLeakAnalysis(boolean possibleLeak, double growthRate, String diagnosis) {
            this.possibleLeak = possibleLeak;
            this.growthRate = growthRate;
            this.diagnosis = diagnosis;
        }
    }
    
    public static class GcStats {
        public final GcInfo youngGc;
        public final GcInfo oldGc;
        
        public GcStats(GcInfo youngGc, GcInfo oldGc) {
            this.youngGc = youngGc;
            this.oldGc = oldGc;
        }
        
        public static class GcInfo {
            public final long total;
            public final double avgTime;
            public final double avgFreq;
            
            public GcInfo(long total, double avgTime, double avgFreq) {
                this.total = total;
                this.avgTime = avgTime;
                this.avgFreq = avgFreq;
            }
        }
    }
    
    public static class PhysicalMemoryInfo {
        public final long totalPhysical;
        public final long usedPhysical;
        public final long freePhysical;
        public final long totalSwap;
        public final long usedSwap;
        public final long freeSwap;
        
        public PhysicalMemoryInfo(long totalPhysical, long usedPhysical, long freePhysical,
                                 long totalSwap, long usedSwap, long freeSwap) {
            this.totalPhysical = totalPhysical;
            this.usedPhysical = usedPhysical;
            this.freePhysical = freePhysical;
            this.totalSwap = totalSwap;
            this.usedSwap = usedSwap;
            this.freeSwap = freeSwap;
        }
    }
}

