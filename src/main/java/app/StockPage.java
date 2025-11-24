package app;

import okhttp3.*;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A JPanel containing the real-time trade dashboard UI and WebSocket logic.
 * This component is embedded within the main Dashboard JFrame.
 * NOTE: This version uses manual string parsing instead of a JSON library.
 */
public class StockPage extends JPanel { // Class name changed from DashboardContent

    private static final Logger LOGGER = Logger.getLogger(StockPage.class.getName()); // Logger updated

    // Replace with your actual Finnhub API Key
    private static final String API_KEY = "d4977ehr01qshn3kvpt0d4977ehr01qshn3kvptg";
    private static final String WEB_SOCKET_URL = "wss://ws.finnhub.io?token=" + API_KEY;
    private static final String DEFAULT_SYMBOL = "BINANCE:BTCUSDT"; // Example symbol

    // GUI Elements for display
    private final JLabel symbolLabel = new JLabel("---");
    private final JLabel priceLabel = new JLabel("---");
    private final JLabel volumeLabel = new JLabel("---");
    private final JLabel timestampLabel = new JLabel("---");
    private final JLabel statusLabel = new JLabel("Status: Disconnected", SwingConstants.CENTER);

    private WebSocket webSocket;
    private final JButton connectButton = new JButton("Connect");

    /**
     * Initializes the GUI components.
     */
    public StockPage() { // Constructor name changed
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Real-Time Trade Data"));

        // --- Header (Status) ---
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(statusLabel, BorderLayout.NORTH);

        // --- Main Data Panel ---
        JPanel dataPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        dataPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Helper method to style the value labels
        styleLabel(symbolLabel);
        styleLabel(priceLabel);
        styleLabel(volumeLabel);
        styleLabel(timestampLabel);

        dataPanel.add(new JLabel("Symbol:"));
        dataPanel.add(symbolLabel);

        dataPanel.add(new JLabel("Last Price (P):"));
        dataPanel.add(priceLabel);

        dataPanel.add(new JLabel("Volume (V):"));
        dataPanel.add(volumeLabel);

        dataPanel.add(new JLabel("Timestamp (T):"));
        dataPanel.add(timestampLabel);

        add(dataPanel, BorderLayout.CENTER);

        // --- Control Panel (Connect/Disconnect) ---
        JPanel controlPanel = new JPanel();
        connectButton.addActionListener(e -> toggleConnection());
        controlPanel.add(connectButton);
        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Helper to apply basic styling to the data display labels.
     */
    private void styleLabel(JLabel label) {
        label.setFont(new Font("Monospaced", Font.BOLD, 18));
        label.setForeground(new Color(0, 128, 0)); // Green color for data
    }

    /**
     * Toggles the WebSocket connection status.
     */
    private void toggleConnection() {
        if (webSocket != null) {
            closeWebSocket();
        } else {
            connectWebSocket();
        }
    }

    /**
     * Gracefully closes the WebSocket connection.
     */
    public void closeWebSocket() {
        if (webSocket != null) {
            LOGGER.info("Closing existing connection...");
            webSocket.close(1000, "User initiated disconnect");
            webSocket = null;
            // Also update GUI elements
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Status: Disconnected");
                statusLabel.setForeground(Color.BLACK);
                connectButton.setText("Connect");
                connectButton.setEnabled(true);
            });
        }
    }

    /**
     * Connects to the WebSocket and sends subscription messages.
     */
    private void connectWebSocket() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder().url(WEB_SOCKET_URL).build();

        statusLabel.setText("Status: Connecting...");
        statusLabel.setForeground(Color.ORANGE);
        connectButton.setText("Connecting...");
        connectButton.setEnabled(false);

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                LOGGER.info("WebSocket Opened. Subscribing to " + DEFAULT_SYMBOL);

                String subscribeMsg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", DEFAULT_SYMBOL);
                ws.send(subscribeMsg);

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Status: Connected to " + DEFAULT_SYMBOL);
                    statusLabel.setForeground(new Color(0, 128, 0));
                    connectButton.setText("Disconnect");
                    connectButton.setEnabled(true);
                });
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                processMessage(text);
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                LOGGER.info("WebSocket Closing: Code " + code + ", Reason: " + reason);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                LOGGER.log(Level.SEVERE, "WebSocket Failure: " + t.getMessage(), t);
                closeWebSocket(); // Attempt to clean up resources
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Status: Failure! Check API Key/Console.");
                    statusLabel.setForeground(Color.RED);
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);
                });
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                LOGGER.info("WebSocket Closed.");
                webSocket = null;
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Status: Disconnected");
                    statusLabel.setForeground(Color.BLACK);
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);
                });
            }
        });
    }

    /**
     * Extracts a value from a JSON string using basic String methods.
     */
    private String extractValue(String json, String key) {
        String keySearch = "\"" + key + "\":";
        int keyIndex = json.indexOf(keySearch);

        if (keyIndex == -1) {
            return null;
        }

        int valueStart = keyIndex + keySearch.length();

        // Check for string value (starts with double quote)
        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
            valueStart++;
            int valueEnd = json.indexOf('"', valueStart);
            if (valueEnd != -1) {
                return json.substring(valueStart, valueEnd);
            }
        }

        // Logic for numerical/boolean values
        else {
            // Find the index of the immediate next JSON delimiter: ',', '}', or ']'
            int indexComma = json.indexOf(',', valueStart);
            int indexCurly = json.indexOf('}', valueStart);
            int indexBracket = json.indexOf(']', valueStart);

            // Start with a large index to find the minimum
            int terminationIndex = Integer.MAX_VALUE;

            // Find the minimum positive index among the delimiters
            if (indexComma != -1 && indexComma > valueStart) terminationIndex = Math.min(terminationIndex, indexComma);
            if (indexCurly != -1 && indexCurly > valueStart) terminationIndex = Math.min(terminationIndex, indexCurly);
            if (indexBracket != -1 && indexBracket > valueStart) terminationIndex = Math.min(terminationIndex, indexBracket);

            // If a valid termination character was found
            if (terminationIndex != Integer.MAX_VALUE) {
                // substring is exclusive of the end index, correctly extracting the value
                return json.substring(valueStart, terminationIndex).trim();
            }
        }
        return null;
    }

    /**
     * Parses the incoming JSON message using manual string manipulation and updates the GUI.
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

                SwingUtilities.invokeLater(() -> {
                    symbolLabel.setText(symbol != null ? symbol : "N/A");
                    priceLabel.setText(String.format("$%,.2f", price));
                    volumeLabel.setText(String.format("%,.4f", volume));

                    if (timestamp > 0) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                        String time = Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .format(formatter);
                        timestampLabel.setText(time);
                    } else {
                        timestampLabel.setText("N/A");
                    }
                });
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Error processing JSON message: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "General error in processMessage: " + e.getMessage());
        }
    }
}