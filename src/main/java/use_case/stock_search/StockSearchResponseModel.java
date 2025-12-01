package use_case.stock_search;

public class StockSearchResponseModel {
    private final String symbol;
    private final double currentPrice;
    private final double open;
    private final double high;
    private final double low;
    private final double previousClose;

    public StockSearchResponseModel(String symbol,
                                    double currentPrice,
                                    double open,
                                    double high,
                                    double low,
                                    double previousClose) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.open = open;
        this.high = high;
        this.low = low;
        this.previousClose = previousClose;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getPreviousClose() {
        return previousClose;
    }
}
