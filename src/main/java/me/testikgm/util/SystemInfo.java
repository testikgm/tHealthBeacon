package me.testikgm.util;

import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

public class SystemInfo {

    public static PlatformInfo getPlatformInfo() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");

        String serverVersion = Bukkit.getVersion();
        String serverImplementation = Bukkit.getServer().getClass().getPackage().getImplementationTitle();
        String serverImplementationVersion = Bukkit.getServer().getClass().getPackage().getImplementationVersion();
        
        return new PlatformInfo(
            javaVersion,
            javaVendor,
            osName,
            osVersion,
            osArch,
            serverVersion,
            serverImplementation,
            serverImplementationVersion
        );
    }

    public static JvmFlagsInfo getJvmFlags() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        List<String> inputArguments = runtimeBean.getInputArguments();
        
        List<String> gcFlags = new ArrayList<>();
        List<String> memoryFlags = new ArrayList<>();
        List<String> otherFlags = new ArrayList<>();
        
        for (String arg : inputArguments) {
            if (arg.contains("GC") || arg.contains("gc") || arg.contains("G1") || 
                arg.contains("UseG1GC") || arg.contains("UseConcMarkSweepGC")) {
                gcFlags.add(arg);
            } else if (arg.contains("Xmx") || arg.contains("Xms") || arg.contains("Xmn") ||
                      arg.contains("MaxMetaspaceSize") || arg.contains("MetaspaceSize")) {
                memoryFlags.add(arg);
            } else {
                otherFlags.add(arg);
            }
        }
        
        return new JvmFlagsInfo(gcFlags, memoryFlags, otherFlags);
    }

    public static ConfigInfo getConfigInfo() {

        List<String> configs = new ArrayList<>();
        
        try {

            Object server = Bukkit.getServer();

        } catch (Exception e) {

        }
        
        configs.add("view-distance: " + Bukkit.getViewDistance());
        configs.add("max-players: " + Bukkit.getMaxPlayers());
        configs.add("online-mode: " + Bukkit.getOnlineMode());
        configs.add("hardcore: " + Bukkit.isHardcore());
        
        return new ConfigInfo(configs);
    }
    
    public static class PlatformInfo {
        public final String javaVersion;
        public final String javaVendor;
        public final String osName;
        public final String osVersion;
        public final String osArch;
        public final String serverVersion;
        public final String serverImplementation;
        public final String serverImplementationVersion;
        
        public PlatformInfo(String javaVersion, String javaVendor, String osName, 
                           String osVersion, String osArch, String serverVersion,
                           String serverImplementation, String serverImplementationVersion) {
            this.javaVersion = javaVersion;
            this.javaVendor = javaVendor;
            this.osName = osName;
            this.osVersion = osVersion;
            this.osArch = osArch;
            this.serverVersion = serverVersion;
            this.serverImplementation = serverImplementation;
            this.serverImplementationVersion = serverImplementationVersion;
        }
    }
    
    public static class JvmFlagsInfo {
        public final List<String> gcFlags;
        public final List<String> memoryFlags;
        public final List<String> otherFlags;
        
        public JvmFlagsInfo(List<String> gcFlags, List<String> memoryFlags, List<String> otherFlags) {
            this.gcFlags = gcFlags;
            this.memoryFlags = memoryFlags;
            this.otherFlags = otherFlags;
        }
    }
    
    public static class ConfigInfo {
        public final List<String> configs;
        
        public ConfigInfo(List<String> configs) {
            this.configs = configs;
        }
    }
}

