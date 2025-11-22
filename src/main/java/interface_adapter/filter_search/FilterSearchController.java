package interface_adapter.filter_search;

import use_case.filter_search.FilterSearchInputBoundary;
import use_case.filter_search.FilterSearchInputData;

public class FilterSearchController {
    private final FilterSearchInputBoundary filterSearchInputBoundary;

    public FilterSearchController(FilterSearchInputBoundary filterSearchInputBoundary) {
        this.filterSearchInputBoundary = filterSearchInputBoundary;
    }

    /**
     * Executes the Change Password Use Case.
     * @param exchange the exchange
     * @param mic the mic
     * @param securityType the security type
     * @param currency the currency
     */
    public void execute(String exchange, String mic, String securityType, String currency) {
        final FilterSearchInputData filterSearchInputData = new FilterSearchInputData(exchange, mic, securityType, currency);

        filterSearchInputBoundary.execute(filterSearchInputData);
    }
}
