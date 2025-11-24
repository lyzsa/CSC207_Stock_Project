package app;

import Service.LoginService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main extends JFrame {
    private static LoginService loginService = new LoginService();
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JTextField usernameField, signupUsernameField;
    private JPasswordField passwordField, signupPasswordField;
    private JLabel statusLabel;

    public Main() {
        setTitle("Stock App - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // Create card layout for switching between login and signup panels
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Create login panel
        JPanel loginPanel = createLoginPanel();

        // Create signup panel
        JPanel signupPanel = createSignupPanel();

        // Add panels to card layout
        cardPanel.add(loginPanel, "login");
        cardPanel.add(signupPanel, "signup");

        add(cardPanel, BorderLayout.CENTER);

        // Status label at the bottom
        statusLabel = new JLabel(" ", JLabel.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Show login panel by default
        cardLayout.show(cardPanel, "login");
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Login to Stock App", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, gbc);

        // Username
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        // Login button
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> handleLogin());
        panel.add(loginButton, gbc);

        // Switch to signup
        gbc.gridy++;
        JLabel signupPrompt = new JLabel("Don't have an account? ");
        JButton switchToSignup = new JButton("Sign Up");
        switchToSignup.setBorderPainted(false);
        switchToSignup.setContentAreaFilled(false);
        switchToSignup.setForeground(Color.BLUE);
        switchToSignup.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchToSignup.addActionListener(e -> cardLayout.show(cardPanel, "signup"));

        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        switchPanel.add(signupPrompt);
        switchPanel.add(switchToSignup);
        panel.add(switchPanel, gbc);

        // Add enter key listener for login
        passwordField.addActionListener(e -> handleLogin());

        return panel;
    }

    private JPanel createSignupPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Create Account", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, gbc);

        // Username
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Choose a username:"), gbc);
        gbc.gridx = 1;
        signupUsernameField = new JTextField(15);
        panel.add(signupUsernameField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Choose a password:"), gbc);
        gbc.gridx = 1;
        signupPasswordField = new JPasswordField(15);
        panel.add(signupPasswordField, gbc);

        // Sign Up button
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JButton signupButton = new JButton("Sign Up");
        signupButton.addActionListener(e -> handleSignup());
        panel.add(signupButton, gbc);

        // Switch to login
        gbc.gridy++;
        JLabel loginPrompt = new JLabel("Already have an account? ");
        JButton switchToLogin = new JButton("Login");
        switchToLogin.setBorderPainted(false);
        switchToLogin.setContentAreaFilled(false);
        switchToLogin.setForeground(Color.BLUE);
        switchToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchToLogin.addActionListener(e -> {
            cardLayout.show(cardPanel, "login");
            clearFields();
        });

        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        switchPanel.add(loginPrompt);
        switchPanel.add(switchToLogin);
        panel.add(switchPanel, gbc);

        return panel;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please fill in all fields", Color.RED);
            return;
        }

        if (loginService.login(username, password)) {
            showStatus("Login successful! Opening dashboard...", new Color(0, 150, 0));

            // --- NEW DASHBOARD LOGIC ---
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Small delay for status message to register
                    SwingUtilities.invokeLater(() -> {
                        // 1. Dispose of the current login window
                        this.dispose();
                        // 2. Open the main application dashboard
                        Dashboard dashboard = new Dashboard(username);
                        dashboard.setVisible(true);
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
            // --- END NEW DASHBOARD LOGIC ---

        } else {
            showStatus("Invalid username or password", Color.RED);
        }
    }

    private void handleSignup() {
        String username = signupUsernameField.getText().trim();
        String password = new String(signupPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please fill in all fields", Color.RED);
            return;
        }

        if (loginService.signUp(username, password)) {
            showStatus("Account created successfully!", new Color(0, 150, 0));
            // Switch back to login after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    SwingUtilities.invokeLater(() -> {
                        cardLayout.show(cardPanel, "login");
                        clearFields();
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        } else {
            showStatus("Username already exists", Color.RED);
        }
    }

    private void showStatus(String message, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(message);
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        signupUsernameField.setText("");
        signupPasswordField.setText("");
        statusLabel.setText(" ");
    }

    public static void main(String[] args) {
        // Set look and feel for better appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and show the login window
        SwingUtilities.invokeLater(() -> {
            Main loginWindow = new Main();
            loginWindow.setVisible(true);
        });
    }
}