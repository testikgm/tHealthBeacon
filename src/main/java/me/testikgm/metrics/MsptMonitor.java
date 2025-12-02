package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MsptMonitor {
    
    private final tHealthBeacon plugin;
    private final Queue<Double> msptHistory;
    private final Queue<Long> tickTimings;
    private long lastTickStart;

    private double minMspt = Double.MAX_VALUE;
    private double maxMspt = 0.0;
    private final List<Double> recentMspt = new ArrayList<>();
    
    public MsptMonitor(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.msptHistory = new ConcurrentLinkedQueue<>();
        this.tickTimings = new ConcurrentLinkedQueue<>();
    }
    
    public void start() {

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            lastTickStart = System.nanoTime();
        }, 0L, 1L);

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (lastTickStart > 0) {
                long tickTime = (System.nanoTime() - lastTickStart) / 1_000_000;
                double mspt = tickTime / 1.0;
                
                msptHistory.offer(mspt);
                tickTimings.offer(System.currentTimeMillis());

                minMspt = Math.min(minMspt, mspt);
                maxMspt = Math.max(maxMspt, mspt);
                
                recentMspt.add(mspt);
                if (recentMspt.size() > 1000) {
                    recentMspt.remove(0);
                }

                while (msptHistory.size() > 6000) {
                    msptHistory.poll();
                    tickTimings.poll();
                }
            }
        }, 1L, 1L);
    }
    
    public void stop() {

    }
    
    public void collect() {

    }

    public MsptStats getStats() {
        if (recentMspt.isEmpty()) {
            return new MsptStats(0.0, 0.0, 0.0, 0.0);
        }
        
        List<Double> sorted = new ArrayList<>(recentMspt);
        Collections.sort(sorted);
        
        double min = sorted.get(0);
        double median = sorted.get(sorted.size() / 2);
        double p95 = sorted.get((int) (sorted.size() * 0.95));
        double max = sorted.get(sorted.size() - 1);
        
        return new MsptStats(min, median, p95, max);
    }
    
    public Queue<Double> getMsptHistory() {
        return new LinkedList<>(msptHistory);
    }
    
    public static class MsptStats {
        public final double min;
        public final double median;
        public final double p95;
        public final double max;
        
        public MsptStats(double min, double median, double p95, double max) {
            this.min = min;
            this.median = median;
            this.p95 = p95;
            this.max = max;
        }
    }
}

