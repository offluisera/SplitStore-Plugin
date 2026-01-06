package org.gruposplit.github.offluisera.splitstore.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente HTTP para comunicação com a API do SplitStore
 */
public class APIClient {

    private final String apiUrl;
    private final String apiKey;
    private final String apiSecret;
    private final Logger logger;
    private final Gson gson;
    private final int connectionTimeout;
    private final int readTimeout;

    public APIClient(String apiUrl, String apiKey, String apiSecret, Logger logger) {
        this(apiUrl, apiKey, apiSecret, logger, 5000, 5000);
    }

    public APIClient(String apiUrl, String apiKey, String apiSecret, Logger logger,
                     int connectionTimeout, int readTimeout) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.logger = logger;
        this.gson = new Gson();
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * Verifica a conexão com a API
     */
    public APIResponse verifyConnection(String serverVersion) {
        Map<String, Object> data = new HashMap<>();
        data.put("server", "minecraft");
        data.put("version", serverVersion);

        return post("/plugin/verify", data);
    }

    /**
     * Busca compras pendentes para um jogador
     */
    public APIResponse getPendingPurchases(String playerUUID, String playerName) {
        Map<String, Object> data = new HashMap<>();
        data.put("player_uuid", playerUUID);
        data.put("player_name", playerName);

        return post("/plugin/purchases/pending", data);
    }

    /**
     * Confirma entrega de uma compra
     */
    public APIResponse confirmDelivery(String purchaseId, String playerUUID) {
        Map<String, Object> data = new HashMap<>();
        data.put("purchase_id", purchaseId);
        data.put("player_uuid", playerUUID);
        data.put("delivered_at", System.currentTimeMillis());

        return post("/plugin/purchases/confirm", data);
    }

    /**
     * Notifica logout de jogador
     */
    public APIResponse notifyPlayerLogout(String playerUUID, String playerName) {
        Map<String, Object> data = new HashMap<>();
        data.put("player_uuid", playerUUID);
        data.put("player_name", playerName);
        data.put("timestamp", System.currentTimeMillis());

        return post("/plugin/player/logout", data);
    }

    /**
     * Sincroniza status do servidor
     */
    public APIResponse syncServerStatus(int onlinePlayers, int maxPlayers) {
        Map<String, Object> data = new HashMap<>();
        data.put("online_players", onlinePlayers);
        data.put("max_players", maxPlayers);
        data.put("timestamp", System.currentTimeMillis());

        return post("/plugin/server/status", data);
    }

    /**
     * Envia log de erro para a API
     */
    public APIResponse sendErrorLog(String errorType, String errorMessage, String stackTrace) {
        Map<String, Object> data = new HashMap<>();
        data.put("error_type", errorType);
        data.put("error_message", errorMessage);
        data.put("stack_trace", stackTrace);
        data.put("timestamp", System.currentTimeMillis());

        return post("/plugin/errors/report", data);
    }

    /**
     * Realiza requisição POST para a API
     */
    private APIResponse post(String endpoint, Map<String, Object> data) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(apiUrl + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("X-API-Key", apiKey);
            conn.setRequestProperty("X-API-Secret", apiSecret);
            conn.setRequestProperty("User-Agent", "SplitStore-Plugin/1.0.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(readTimeout);

            // Serializar dados para JSON
            String jsonInput = gson.toJson(data);

            // Enviar requisição
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Ler resposta
            int responseCode = conn.getResponseCode();

            BufferedReader in;
            if (responseCode >= 200 && responseCode < 300) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parsear resposta JSON
            JsonObject jsonResponse = null;
            try {
                jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao parsear JSON: " + response.toString(), e);
            }

            return new APIResponse(responseCode, response.toString(), jsonResponse, true);

        } catch (java.net.SocketTimeoutException e) {
            logger.log(Level.WARNING, "Timeout na conexão com API: " + endpoint, e);
            return new APIResponse(408, "Connection timeout", null, false);

        } catch (java.net.UnknownHostException e) {
            logger.log(Level.WARNING, "Host não encontrado: " + apiUrl, e);
            return new APIResponse(0, "Unknown host", null, false);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao fazer requisição POST para: " + endpoint, e);
            return new APIResponse(0, e.getMessage(), null, false);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Classe para encapsular respostas da API
     */
    public static class APIResponse {
        private final int statusCode;
        private final String rawResponse;
        private final JsonObject jsonResponse;
        private final boolean success;

        public APIResponse(int statusCode, String rawResponse, JsonObject jsonResponse, boolean success) {
            this.statusCode = statusCode;
            this.rawResponse = rawResponse;
            this.jsonResponse = jsonResponse;
            this.success = success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public JsonObject getJsonResponse() {
            return jsonResponse;
        }

        public boolean isSuccess() {
            return success && statusCode >= 200 && statusCode < 300;
        }

        public boolean hasError() {
            return !success || statusCode >= 400;
        }

        public String getErrorMessage() {
            if (jsonResponse != null && jsonResponse.has("error")) {
                return jsonResponse.get("error").getAsString();
            }
            return rawResponse;
        }

        public boolean hasData() {
            return jsonResponse != null && jsonResponse.has("data");
        }

        public JsonObject getData() {
            if (hasData()) {
                return jsonResponse.getAsJsonObject("data");
            }
            return null;
        }
    }
}