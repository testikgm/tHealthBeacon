package me.testikgm.ai;

import me.testikgm.tHealthBeacon;
import me.testikgm.util.JsonUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class OpenRouterClient {
    
    private final tHealthBeacon plugin;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final boolean enabled;
    
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    
    public OpenRouterClient(tHealthBeacon plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        
        this.enabled = config.getBoolean("openrouter.enabled", false);
        this.apiKey = config.getString("openrouter.api_key", "");
        this.model = config.getString("openrouter.model", "gpt-4o-mini");
        this.temperature = config.getDouble("openrouter.temperature", 0.3);
        this.maxTokens = config.getInt("openrouter.max_tokens", 1000);
    }

    public String analyzeMetric(String metricType, String metricData) {
        if (!enabled || apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENROUTER_API_KEY_HERE")) {
            return null;
        }
        
        try {
            String prompt = buildMetricPrompt(metricType, metricData);
            
            String requestBody = JsonUtil.toJson(Map.of(
                "model", model,
                "messages", new Map[]{
                    Map.of("role", "user", "content", prompt)
                },
                "temperature", temperature,
                "max_tokens", maxTokens
            ));
            
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                plugin.getLogger().warning("OpenRouter API вернул код 429 (Rate Limit - слишком много запросов).");

                try {
                    java.io.BufferedReader errorReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    if (errorResponse.length() > 0) {
                        plugin.getLogger().warning("Детали ошибки: " + errorResponse.toString());
                    }
                } catch (Exception e) {

                }
                throw new RuntimeException("429 Rate Limit");
            } else if (responseCode == 401) {
                plugin.getLogger().severe("OpenRouter API вернул код 401 (Unauthorized). Проверьте API ключ в config.yml!");
                throw new RuntimeException("401 Unauthorized - неверный API ключ");
            } else if (responseCode != 200) {
                plugin.getLogger().warning("OpenRouter API вернул код: " + responseCode);
                return null;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseStr = response.toString();
            int contentIndex = responseStr.indexOf("\"content\":\"");
            if (contentIndex > 0) {
                int start = contentIndex + 11;
                int end = responseStr.indexOf("\"", start);
                if (end > start) {
                    String content = responseStr.substring(start, end);

                    content = content.replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t");
                    return content;
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при запросе к OpenRouter для метрики " + metricType + ": " + e.getMessage());
            return null;
        }
    }

    public String analyzeMetricWithHistory(String metricType, String currentMetricData, 
                                            List<Map<String, Object>> history) {
        if (!enabled || apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENROUTER_API_KEY_HERE")) {
            return null;
        }
        
        try {

            StringBuilder historyStr = new StringBuilder();
            if (!history.isEmpty()) {
                historyStr.append("История изменений (").append(history.size()).append(" снимков, от старого к новому):\n");

                for (int i = 0; i < history.size(); i++) {
                    Map<String, Object> point = history.get(i);
                    Object data = point.get("data");
                    if (data != null) {
                        historyStr.append(i + 1).append(". ").append(data.toString());
                        if (i < history.size() - 1) {
                            historyStr.append("\n");
                        }
                    }
                }
            }
            
            String prompt = buildMetricPromptWithHistory(metricType, currentMetricData, historyStr.toString());
            
            String requestBody = JsonUtil.toJson(Map.of(
                "model", model,
                "messages", new Map[]{
                    Map.of("role", "user", "content", prompt)
                },
                "temperature", temperature,
                "max_tokens", maxTokens
            ));
            
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                plugin.getLogger().warning("OpenRouter API вернул код 429 (Rate Limit - слишком много запросов).");

                try {
                    java.io.BufferedReader errorReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    if (errorResponse.length() > 0) {
                        plugin.getLogger().warning("Детали ошибки: " + errorResponse.toString());
                    }
                } catch (Exception e) {

                }
                throw new RuntimeException("429 Rate Limit");
            } else if (responseCode == 401) {
                plugin.getLogger().severe("OpenRouter API вернул код 401 (Unauthorized). Проверьте API ключ в config.yml!");
                throw new RuntimeException("401 Unauthorized - неверный API ключ");
            } else if (responseCode != 200) {
                plugin.getLogger().warning("OpenRouter API вернул код: " + responseCode);
                return null;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseStr = response.toString();
            int contentIndex = responseStr.indexOf("\"content\":\"");
            if (contentIndex > 0) {
                int start = contentIndex + 11;
                int end = responseStr.indexOf("\"", start);
                if (end > start) {
                    String content = responseStr.substring(start, end);

                    content = content.replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t");
                    return content;
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при запросе к OpenRouter для метрики " + metricType + " с историей: " + e.getMessage());
            return null;
        }
    }

    private String buildMetricPromptWithHistory(String metricType, String currentMetricData, String historyData) {
        switch (metricType.toLowerCase()) {
            case "tps":
                return "Проанализируй метрики TPS (Ticks Per Second) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд (улучшается/ухудшается/стабильно)" +
                       "\n- Если есть проблема, укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "memory":
                return "Проанализируй метрики памяти (Memory) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд использования памяти" +
                       "\n- Если есть проблема (утечка, высокое использование, рост), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "disk":
                return "Проанализируй метрики диска (Disk) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд использования диска" +
                       "\n- Если есть проблема (мало места, высокая задержка, быстрый рост), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "network":
                return "Проанализируй метрики сети (Network) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд сетевых показателей" +
                       "\n- Если есть проблема (высокий пинг, джиттер, нестабильность), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "cpu":
                return "Проанализируй метрики CPU Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд нагрузки CPU" +
                       "\n- Если есть проблема (высокая нагрузка, рост), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "gc":
                return "Проанализируй метрики сборки мусора (GC) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд частоты и времени GC" +
                       "\n- Если есть проблема (частая сборка, долгое время, рост), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "mspt":
                return "Проанализируй метрики MSPT (Milliseconds Per Tick) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд MSPT (идеально < 50ms)" +
                       "\n- Если есть проблема (высокий MSPT, рост, нестабильность), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "chunks":
                return "Проанализируй метрики чанков (Chunks) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд загруженных чанков и сущностей" +
                       "\n- Если есть проблема (слишком много чанков, рост сущностей, нестабильность), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "plugins":
                return "Проанализируй метрики плагинов (Plugins) Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Проанализируй тренд времени загрузки плагинов и проблем" +
                       "\n- Если есть проблема (медленная загрузка, ошибки, зависимости), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            default:
                return "Проанализируй метрики " + metricType + " Minecraft сервера.\n\n" +
                       "Текущее состояние: " + currentMetricData + "\n\n" +
                       historyData + "\n\n" +
                       "Верни ТОЛЬКО краткий анализ на русском языке (2-3 предложения) с анализом тренда, указанием проблем и способов их решения, если они есть.";
        }
    }

    private String buildMetricPrompt(String metricType, String metricData) {
        switch (metricType.toLowerCase()) {
            case "tps":
                return "Проанализируй метрики TPS (Ticks Per Second) Minecraft сервера из JSON: " + metricData + 
                       "\n\nВерни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Если есть проблема, укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "memory":
                return "Проанализируй метрики памяти (Memory) Minecraft сервера из JSON: " + metricData + 
                       "\n\nВерни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Если есть проблема (утечка, высокое использование), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "disk":
                return "Проанализируй метрики диска (Disk) Minecraft сервера из JSON: " + metricData + 
                       "\n\nВерни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Если есть проблема (мало места, высокая задержка), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "network":
                return "Проанализируй метрики сети (Network) Minecraft сервера из JSON: " + metricData + 
                       "\n\nВерни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Если есть проблема (высокий пинг, джиттер), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "cpu":
                return "Проанализируй метрики CPU Minecraft сервера из JSON: " + metricData + 
                       "\n\nВерни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Если есть проблема (высокая нагрузка), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            case "gc":
                return "Проанализируй метрики сборки мусора (GC) Minecraft сервера из JSON: " + metricData + 
                       "\n\nВерни ТОЛЬКО краткий анализ на русском языке (2-3 предложения):" +
                       "\n- Если есть проблема (частая сборка, долгое время), укажи причину и как исправить" +
                       "\n- Если всё нормально, просто кратко подтверди" +
                       "\n- НЕ повторяй цифры, которые уже показаны" +
                       "\n- Без заголовков и форматирования, только текст";
            
            default:
                return "Проанализируй метрики " + metricType + " Minecraft сервера из JSON: " + metricData + 
                       "\n\nВерни ТОЛЬКО краткий анализ на русском языке (2-3 предложения) с указанием проблем и способов их решения, если они есть.";
        }
    }

    public String analyzeReport(String jsonReport) {
        if (!enabled || apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENROUTER_API_KEY_HERE")) {
            plugin.getLogger().warning("OpenRouter не настроен. Пропускаем AI-анализ.");
            return null;
        }
        
        try {
            String prompt = "Проанализируй следующий JSON отчёт о производительности Minecraft сервера и предоставь краткое резюме на русском языке с рекомендациями:\n\n" + jsonReport;
            
            String requestBody = JsonUtil.toJson(Map.of(
                "model", model,
                "messages", new Map[]{
                    Map.of("role", "user", "content", prompt)
                },
                "temperature", temperature,
                "max_tokens", maxTokens
            ));
            
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                plugin.getLogger().warning("OpenRouter API вернул код 429 (Rate Limit - слишком много запросов).");

                try {
                    java.io.BufferedReader errorReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    if (errorResponse.length() > 0) {
                        plugin.getLogger().warning("Детали ошибки: " + errorResponse.toString());
                    }
                } catch (Exception e) {

                }
                throw new RuntimeException("429 Rate Limit");
            } else if (responseCode == 401) {
                plugin.getLogger().severe("OpenRouter API вернул код 401 (Unauthorized). Проверьте API ключ в config.yml!");
                throw new RuntimeException("401 Unauthorized - неверный API ключ");
            } else if (responseCode != 200) {
                plugin.getLogger().warning("OpenRouter API вернул код: " + responseCode);
                return null;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseStr = response.toString();
            int contentIndex = responseStr.indexOf("\"content\":\"");
            if (contentIndex > 0) {
                int start = contentIndex + 11;
                int end = responseStr.indexOf("\"", start);
                if (end > start) {
                    String content = responseStr.substring(start, end);

                    content = content.replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t");
                    return content;
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при запросе к OpenRouter: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String compareReports(String report1, String report2) {
        if (!enabled || apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENROUTER_API_KEY_HERE")) {
            return null;
        }
        
        try {
            String prompt = "Сравни два отчёта о производительности Minecraft сервера и предоставь анализ изменений на русском языке:\n\nОтчёт 1:\n" + report1 + "\n\nОтчёт 2:\n" + report2;
            
            String requestBody = JsonUtil.toJson(Map.of(
                "model", model,
                "messages", new Map[]{
                    Map.of("role", "user", "content", prompt)
                },
                "temperature", temperature,
                "max_tokens", maxTokens
            ));
            
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseStr = response.toString();
            int contentIndex = responseStr.indexOf("\"content\":\"");
            if (contentIndex > 0) {
                int start = contentIndex + 11;
                int end = responseStr.indexOf("\"", start);
                if (end > start) {
                    String content = responseStr.substring(start, end);

                    content = content.replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t");
                    return content;
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при сравнении отчётов: " + e.getMessage());
            return null;
        }
    }

    public java.util.Map<String, String> analyzeAllMetrics(java.util.Map<String, Object> currentMetrics,
                                                           java.util.Map<String, java.util.List<java.util.Map<String, Object>>> historyMap) {
        if (!enabled || apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENROUTER_API_KEY_HERE")) {
            return new java.util.HashMap<>();
        }
        
        try {

            StringBuilder prompt = new StringBuilder();
            prompt.append("Проанализируй ВСЕ метрики Minecraft сервера и верни анализ для каждой метрики.\n\n");
            prompt.append("ТЕКУЩИЕ МЕТРИКИ:\n");
            prompt.append(me.testikgm.util.JsonUtil.toJson(currentMetrics));
            prompt.append("\n\n");
            
            prompt.append("ИСТОРИЯ ИЗМЕНЕНИЙ:\n");
            for (java.util.Map.Entry<String, java.util.List<java.util.Map<String, Object>>> entry : historyMap.entrySet()) {
                String metricName = entry.getKey();
                java.util.List<java.util.Map<String, Object>> history = entry.getValue();
                if (history != null && !history.isEmpty()) {
                    prompt.append("\n").append(metricName.toUpperCase()).append(" (").append(history.size()).append(" снимков):\n");
                    for (int i = 0; i < Math.min(history.size(), 10); i++) {
                        java.util.Map<String, Object> point = history.get(i);
                        Object data = point.get("data");
                        if (data != null) {
                            prompt.append(i + 1).append(". ").append(data.toString()).append("\n");
                        }
                    }
                }
            }
            
            prompt.append("\n\nВЕРНИ ОТВЕТ В СТРОГОМ JSON ФОРМАТЕ (без markdown, без ```json):\n");
            prompt.append("{\n");
            prompt.append("  \"tps\": \"краткий анализ TPS (2-3 предложения)\",\n");
            prompt.append("  \"memory\": \"краткий анализ Memory (2-3 предложения)\",\n");
            prompt.append("  \"disk\": \"краткий анализ Disk (2-3 предложения)\",\n");
            prompt.append("  \"network\": \"краткий анализ Network (2-3 предложения)\",\n");
            prompt.append("  \"cpu\": \"краткий анализ CPU (2-3 предложения)\",\n");
            prompt.append("  \"gc\": \"краткий анализ GC (2-3 предложения)\",\n");
            prompt.append("  \"mspt\": \"краткий анализ MSPT (2-3 предложения)\",\n");
            prompt.append("  \"chunks\": \"краткий анализ Chunks (2-3 предложения)\",\n");
            prompt.append("  \"plugins\": \"краткий анализ Plugins (2-3 предложения)\"\n");
            prompt.append("}\n\n");
            prompt.append("ТРЕБОВАНИЯ:\n");
            prompt.append("- Анализ на русском языке\n");
            prompt.append("- Для каждой метрики: проанализируй тренд, укажи проблемы (если есть) и как исправить\n");
            prompt.append("- Если всё нормально, просто кратко подтверди\n");
            prompt.append("- НЕ повторяй цифры, которые уже показаны\n");
            prompt.append("- Только JSON, без дополнительного текста\n");
            
            String requestBody = JsonUtil.toJson(Map.of(
                "model", model,
                "messages", new Map[]{
                    Map.of("role", "user", "content", prompt.toString())
                },
                "temperature", temperature,
                "max_tokens", maxTokens * 2
            ));
            
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                plugin.getLogger().warning("OpenRouter API вернул код 429 (Rate Limit - слишком много запросов).");
                throw new RuntimeException("429 Rate Limit");
            } else if (responseCode == 401) {
                plugin.getLogger().severe("OpenRouter API вернул код 401 (Unauthorized). Проверьте API ключ в config.yml!");
                throw new RuntimeException("401 Unauthorized - неверный API ключ");
            } else if (responseCode != 200) {
                plugin.getLogger().warning("OpenRouter API вернул код: " + responseCode);
                return new java.util.HashMap<>();
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseStr = response.toString();

            String content = null;

            int contentIndex = responseStr.indexOf("\"content\":\"");
            if (contentIndex > 0) {
                int start = contentIndex + 11;

                int end = start;
                boolean escaped = false;
                int depth = 0;
                while (end < responseStr.length()) {
                    char c = responseStr.charAt(end);
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                    } else if (c == '"' && !escaped && depth == 0) {

                        break;
                    }
                    end++;
                }
                if (end > start) {
                    content = responseStr.substring(start, end);

                    content = content.replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t");
                }
            }

            if (content == null || content.isEmpty()) {
                int contentIndex2 = responseStr.indexOf("\"content\":");
                if (contentIndex2 > 0) {

                    int jsonStart = responseStr.indexOf("{", contentIndex2);
                    if (jsonStart > 0) {

                        int jsonEnd = findMatchingBrace(responseStr, jsonStart);
                        if (jsonEnd > jsonStart) {
                            content = responseStr.substring(jsonStart, jsonEnd + 1);
                        }
                    } else {

                        int start = responseStr.indexOf("\"", contentIndex2 + 10) + 1;
                        if (start > 0) {
                            int end = responseStr.indexOf("\"", start);
                            if (end > start) {
                                content = responseStr.substring(start, end);
                            }
                        }
                    }
                }
            }
            
            if (content != null && !content.isEmpty()) {

                return parseMetricsJsonResponse(content);
            }
            
            return new java.util.HashMap<>();
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при анализе всех метрик: " + e.getMessage());
            e.printStackTrace();
            return new java.util.HashMap<>();
        }
    }

    private int findMatchingBrace(String json, int start) {
        int depth = 1;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private java.util.Map<String, String> parseMetricsJsonResponse(String jsonContent) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        
        try {

            jsonContent = jsonContent.replace("```json", "").replace("```", "").trim();

            int jsonStart = jsonContent.indexOf("{");
            int jsonEnd = jsonContent.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonContent = jsonContent.substring(jsonStart, jsonEnd + 1);
            }

            String[] metrics = {"tps", "memory", "disk", "network", "cpu", "gc", "mspt", "chunks", "plugins"};
            
            for (String metric : metrics) {

                int startIndex = -1;
                int valueStart = -1;
                String searchKey = "\"" + metric + "\":";
                startIndex = jsonContent.indexOf(searchKey);
                if (startIndex < 0) {
                    searchKey = "'" + metric + "':";
                    startIndex = jsonContent.indexOf(searchKey);
                }
                
                if (startIndex >= 0) {
                    valueStart = startIndex + searchKey.length();

                    while (valueStart < jsonContent.length() && Character.isWhitespace(jsonContent.charAt(valueStart))) {
                        valueStart++;
                    }

                    if (valueStart < jsonContent.length() && (jsonContent.charAt(valueStart) == '"' || jsonContent.charAt(valueStart) == '\'')) {
                        valueStart++;
                    }
                }
                
                if (startIndex >= 0 && valueStart > 0 && valueStart < jsonContent.length()) {

                    int endIndex = valueStart;
                    boolean escaped = false;
                    char quoteChar = jsonContent.charAt(valueStart - 1);
                    
                    while (endIndex < jsonContent.length()) {
                        char c = jsonContent.charAt(endIndex);
                        if (escaped) {
                            escaped = false;
                        } else if (c == '\\') {
                            escaped = true;
                        } else if (c == quoteChar && !escaped) {
                            break;
                        }
                        endIndex++;
                    }
                    
                    if (endIndex > valueStart) {
                        String value = jsonContent.substring(valueStart, endIndex);

                        value = value.replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                                    .replace("\\r", "\r")
                                    .replace("\\t", "\t")
                                    .replace("\\'", "'");
                        if (!value.trim().isEmpty()) {
                            result.put(metric, value.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при парсинге JSON ответа: " + e.getMessage());
        }
        
        return result;
    }
    
    public boolean isEnabled() {
        if (!enabled) {
            return false;
        }
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENROUTER_API_KEY_HERE") || apiKey.equals("your-api-key-here")) {
            plugin.getLogger().warning("OpenRouter API ключ не настроен! Установите openrouter.api_key в config.yml");
            return false;
        }
        return true;
    }
}

