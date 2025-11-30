package interface_adapter.filter_search;

import interface_adapter.ViewManagerModel;
import use_case.filter_search.FilterSearchOutputBoundary;
import use_case.filter_search.FilterSearchResponse;

/**
 * The Presenter for the Filter Search Use Case.
 */

public class FilterSearchPresenter implements FilterSearchOutputBoundary{
    private final FilterSearchViewModel filterSearchViewModel;

    public FilterSearchPresenter(FilterSearchViewModel filterSearchViewModel, ViewManagerModel viewManagerModel) {

        this.filterSearchViewModel = filterSearchViewModel;
    }

    @Override
    public void prepareSuccessView(FilterSearchResponse response) {

        filterSearchViewModel.setStocks(response.getStocks());
        filterSearchViewModel.setState(new FilterSearchState());
        filterSearchViewModel.firePropertyChange();

    }

    @Override
    public void prepareFailView(String message) {
        filterSearchViewModel.setStocks(null);
        filterSearchViewModel.setErrorMessage(message);
        filterSearchViewModel.firePropertyChange();
    }

}
