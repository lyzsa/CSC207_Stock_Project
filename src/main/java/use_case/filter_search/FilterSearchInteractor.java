package use_case.filter_search;

import use_case.login.LoginInputBoundary;

/**
 * The Filter Search Interactor.
 */

public class FilterSearchInteractor implements FilterSearchInputBoundary {
    private final FilterSearchOutputBoundary filterSearchPresenter;

    public FilterSearchInteractor(FilterSearchOutputBoundary filterSearchOutputBoundary) {
        this.filterSearchPresenter = filterSearchOutputBoundary;
    }

    @Override
    public void execute(FilterSearchInputData filterSearchInputData) {
        final String exchange =  filterSearchInputData.getExchange();
        final String mic =  filterSearchInputData.getMic();
        final String securityType = filterSearchInputData.getSecurityType();
        final String currency =  filterSearchInputData.getCurrency();

        final FilterSearchOutputData filterSearchOutputData = new FilterSearchOutputData();
        filterSearchPresenter.prepareSuccessView(filterSearchOutputData);
    }
}
