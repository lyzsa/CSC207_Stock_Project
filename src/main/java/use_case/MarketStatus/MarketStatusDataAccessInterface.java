package use_case.MarketStatus;

import entity.MarketStatus;

public interface MarketStatusDataAccessInterface {
    MarketStatus loadStatus() throws Exception;
}
