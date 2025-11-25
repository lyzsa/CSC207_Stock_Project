package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 * The main application window displayed after successful login.
 * Manages the navigation between different dashboard views using CardLayout.
 */
public class Dashboard extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(Dashboard.class.getName());

    private final StockPage homePanel;
    private final EarningsPage earningsPanel;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel;
    private final String username;

    public Dashboard(String username) {
        this.username = username;
        setTitle("Stock App Dashboard - Logged in as: " + username);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 600); // Slightly larger frame for better dashboard layout
        setLocationRelativeTo(null);

        this.homePanel = new StockPage(); // The real-time trade view
        this.mainContentPanel = new JPanel(cardLayout);
        this.earningsPanel = new EarningsPage();

        setLayout(new BorderLayout());

        // 1. Create the Navigation Bar (North Panel)
        JPanel navBar = createNavigationBar();
        add(navBar, BorderLayout.NORTH);

        // 2. Setup the Card Layout for main content (Center Panel)
        setupMainContent();
        add(mainContentPanel, BorderLayout.CENTER);

        // Add a listener to ensure WebSocket is closed on application exit
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                homePanel.closeWebSocket(); // Ensure WebSocket connection is closed
                dispose(); // Close the window
                System.exit(0); // Terminate the application
            }
        });
    }

    /**
     * Creates the top navigation bar containing the four main buttons and the Logout button.
     * Uses a GridBagLayout for flexible spacing.
     */
    private JPanel createNavigationBar() {
        JPanel navBar = new JPanel(new GridBagLayout());
        navBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        navBar.setBackground(new Color(240, 240, 240));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        String[] buttonNames = {"Home (Real-Time Trade)", "Portfolio", "Watchlist", "Company Earnings History", "Settings"};

        // Create and add the four main navigation buttons
        for (int i = 0; i < buttonNames.length; i++) {
            JButton button = new JButton(buttonNames[i]);
            button.setFont(new Font("Arial", Font.BOLD, 14));
            // String cardName = buttonNames[i].split(" ")[0]; // Use first word as card key
            String cardName;
            if (buttonNames[i].equals("Company Earnings History")) {
                cardName = "Earnings";   // must match your CardLayout key
            } else {
                cardName = buttonNames[i].split(" ")[0];
            }

            button.addActionListener(e -> {
                cardLayout.show(mainContentPanel, cardName);
                if (!cardName.equals("Home")) {
                    homePanel.closeWebSocket(); // Stop real-time data when navigating away
                }
                updateButtonStyles(navBar, button);
            });

            gbc.gridx = i;
            gbc.weightx = 1.0; // Distribute space evenly
            navBar.add(button, gbc);
        }

        // Add a flexible spacer to push the Logout button to the far right
        gbc.gridx = buttonNames.length;
        gbc.weightx = 10.0;
        navBar.add(Box.createHorizontalGlue(), gbc);

        // Add Logout button
        gbc.gridx = buttonNames.length + 1;
        gbc.weightx = 0.0; // Don't take extra space
        JButton logoutButton = new JButton("Logout (" + username + ")");
        logoutButton.setBackground(new Color(200, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.addActionListener(e -> handleLogout());
        navBar.add(logoutButton, gbc);

        // Initially set "Home" button as selected (assuming it's the first button)
        SwingUtilities.invokeLater(() -> {
            if (navBar.getComponentCount() > 0) {
                updateButtonStyles(navBar, (JButton) navBar.getComponent(0));
            }
        });

        return navBar;
    }

    /**
     * Sets up the main content area with different panels for the CardLayout.
     */
    private void setupMainContent() {
        // Add the real-time trade panel (Home)
        mainContentPanel.add(homePanel, "Home");

        // Add placeholder panels for other views
        mainContentPanel.add(createPlaceholderPanel("Portfolio View", Color.LIGHT_GRAY), "Portfolio");
        mainContentPanel.add(createPlaceholderPanel("Watchlist Management", Color.CYAN), "Watchlist");
        mainContentPanel.add(earningsPanel, "Earnings");
        mainContentPanel.add(createPlaceholderPanel("User Settings", Color.YELLOW), "Settings");

        // Show the Home view by default
        cardLayout.show(mainContentPanel, "Home");
    }

    /**
     * Helper to create a simple placeholder panel for non-implemented views.
     */
    private JPanel createPlaceholderPanel(String title, Color bgColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);
        JLabel label = new JLabel("--- " + title + " ---", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.ITALIC, 24));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Updates the styling of the navigation buttons to indicate the active view.
     */
    private void updateButtonStyles(JPanel navBar, JButton active) {
        for (Component comp : navBar.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                if (button.getText().startsWith("Logout")) continue; // Skip Logout button

                if (button == active) {
                    button.setBackground(new Color(50, 150, 250)); // Active color (Blue)
                    button.setForeground(Color.WHITE);
                } else {
                    button.setBackground(UIManager.getColor("Button.background")); // Default color
                    button.setForeground(UIManager.getColor("Button.foreground"));
                }
            }
        }
    }

    /**
     * Handles the user logging out.
     */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            homePanel.closeWebSocket(); // Close connection before logging out

            // Close dashboard and open the login screen
            this.dispose();
            SwingUtilities.invokeLater(() -> {
                // Relaunch the Main (Login) window
                new Main().setVisible(true);
            });
        }
    }
}