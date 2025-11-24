package use_case.MarketStatus;

import entity.MarketStatus;

public class MarketStatusResponseModel {

    private final MarketStatus marketStatus;

    public MarketStatusResponseModel(MarketStatus marketStatus) {
        this.marketStatus = marketStatus;
    }

    public MarketStatus getMarketStatus() {return marketStatus;}
}