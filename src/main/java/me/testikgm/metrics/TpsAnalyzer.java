package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TpsAnalyzer {
    
    private final tHealthBeacon plugin;
    private final Queue<Double> tpsHistory;
    private final Queue<Long> timestamps;
    private final int maxHistorySize;
    private boolean running;

    private static final int WINDOW_5MIN = 5 * 60 * 20;
    private static final int WINDOW_10MIN = 10 * 60 * 20;
    private static final int WINDOW_30MIN = 30 * 60 * 20;
    
    public TpsAnalyzer(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.tpsHistory = new LinkedList<>();
        this.timestamps = new LinkedList<>();
        this.maxHistorySize = WINDOW_30MIN;
        this.running = false;
    }
    
    public void collect() {
        if (!running) return;

        double currentTps = getCurrentTps();
        long currentTime = System.currentTimeMillis();
        
        tpsHistory.offer(currentTps);
        timestamps.offer(currentTime);

        while (tpsHistory.size() > maxHistorySize) {
            tpsHistory.poll();
            timestamps.poll();
        }
    }
    
    public void start() {
        running = true;
    }
    
    public void stop() {
        running = false;
    }

    private double getCurrentTps() {
        try {

            if (Bukkit.getServer() instanceof org.bukkit.Server) {

                Object server = Bukkit.getServer();
                try {
                    java.lang.reflect.Method getTPS = server.getClass().getMethod("getTPS");
                    double[] tps = (double[]) getTPS.invoke(server);
                    return tps[0];
                } catch (Exception e) {

                    return 20.0;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось получить TPS: " + e.getMessage());
        }
        return 20.0;
    }

    public TpsTrend analyzeTrend(int windowMinutes) {
        if (tpsHistory.isEmpty()) {
            return new TpsTrend(20.0, 20.0, 20.0, 0.0, "Недостаточно данных");
        }
        
        List<Double> windowData = getWindowData(windowMinutes);
        if (windowData.isEmpty()) {
            return new TpsTrend(20.0, 20.0, 20.0, 0.0, "Недостаточно данных");
        }
        
        double avg = windowData.stream().mapToDouble(Double::doubleValue).average().orElse(20.0);
        double min = windowData.stream().mapToDouble(Double::doubleValue).min().orElse(20.0);
        double max = windowData.stream().mapToDouble(Double::doubleValue).max().orElse(20.0);

        double variance = windowData.stream()
            .mapToDouble(x -> Math.pow(x - avg, 2))
            .average()
            .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        String stability;
        if (stdDev < 0.5) {
            stability = "Очень стабильный";
        } else if (stdDev < 1.0) {
            stability = "Стабильный";
        } else if (stdDev < 2.0) {
            stability = "Умеренно нестабильный";
        } else {
            stability = "Нестабильный";
        }
        
        return new TpsTrend(avg, min, max, stdDev, stability);
    }

    public List<TpsDrop> detectDrops(double threshold) {
        List<TpsDrop> drops = new ArrayList<>();
        List<Double> history = new ArrayList<>(tpsHistory);
        
        for (int i = 1; i < history.size(); i++) {
            double prev = history.get(i - 1);
            double current = history.get(i);
            
            if (prev > threshold && current < threshold) {

                drops.add(new TpsDrop(i, prev, current, prev - current));
            }
        }
        
        return drops;
    }

    private List<Double> getWindowData(int windowMinutes) {
        long windowMs = windowMinutes * 60 * 1000L;
        long currentTime = System.currentTimeMillis();
        List<Double> result = new ArrayList<>();
        
        List<Double> history = new ArrayList<>(tpsHistory);
        List<Long> times = new ArrayList<>(timestamps);
        
        for (int i = times.size() - 1; i >= 0; i--) {
            if (currentTime - times.get(i) <= windowMs) {
                result.add(0, history.get(i));
            } else {
                break;
            }
        }
        
        return result;
    }
    
    public double getCurrentTpsValue() {
        return getCurrentTps();
    }
    
    public Queue<Double> getTpsHistory() {
        return new LinkedList<>(tpsHistory);
    }

    public static class TpsTrend {
        public final double average;
        public final double min;
        public final double max;
        public final double stdDev;
        public final String stability;
        
        public TpsTrend(double average, double min, double max, double stdDev, String stability) {
            this.average = average;
            this.min = min;
            this.max = max;
            this.stdDev = stdDev;
            this.stability = stability;
        }
    }
    
    public static class TpsDrop {
        public final int index;
        public final double before;
        public final double after;
        public final double drop;
        
        public TpsDrop(int index, double before, double after, double drop) {
            this.index = index;
            this.before = before;
            this.after = after;
            this.drop = drop;
        }
    }
}

