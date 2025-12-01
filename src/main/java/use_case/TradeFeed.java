package use_case;

/**
 * Abstraction for any real-time trade data source.
 * Infrastructure implementations (e.g., Finnhub WebSocket) should implement this.
 */
public interface TradeFeed {

    /**
     * Starts or (re)connects the feed and begins streaming trades to the listener.
     * @param symbol The trading symbol to subscribe to (e.g., "BINANCE:BTCUSDT", "AAPL")
     * @param listener The listener to receive trade updates
     */
    void connect(String symbol, TradeListener listener);

    /**
     * Gracefully disconnects the feed.
     */
    void disconnect();
}

