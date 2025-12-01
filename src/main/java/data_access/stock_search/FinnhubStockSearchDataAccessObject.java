package data_access.stock_search;

import data_access.*;
import entity.StockQuote;
import use_case.stock_search.StockSearchDataAccessInterface;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FinnhubStockSearchDataAccessObject implements StockSearchDataAccessInterface {
    private static final String BASE_URL = "https://finnhub.io/api/v1/quote";

    private final String apiKey;

    public FinnhubStockSearchDataAccessObject(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public StockQuote loadQuote(String symbol) throws Exception {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be empty.");
        }

        StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                .append("?symbol=")
                .append(URLEncoder.encode(symbol, StandardCharsets.UTF_8))
                .append("&token=")
                .append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));

        JSONObject json = fetchJsonObject(urlBuilder.toString());
        return parseQuote(symbol, json);
    }

    private JSONObject fetchJsonObject(String urlString) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                String errorBody = readStream(connection.getErrorStream());
                throw new IOException("HTTP error " + status + " from Finnhub: " + errorBody);
            }

            inputStream = connection.getInputStream();
            String body = readStream(inputStream);
            return new JSONObject(body);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private StockQuote parseQuote(String symbol, JSONObject obj) {
        if (obj == null) {
            throw new IllegalStateException("Empty response from Finnhub quote endpoint.");
        }

        double current = obj.optDouble("c", Double.NaN);
        double open = obj.optDouble("o", Double.NaN);
        double high = obj.optDouble("h", Double.NaN);
        double low = obj.optDouble("l", Double.NaN);
        double prevClose = obj.optDouble("pc", Double.NaN);
        long t = obj.optLong("t", 0L);

        return new StockQuote(symbol, current, open, high, low, prevClose, t);
    }
}
