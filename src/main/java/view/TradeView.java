package view;

import entity.Trade;
import interface_adapter.ViewManagerModel;
import use_case.trade.TradeFeed;
import use_case.trade.TradeListener;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Swing view for displaying real-time trade data.
 * Depends only on the TradeFeed abstraction and domain Trade entity.
 */
public class TradeView extends JPanel {

    private final String viewName = "trade";
    private ViewManagerModel viewManagerModel;
    private String homeViewName;

    private final TradeFeed tradeFeed;

    // Trade data labels
    private final JLabel symbolLabel = new JLabel("---");
    private final JLabel priceLabel = new JLabel("---");
    private final JLabel volumeLabel = new JLabel("---");
    private final JLabel timestampLabel = new JLabel("---");
    private final JLabel statusLabel = new JLabel("Status: Disconnected", SwingConstants.CENTER);
    private final JTextField symbolInputField = new JTextField(20);
    private JButton connectButton;
    private JButton disconnectButton;
    private String currentSymbol = "";
    private long connectionStartTime = 0;
    private long lastConnectionAttemptTime = 0;
    private boolean isConnected = false;
    private static final long SYMBOL_NOT_FOUND_TIMEOUT = 10000; // 10 seconds
    private static final long CONNECTION_COOLDOWN_MS = 5000; // 5 seconds between connection attempts

    /**
     * Initializes the GUI components.
     */
    public TradeView(TradeFeed tradeFeed) {
        this.tradeFeed = tradeFeed;
        setLayout(new BorderLayout());

        // --- Top: Back button + status ---
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("Back to Home");
        backButton.addActionListener(e -> {
            if (viewManagerModel != null && homeViewName != null) {
                viewManagerModel.setState(homeViewName);
                viewManagerModel.firePropertyChange();
            }
        });
        backPanel.add(backButton);
        topPanel.add(backPanel, BorderLayout.WEST);

        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        topPanel.add(statusLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // --- Center: Trade data display ---
        JPanel mainPanel = createMainDashboardCard();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Creates the main dashboard card that shows the trade data and connect button.
     */
    private JPanel createMainDashboardCard() {
        JPanel root = new JPanel(new BorderLayout(10, 10));

        // Search panel at top with Connect and Disconnect buttons
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.add(new JLabel("Symbol:"));
        symbolInputField.setText("BINANCE:BTCUSDT"); // Default value
        searchPanel.add(symbolInputField);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> onConnectClicked());
        searchPanel.add(connectButton);
        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> onDisconnectClicked());
        disconnectButton.setEnabled(false); // Initially disabled
        searchPanel.add(disconnectButton);
        root.add(searchPanel, BorderLayout.NORTH);

        // Data display panel
        JPanel dataPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        dataPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

        root.add(dataPanel, BorderLayout.CENTER);

        return root;
    }

    /**
     * Helper to apply basic styling to the data display labels.
     */
    private void styleLabel(JLabel label) {
        label.setFont(new Font("Monospaced", Font.BOLD, 18));
        label.setForeground(new Color(0, 128, 0)); // Green color for data
    }

