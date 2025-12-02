package me.testikgm.metrics;

import me.testikgm.tHealthBeacon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkMonitor {
    
    private final tHealthBeacon plugin;
    private final Map<UUID, PlayerNetworkData> playerData;
    private final List<NetworkSnapshot> networkHistory;
    private boolean running;
    
    public NetworkMonitor(tHealthBeacon plugin) {
        this.plugin = plugin;
        this.playerData = new ConcurrentHashMap<>();
        this.networkHistory = new ArrayList<>();
        this.running = false;
    }
    
    public void collect() {
        if (!running) return;
        
        long currentTime = System.currentTimeMillis();
        List<PlayerPingData> pings = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            int ping = getPlayerPing(player);
            UUID uuid = player.getUniqueId();
            
            PlayerNetworkData data = playerData.computeIfAbsent(uuid, k -> new PlayerNetworkData(uuid, player.getName()));
            data.addPing(ping, currentTime);
            
            pings.add(new PlayerPingData(player.getName(), ping));
        }

        if (!pings.isEmpty()) {
            double avgPing = pings.stream().mapToInt(p -> p.ping).average().orElse(0.0);
            int minPing = pings.stream().mapToInt(p -> p.ping).min().orElse(0);
            int maxPing = pings.stream().mapToInt(p -> p.ping).max().orElse(0);

            double variance = pings.stream()
                .mapToDouble(p -> Math.pow(p.ping - avgPing, 2))
                .average()
                .orElse(0.0);
            double jitter = Math.sqrt(variance);
            
            networkHistory.add(new NetworkSnapshot(currentTime, avgPing, minPing, maxPing, jitter, pings.size()));

            if (networkHistory.size() > 1000) {
                networkHistory.remove(0);
            }
        }
    }
    
    public void start() {
        running = true;
    }
    
    public void stop() {
        running = false;
    }

    private int getPlayerPing(Player player) {
        try {

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("connection").get(handle);
            return (int) connection.getClass().getMethod("getLatency").invoke(connection);
        } catch (Exception e) {

            return 0;
        }
    }

    public NetworkStabilityAnalysis analyzeStability() {
        if (networkHistory.size() < 10) {
            return new NetworkStabilityAnalysis(0.0, 0.0, false, "Недостаточно данных");
        }
        
        List<NetworkSnapshot> recent = networkHistory.subList(
            Math.max(0, networkHistory.size() - 10),
            networkHistory.size()
        );
        
        double avgJitter = recent.stream()
            .mapToDouble(s -> s.jitter)
            .average()
            .orElse(0.0);
        
        double avgPing = recent.stream()
            .mapToDouble(s -> s.avgPing)
            .average()
            .orElse(0.0);

        double jitterPercent = avgPing > 0 ? (avgJitter / avgPing) * 100 : 0;
        
        int threshold = plugin.getConfig().getInt("ping-jitter-threshold", 30);
        boolean hasIssue = jitterPercent > threshold;
        
        String diagnosis;
        if (hasIssue) {
            diagnosis = String.format("Нестабильная сеть: джиттер %.2f%% (порог: %d%%)", jitterPercent, threshold);
        } else {
            diagnosis = String.format("Сеть стабильна: джиттер %.2f%%", jitterPercent);
        }
        
        return new NetworkStabilityAnalysis(avgPing, jitterPercent, hasIssue, diagnosis);
    }

    public List<PlayerNetworkData> findBadConnections() {
        List<PlayerNetworkData> badConnections = new ArrayList<>();
        
        for (PlayerNetworkData data : playerData.values()) {
            if (data.getAvgPing() > 200 || data.getJitter() > 50) {
                badConnections.add(data);
            }
        }
        
        return badConnections;
    }
    
    public List<NetworkSnapshot> getNetworkHistory() {
        return new ArrayList<>(networkHistory);
    }

    public static class NetworkSnapshot {
        public final long timestamp;
        public final double avgPing;
        public final int minPing;
        public final int maxPing;
        public final double jitter;
        public final int playerCount;
        
        public NetworkSnapshot(long timestamp, double avgPing, int minPing, int maxPing, double jitter, int playerCount) {
            this.timestamp = timestamp;
            this.avgPing = avgPing;
            this.minPing = minPing;
            this.maxPing = maxPing;
            this.jitter = jitter;
            this.playerCount = playerCount;
        }
    }
    
    public static class PlayerPingData {
        public final String name;
        public final int ping;
        
        public PlayerPingData(String name, int ping) {
            this.name = name;
            this.ping = ping;
        }
    }
    
    public static class PlayerNetworkData {
        public final UUID uuid;
        public final String name;
        private final List<Integer> pings;
        private final List<Long> timestamps;
        
        public PlayerNetworkData(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
            this.pings = new ArrayList<>();
            this.timestamps = new ArrayList<>();
        }
        
        public void addPing(int ping, long timestamp) {
            pings.add(ping);
            timestamps.add(timestamp);

            if (pings.size() > 100) {
                pings.remove(0);
                timestamps.remove(0);
            }
        }
        
        public double getAvgPing() {
            if (pings.isEmpty()) return 0.0;
            return pings.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        }
        
        public double getJitter() {
            if (pings.size() < 2) return 0.0;
            double avg = getAvgPing();
            double variance = pings.stream()
                .mapToDouble(p -> Math.pow(p - avg, 2))
                .average()
                .orElse(0.0);
            return Math.sqrt(variance);
        }
    }
    
    public static class NetworkStabilityAnalysis {
        public final double avgPing;
        public final double jitterPercent;
        public final boolean hasIssue;
        public final String diagnosis;
        
        public NetworkStabilityAnalysis(double avgPing, double jitterPercent, boolean hasIssue, String diagnosis) {
            this.avgPing = avgPing;
            this.jitterPercent = jitterPercent;
            this.hasIssue = hasIssue;
            this.diagnosis = diagnosis;
        }
    }
}

