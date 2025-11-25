package app;

import okhttp3.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A JPanel that shows historical company earnings using Finnhub's REST API.
 * NOTE: This version also avoids JSON libraries and uses manual String parsing.
 */
public class EarningsPage extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(EarningsPage.class.getName());

    // Finnhub API
    private static final String API_KEY = "d4977ehr01qshn3kvpt0d4977ehr01qshn3kvptg"; // <- replace
    private static final String BASE_URL = "https://finnhub.io/api/v1/stock/earnings";

    // UI components
    private final JTextField symbolField = new JTextField("AAPL", 10);
    private final JButton loadButton = new JButton("Load Earnings");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Period", "Actual", "Estimate", "Surprise"}, 0
    );
    private final JTable table = new JTable(tableModel);

    public EarningsPage() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Company Earnings"));

        // --- Top controls ---
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Symbol:"));
        topPanel.add(symbolField);
        topPanel.add(loadButton);
        add(topPanel, BorderLayout.NORTH);

        // --- Table ---
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Button action
        loadButton.addActionListener(e -> fetchEarnings());
    }

    /**
     * Starts a background thread to fetch earnings for the given symbol.
     */
    private void fetchEarnings() {
        final String symbol = symbolField.getText().trim().toUpperCase();
        if (symbol.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a symbol.");
            return;
        }

        loadButton.setEnabled(false);

        new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(10, TimeUnit.SECONDS)
                    .build();

            HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                    .addQueryParameter("symbol", symbol)
                    .addQueryParameter("limit", "10")
                    .addQueryParameter("token", API_KEY)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String body = response.body().string();

// If API returns error object
                if (body.contains("\"error\"")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "Company not found or not supported.\nPlease check the symbol.",
                                "Symbol Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        tableModel.setRowCount(0);  // clear table
                        loadButton.setEnabled(true);
                    });
                    return;
                }


                List<EarningsRow> rows = parseEarningsJson(body);

// If empty results
                if (rows.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "No earnings data found for this company.",
                                "No Data",
                                JOptionPane.WARNING_MESSAGE
                        );
                        tableModel.setRowCount(0);
                        loadButton.setEnabled(true);
                    });
                    return;
                }

// Normal success case
                SwingUtilities.invokeLater(() -> {
                    updateTable(rows);
                    loadButton.setEnabled(true);
                });


            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error fetching earnings", ex);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "Network error or API issue.\nCheck your internet or API key.",
                            "Connection Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    loadButton.setEnabled(true);
                });
            }

        }).start();
    }

    /**
     * Clear table and add new rows.
     */
    private void updateTable(List<EarningsRow> rows) {
        tableModel.setRowCount(0);
        for (EarningsRow r : rows) {
            tableModel.addRow(new Object[]{
                    r.period,
                    r.actual != null ? String.format("%.3f", r.actual) : "N/A",
                    r.estimate != null ? String.format("%.3f", r.estimate) : "N/A",
                    r.surprise != null ? String.format("%.3f", r.surprise) : "N/A"
            });
        }
    }


    private static class EarningsRow {
        String period;
        Double actual;
        Double estimate;
        Double surprise;
    }

    /**
     * Expect Finnhub to return a JSON array:
     * [ { "period": "...", "actual": ..., "estimate": ..., "surprise": ... }, ... ]
     */
    private List<EarningsRow> parseEarningsJson(String json) {
        List<EarningsRow> rows = new ArrayList<>();
        if (json == null) return rows;

        String trimmed = json.trim();
        if (trimmed.length() < 2 || trimmed.equals("[]")) return rows;

        // Remove [ and ]
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        // Split by "},{" between objects
        String[] objects = trimmed.split("\\},\\s*\\{");

        for (String obj : objects) {
            obj = obj.trim();
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";

            EarningsRow row = new EarningsRow();
            row.period = extractValue(obj, "period");

            String actualStr = extractValue(obj, "actual");
            String estimateStr = extractValue(obj, "estimate");
            String surpriseStr = extractValue(obj, "surprise");

            row.actual = parseDoubleOrNull(actualStr);
            row.estimate = parseDoubleOrNull(estimateStr);
            row.surprise = parseDoubleOrNull(surpriseStr);

            rows.add(row);
        }
        return rows;
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isEmpty() || s.equals("null")) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Basic key lookup
     */
    private String extractValue(String json, String key) {
        String keySearch = "\"" + key + "\":";
        int keyIndex = json.indexOf(keySearch);
        if (keyIndex == -1) return null;

        int valueStart = keyIndex + keySearch.length();
        if (valueStart >= json.length()) return null;

        char firstChar = json.charAt(valueStart);

        // String value in quotes
        if (firstChar == '"') {
            valueStart++;
            int valueEnd = json.indexOf('"', valueStart);
            if (valueEnd != -1) {
                return json.substring(valueStart, valueEnd);
            }
        } else { // number / null / boolean
            int indexComma = json.indexOf(',', valueStart);
            int indexCurly = json.indexOf('}', valueStart);

            int terminationIndex = json.length();
            if (indexComma != -1) terminationIndex = Math.min(terminationIndex, indexComma);
            if (indexCurly != -1) terminationIndex = Math.min(terminationIndex, indexCurly);

            return json.substring(valueStart, terminationIndex).trim();
        }
        return null;
    }
}