    /**
     * Handles the Connect button click by delegating to the TradeFeed.
     */
    private void onConnectClicked() {
        if (tradeFeed != null) {
            // Check if already connected - user must disconnect first
            if (isConnected) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please disconnect from the current trade before connecting to a new one.",
                    "Already Connected",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            String symbol = symbolInputField.getText().trim();
            if (symbol.isEmpty()) {
                statusLabel.setText("Status: Please enter a symbol");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            // Check rate limiting - prevent too frequent connection attempts
            long currentTime = System.currentTimeMillis();
            long timeSinceLastAttempt = currentTime - lastConnectionAttemptTime;
            
            if (timeSinceLastAttempt < CONNECTION_COOLDOWN_MS) {
                long remainingTime = (CONNECTION_COOLDOWN_MS - timeSinceLastAttempt) / 1000;
                JOptionPane.showMessageDialog(
                    this,
                    "Please wait " + remainingTime + " second(s) before connecting again.\nToo many requests may result in rate limiting.",
                    "Rate Limit",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Update last connection attempt time
            lastConnectionAttemptTime = currentTime;
            
            // Disconnect previous connection if any
            tradeFeed.disconnect();
            
            // Reset state
            currentSymbol = symbol;
            connectionStartTime = System.currentTimeMillis();
            symbolLabel.setText("---");
            priceLabel.setText("---");
            volumeLabel.setText("---");
            timestampLabel.setText("---");
            
            // Disable connect button during connection attempt, enable disconnect button
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            
            // Immediately show "Connecting..." while we wait for the first trade.
            statusLabel.setText("Status: Connecting...");
            statusLabel.setForeground(Color.ORANGE);

            tradeFeed.connect(symbol, new TradeListener() {
                @Override
                public void onTrade(Trade trade) {
                    connectionStartTime = 0; // Reset timeout on first trade
                    isConnected = true; // Mark as connected on first trade
                    SwingUtilities.invokeLater(() -> {
                        connectButton.setEnabled(false); // Disable connect when connected
                        disconnectButton.setEnabled(true); // Enable disconnect when connected
                    });
                    updateTradeOnUi(trade);
                }

                @Override
                public void onStatusChanged(String statusText, boolean isError) {
                    SwingUtilities.invokeLater(() -> {
                        if (isError) {
                            // On error, re-enable connect button and disable disconnect
                            connectButton.setEnabled(true);
                            disconnectButton.setEnabled(false);
                            isConnected = false;
                        } else if (statusText.contains("Disconnected")) {
                            // On disconnect, reset button states
                            connectButton.setEnabled(true);
                            disconnectButton.setEnabled(false);
                            isConnected = false;
                        }
                    });
                    updateStatusOnUi(statusText, isError);
                    
                    // Combined error handling for rate limiting and symbol not found
                    if (isError) {
                        boolean isRateLimit = statusText.contains("429") || statusText.contains("Too Many Requests") || statusText.contains("Rate limit");
                        boolean isOtherError = statusText.contains("Error") || statusText.contains("Failure");
                        
                        if (isRateLimit || isOtherError) {
                            SwingUtilities.invokeLater(() -> {
                                String message;
                                if (isRateLimit) {
                                    message = "Connection failed: Too Many Requests (429).\n" +
                                              "This may also occur if the symbol '" + currentSymbol + "' is invalid.\n\n" +
                                              "Please wait " + (CONNECTION_COOLDOWN_MS / 1000) + 
                                              " seconds before trying again.\n" +
                                              "If the problem persists, please verify the symbol is correct.";
                                    // Enforce cooldown after 429 error
                                    lastConnectionAttemptTime = System.currentTimeMillis();
                                    startCooldownTimer();
                                } else {
                                    message = "Connection failed: Symbol '" + currentSymbol + "' not found or invalid.\n" +
                                              "This may also occur if requests are made too frequently.\n\n" +
                                              "Please wait " + (CONNECTION_COOLDOWN_MS / 1000) + 
                                              " seconds before trying again.\n" +
                                              "If the problem persists, please verify the symbol is correct.";
                                    // Enforce cooldown for other errors too
                                    lastConnectionAttemptTime = System.currentTimeMillis();
                                    startCooldownTimer();
                                }
                                
                                JOptionPane.showMessageDialog(
                                    TradeView.this,
                                    message,
                                    "Connection Error",
                                    JOptionPane.ERROR_MESSAGE
                                );
                            });
                        }
                    }
                }
            });
            
            // Start a timeout check for symbol not found
            startSymbolNotFoundCheck();
        }
    }
    
    /**
     * Handles the Disconnect button click.
     */
    private void onDisconnectClicked() {
        if (tradeFeed != null) {
            // Disconnect from the trade feed
            tradeFeed.disconnect();
            
            // Reset all info to default
            resetToDefault();
        }
    }
    
    /**
     * Resets all information to default values.
     */
    private void resetToDefault() {
        // Reset connection state
        isConnected = false;
        connectionStartTime = 0;
        currentSymbol = "";
        
        // Reset UI labels to default
        symbolLabel.setText("---");
        priceLabel.setText("---");
        volumeLabel.setText("---");
        timestampLabel.setText("---");
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(Color.BLACK);
        
        // Reset button states
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        connectButton.setText("Connect");
    }
    
    /**
     * Starts a background thread to check if symbol is not found after timeout.
     */
    private void startSymbolNotFoundCheck() {
        new Thread(() -> {
            try {
                Thread.sleep(SYMBOL_NOT_FOUND_TIMEOUT);
                // If we're still connecting after timeout and no trades received
                if (connectionStartTime > 0 && System.currentTimeMillis() - connectionStartTime >= SYMBOL_NOT_FOUND_TIMEOUT) {
                    SwingUtilities.invokeLater(() -> {
                        if (symbolLabel.getText().equals("---")) {
                            // No trades received - could be symbol not found or rate limiting
                            String message = "Connection timeout: Symbol '" + currentSymbol + "' not found or no trades available.\n" +
                                           "This may also occur if requests are made too frequently.\n\n" +
                                           "Please wait " + (CONNECTION_COOLDOWN_MS / 1000) + 
                                           " seconds before trying again.\n" +
                                           "If the problem persists, please verify the symbol is correct.";
                            
                            JOptionPane.showMessageDialog(
                                TradeView.this,
                                message,
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                            statusLabel.setText("Status: Connection failed");
                            statusLabel.setForeground(Color.RED);
                            tradeFeed.disconnect();
                            resetToDefault();
                            // Enforce cooldown after timeout
                            lastConnectionAttemptTime = System.currentTimeMillis();
                            startCooldownTimer();
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Starts a cooldown timer that disables the connect button and shows countdown.
     */
    private void startCooldownTimer() {
        connectButton.setEnabled(false);
        new Thread(() -> {
            try {
                for (int remaining = (int)(CONNECTION_COOLDOWN_MS / 1000); remaining > 0; remaining--) {
                    final int seconds = remaining;
                    SwingUtilities.invokeLater(() -> {
                        connectButton.setText("Wait " + seconds + "s");
                        statusLabel.setText("Status: Rate limited - wait " + seconds + " second(s)");
                        statusLabel.setForeground(Color.ORANGE);
                    });
                    Thread.sleep(1000);
                }
                SwingUtilities.invokeLater(() -> {
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                    statusLabel.setText("Status: Ready to connect");
                    statusLabel.setForeground(Color.BLACK);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(() -> {
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                });
            }
        }).start();
    }

    /**
     * Updates UI labels when a new trade is received.
     */
    private void updateTradeOnUi(Trade trade) {
        SwingUtilities.invokeLater(() -> {
            String symbol = trade.getSymbol();
            double price = trade.getPrice();
            double volume = trade.getVolume();
            Instant timestamp = trade.getTimestamp();

            symbolLabel.setText(symbol != null ? symbol : "N/A");
            priceLabel.setText(String.format("$%,.2f", price));
            volumeLabel.setText(String.format("%,.4f", volume));

            if (timestamp != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                String time = timestamp
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .format(formatter);
                timestampLabel.setText(time);
            } else {
                timestampLabel.setText("N/A");
            }
        });
    }

    /**
     * Updates the status label on the Event Dispatch Thread.
     */
    private void updateStatusOnUi(String statusText, boolean isError) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(statusText);
            if (isError) {
                statusLabel.setForeground(Color.RED);
            } else if (statusText != null && statusText.contains("Connected")) {
                statusLabel.setForeground(new Color(0, 128, 0));
            } else if (statusText != null && statusText.contains("Connecting")) {
                statusLabel.setForeground(Color.ORANGE);
            } else {
                statusLabel.setForeground(Color.BLACK);
            }
        });
    }

    public String getViewName() {
        return this.viewName;
    }

    public void setBackNavigation(ViewManagerModel viewManagerModel, String homeViewName) {
        this.viewManagerModel = viewManagerModel;
        this.homeViewName = homeViewName;
    }
}

