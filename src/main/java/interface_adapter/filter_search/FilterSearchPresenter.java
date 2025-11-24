package interface_adapter.filter_search;

import interface_adapter.ViewManagerModel;
import use_case.filter_search.FilterSearchOutputBoundary;
import use_case.filter_search.FilterSearchOutputData;

/**
 * The Presenter for the Filter Search Use Case.
 */

public class FilterSearchPresenter implements FilterSearchOutputBoundary{
    private final FilterSearchViewModel filterSearchViewModel;
    private final ViewManagerModel viewManagerModel;

    public FilterSearchPresenter(FilterSearchViewModel filterSearchViewModel, ViewManagerModel viewManagerModel) {
        this.viewManagerModel = viewManagerModel;
        this.filterSearchViewModel = filterSearchViewModel;
    }

    @Override
    public void prepareSuccessView(FilterSearchOutputData filterSearchOutputData) {
        this.filterSearchViewModel.firePropertyChange();
        filterSearchViewModel.setState(new FilterSearchState());

        this.viewManagerModel.setState(filterSearchViewModel.getViewName());
        this.viewManagerModel.firePropertyChange();

    }
}
