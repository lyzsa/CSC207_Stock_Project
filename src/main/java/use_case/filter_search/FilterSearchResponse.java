package use_case.filter_search;

import entity.Stock;

import java.util.List;

public class FilterSearchResponse {
    private final List<Stock> result;

    public FilterSearchResponse(List<Stock> result) {
        this.result = result;
    }

    public List<Stock> getStocks() {
        return this.result;
    }
}
