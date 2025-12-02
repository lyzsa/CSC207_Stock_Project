package data_access;

import entity.Trade;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import use_case.trade.TradeDataAccessInterface;
import use_case.trade.TradeListener;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Finnhub-specific implementation of the TradeDataAccessInterface.
 * This class belongs to the data_access / infrastructure layer and knows about WebSockets and JSON format.
 */
public class FinnhubTradeDataAccessObject implements TradeDataAccessInterface {

    private static final Logger LOGGER = Logger.getLogger(FinnhubTradeDataAccessObject.class.getName());

    private static final String API_KEY = "d4977ehr01qshn3kvpt0d4977ehr01qshn3kvptg";
    private static final String WEB_SOCKET_URL = "wss://ws.finnhub.io?token=" + API_KEY;

    private WebSocket webSocket;
    private OkHttpClient client;
    private TradeListener listener;
    private String currentSymbol;
    private boolean hasReceivedPing = false;

    @Override
    public void connect(String symbol, TradeListener listener) {
        this.listener = listener;
        
        // Normalize the symbol input (trim whitespace)
        if (symbol != null) {
            symbol = symbol.trim();
        }
        
        // If we have an existing connection, unsubscribe and close it first
        if (webSocket != null) {
            LOGGER.info("Closing existing Finnhub connection...");
            // Unsubscribe from old symbol if we have one
            if (currentSymbol != null && !currentSymbol.isEmpty()) {
                try {
                    String unsubscribeMsg = String.format("{\"type\":\"unsubscribe\",\"symbol\":\"%s\"}", currentSymbol);
                    webSocket.send(unsubscribeMsg);
                    LOGGER.info("Unsubscribed from " + currentSymbol);
                    // Give a small delay for unsubscribe to be processed
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error unsubscribing: " + e.getMessage());
                }
            }
            webSocket.close(1000, "Reconnecting");
            webSocket = null;
            // Wait a bit for the connection to close
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Set the new symbol (trimmed and normalized) and reset ping tracking
        this.currentSymbol = (symbol != null) ? symbol.trim() : null;
        this.hasReceivedPing = false;
        LOGGER.info("Setting currentSymbol to: '" + this.currentSymbol + "'");

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .pingInterval(25, TimeUnit.SECONDS) // Finnhub often sends pings - keep connection alive
                .build();

        Request request = new Request.Builder().url(WEB_SOCKET_URL).build();

        if (listener != null) {
            listener.onStatusChanged("Status: Connecting...", false);
        }

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                LOGGER.info("WebSocket Opened. Subscribing to symbol: '" + currentSymbol + "'");
                String subscribeMsg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", currentSymbol);
                ws.send(subscribeMsg);
                LOGGER.info("Sent subscribe message: " + subscribeMsg);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                LOGGER.info("Received raw message from WebSocket: " + text);
                LOGGER.info("Message length: " + text.length() + " characters");
                if (text != null && !text.isEmpty()) {
                    LOGGER.info("First 200 chars of message: " + text.substring(0, Math.min(200, text.length())));
                }
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
                    listener.onStatusChanged("Status: Failure! See console.", true);
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
            // Unsubscribe from current symbol before closing
            if (currentSymbol != null && !currentSymbol.isEmpty()) {
                try {
                    String unsubscribeMsg = String.format("{\"type\":\"unsubscribe\",\"symbol\":\"%s\"}", currentSymbol);
                    webSocket.send(unsubscribeMsg);
                    LOGGER.info("Unsubscribed from " + currentSymbol);
                    // Give a small delay for unsubscribe to be processed
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error unsubscribing: " + e.getMessage());
                }
            }
            webSocket.close(1000, "User disconnect");
            webSocket = null;
        }
        // Don't shut down executor service - we might reconnect
        // Just set client to null, a new one will be created on next connect
        client = null;
        currentSymbol = null;
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
     * Processes trade data that comes in array format from Finnhub.
     * Format: {"type":"trade","data":[{"s":"SYMBOL","p":price,"v":volume,"t":timestamp},...]}
     * Or: [{"s":"SYMBOL","p":price,"v":volume,"t":timestamp},...]
     */
    private void processTradeArray(String jsonMessage) {
        try {
            LOGGER.info("Processing trade array message");
            
            // Extract the data array part
            String dataArray = null;
            if (jsonMessage.contains("\"data\":[")) {
                // Format: {"type":"trade","data":[...]}
                int dataStart = jsonMessage.indexOf("\"data\":[") + 8;
                int bracketCount = 1;
                int dataEnd = dataStart;
                while (dataEnd < jsonMessage.length() && bracketCount > 0) {
                    if (jsonMessage.charAt(dataEnd) == '[') bracketCount++;
                    if (jsonMessage.charAt(dataEnd) == ']') bracketCount--;
                    dataEnd++;
                }
                if (bracketCount == 0) {
                    dataArray = jsonMessage.substring(dataStart, dataEnd - 1);
                }
            } else if (jsonMessage.trim().startsWith("[")) {
                // Format: [{...}]
                dataArray = jsonMessage.trim().substring(1, jsonMessage.trim().length() - 1);
            }
            
            if (dataArray == null || dataArray.isEmpty()) {
                LOGGER.warning("Could not extract data array from message: " + jsonMessage);
                return;
            }
            
            LOGGER.info("Extracted data array: " + dataArray);
            
            // Split by },{ to get individual trade objects
            String[] tradeObjects = dataArray.split("\\},\\{");
            LOGGER.info("Found " + tradeObjects.length + " trade object(s) in array");
            
            for (int i = 0; i < tradeObjects.length; i++) {
                String tradeObj = tradeObjects[i];
                // Clean up brackets and braces
                tradeObj = tradeObj.replaceFirst("^\\{?", "{").replaceFirst("\\}?$", "}");
                if (!tradeObj.startsWith("{")) {
                    tradeObj = "{" + tradeObj;
                }
                if (!tradeObj.endsWith("}")) {
                    tradeObj = tradeObj + "}";
                }
                
                LOGGER.info("Processing trade object " + (i + 1) + ": " + tradeObj);
                
                // Process as a single trade message
                String symbol = extractValue(tradeObj, "s");
                String normalizedReceivedSymbol = (symbol != null) ? symbol.trim() : null;
                String normalizedCurrentSymbol = (currentSymbol != null) ? currentSymbol.trim() : null;
                
                if (normalizedCurrentSymbol != null && normalizedReceivedSymbol != null &&
                    normalizedCurrentSymbol.equalsIgnoreCase(normalizedReceivedSymbol)) {
                    LOGGER.info("Processing trade message for symbol: " + normalizedReceivedSymbol);
                    
                    String priceStr = extractValue(tradeObj, "p");
                    double price = (priceStr != null) ? Double.parseDouble(priceStr) : 0.0;
                    
                    String volumeStr = extractValue(tradeObj, "v");
                    double volume = (volumeStr != null) ? Double.parseDouble(volumeStr) : 0.0;
                    
                    String timestampStr = extractValue(tradeObj, "t");
                    long timestamp = (timestampStr != null) ? Long.parseLong(timestampStr) : 0L;
                    
                    Instant ts = timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;
                    
                    if (listener != null) {
                        listener.onStatusChanged("Status: Connected to " + currentSymbol, false);
                        Trade trade = new Trade(symbol, price, volume, ts);
                        listener.onTrade(trade);
                    }
                } else {
                    LOGGER.info("Ignoring trade in array for symbol '" + normalizedReceivedSymbol + 
                               "' (current: '" + normalizedCurrentSymbol + "')");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing trade array: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses the incoming JSON message using manual string manipulation and dispatches Trade domain objects.
     * Based on working Finnhub WebSocket example.
     */
    private void processMessage(String jsonMessage) {
        try {
            // Handle ping messages (connection keep-alive)
            if (jsonMessage.contains("\"type\":\"ping\"")) {
                hasReceivedPing = true;
                LOGGER.info("Received PING: Connection is alive.");
                return;
            }
            
            // Handle trade messages - this is the main format Finnhub uses
            if (jsonMessage.contains("\"type\":\"trade\"")) {
                LOGGER.info("💵 RECEIVED TRADE DATA: " + jsonMessage);
                // Parse the trade data - Finnhub sends: {"type":"trade","data":[{"s":"SYMBOL","p":price,"v":volume,"t":timestamp}]}
                // Or sometimes: {"type":"trade","s":"SYMBOL","p":price,"v":volume,"t":timestamp}
                
                // Check if it's an array format with "data" field
                if (jsonMessage.contains("\"data\":[")) {
                    LOGGER.info("Trade message contains data array, processing as array");
                    processTradeArray(jsonMessage);
                    return;
                }
                
                // Otherwise, try to extract trade data directly from the message
                String symbol = extractValue(jsonMessage, "s");
                
                // Normalize both symbols for comparison (trim and case-insensitive)
                String normalizedReceivedSymbol = (symbol != null) ? symbol.trim() : null;
                String normalizedCurrentSymbol = (currentSymbol != null) ? currentSymbol.trim() : null;
                
                // Only process messages for the current symbol to avoid processing old messages
                // Use case-insensitive comparison to handle API format differences
                if (normalizedCurrentSymbol == null || 
                    normalizedReceivedSymbol == null ||
                    !normalizedCurrentSymbol.equalsIgnoreCase(normalizedReceivedSymbol)) {
                    LOGGER.info("Ignoring trade message for symbol '" + normalizedReceivedSymbol + 
                               "' (current: '" + normalizedCurrentSymbol + "')");
                    return;
                }
                
                LOGGER.info("Processing trade message for symbol: " + normalizedReceivedSymbol);

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
            } 
            // Handle error messages
            else if (jsonMessage.contains("\"type\":\"error\"")) {
                // Handle error messages from the API
                String errorMsg = extractValue(jsonMessage, "msg");
                LOGGER.warning("Received error message: " + errorMsg);
                if (listener != null) {
                    String errorText = errorMsg != null ? errorMsg : "Unknown error";
                    listener.onStatusChanged("Status: Error - " + errorText, true);
                }
            } else {
                // Log any other messages (like subscription confirmations)
                LOGGER.info("Received TEXT: " + jsonMessage);
                // Check if it might be an array format we missed
                if (jsonMessage.trim().startsWith("[") && jsonMessage.contains("\"s\"") && jsonMessage.contains("\"p\"")) {
                    LOGGER.info("Message appears to be an array format, trying array processing");
                    processTradeArray(jsonMessage);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing JSON message with manual parsing: " + e.getMessage()
                    + "\nMessage: " + jsonMessage);
        }
    }
}

