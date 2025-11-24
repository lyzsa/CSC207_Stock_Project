package interface_adapter.MarketStatus;

import use_case.MarketStatus.MarketStatusInputBoundary;

public class MarketStatusController {

    private final MarketStatusInputBoundary interactor;

    public MarketStatusController(MarketStatusInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void updateStatus() {
        interactor.execute();
    }
}
