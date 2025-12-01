package data_access;

import entity.Trade;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import use_case.TradeFeed;
import use_case.TradeListener;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Finnhub-specific implementation of the TradeFeed abstraction.
 * This class belongs to the data_access / infrastructure layer and knows about WebSockets and JSON format.
 */
public class FinnhubTradeFeed implements TradeFeed {

    private static final Logger LOGGER = Logger.getLogger(FinnhubTradeFeed.class.getName());

    private static final String API_KEY = "d4977ehr01qshn3kvpt0d4977ehr01qshn3kvptg";
    private static final String WEB_SOCKET_URL = "wss://ws.finnhub.io?token=" + API_KEY;

    private WebSocket webSocket;
    private OkHttpClient client;
    private TradeListener listener;
    private String currentSymbol;

    @Override
    public void connect(String symbol, TradeListener listener) {
        this.listener = listener;
        this.currentSymbol = symbol;

        if (webSocket != null) {
            LOGGER.info("Closing existing Finnhub connection...");
            webSocket.close(1000, "Reconnecting");
        }

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder().url(WEB_SOCKET_URL).build();

        if (listener != null) {
            listener.onStatusChanged("Status: Connecting...", false);
        }

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                LOGGER.info("WebSocket Opened. Subscribing to " + currentSymbol);
                String subscribeMsg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", currentSymbol);
                ws.send(subscribeMsg);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                processMessage(text);
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                LOGGER.info("WebSocket Closing: Code " + code + ", Reason: " + reason);
                ws.close(1000, null);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                LOGGER.log(Level.SEVERE, "WebSocket Failure: " + t.getMessage(), t);
                if (listener != null) {
                    String errorMessage = "Status: Failure! See console.";
                    // Check for 429 rate limiting error
                    if (t.getMessage() != null && t.getMessage().contains("429")) {
                        errorMessage = "Status: 429 Too Many Requests - Rate limit exceeded";
                    } else if (response != null && response.code() == 429) {
                        errorMessage = "Status: 429 Too Many Requests - Rate limit exceeded";
                    }
                    listener.onStatusChanged(errorMessage, true);
                }
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                LOGGER.info("WebSocket Closed.");
                webSocket = null;
                if (listener != null) {
                    listener.onStatusChanged("Status: Disconnected", false);
                }
            }
        });
    }

    @Override
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User disconnect");
            webSocket = null;
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client = null;
        }
    }

    /**
     * Small internal JSON parsing helper: extracts a value from a JSON string using basic String methods.
     * Kept here in the infrastructure layer so UI and domain remain free of parsing details.
     */
    private String extractValue(String json, String key) {
        String keySearch = "\"" + key + "\":";
        int keyIndex = json.indexOf(keySearch);

        if (keyIndex == -1) {
            return null;
        }

        int valueStart = keyIndex + keySearch.length();
        char firstChar = json.charAt(valueStart);

        if (firstChar == '"') {
            valueStart++;
            int valueEnd = json.indexOf('"', valueStart);
            if (valueEnd != -1) {
                return json.substring(valueStart, valueEnd);
            }
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.' || json.charAt(valueEnd) == '-')) {
                valueEnd++;
            }
            int commaIndex = json.indexOf(',', valueStart);
            int braceIndex = json.indexOf('}', valueStart);

            int terminationIndex = json.length();
            if (commaIndex != -1) terminationIndex = Math.min(terminationIndex, commaIndex);
            if (braceIndex != -1) terminationIndex = Math.min(terminationIndex, braceIndex);

            if (terminationIndex > valueStart) {
                return json.substring(valueStart, terminationIndex).trim();
            }
        }
        return null;
    }

    /**
     * Parses the incoming JSON message using manual string manipulation and dispatches Trade domain objects.
     */
    private void processMessage(String jsonMessage) {
        try {
            if (jsonMessage.contains("\"type\":\"trade\"")) {
                String symbol = extractValue(jsonMessage, "s");

                String priceStr = extractValue(jsonMessage, "p");
                double price = (priceStr != null) ? Double.parseDouble(priceStr) : 0.0;

                String volumeStr = extractValue(jsonMessage, "v");
                double volume = (volumeStr != null) ? Double.parseDouble(volumeStr) : 0.0;

                String timestampStr = extractValue(jsonMessage, "t");
                long timestamp = (timestampStr != null) ? Long.parseLong(timestampStr) : 0L;

                Instant ts = timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;

                if (listener != null) {
                    // First successful trade means we are effectively connected for this symbol.
                    listener.onStatusChanged("Status: Connected to " + currentSymbol, false);

                    Trade trade = new Trade(symbol, price, volume, ts);
                    listener.onTrade(trade);
                }
            } else if (jsonMessage.contains("\"type\":\"ping\"")) {
                // No-op: connection keep-alive
            } else if (jsonMessage.contains("\"type\":\"error\"")) {
                // Handle error messages from the API
                String errorMsg = extractValue(jsonMessage, "msg");
                if (listener != null) {
                    String errorText = errorMsg != null ? errorMsg : "Unknown error";
                    listener.onStatusChanged("Status: Error - " + errorText, true);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing JSON message with manual parsing: " + e.getMessage()
                    + "\nMessage: " + jsonMessage);
        }
    }
}

