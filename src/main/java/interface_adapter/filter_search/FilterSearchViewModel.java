package interface_adapter.filter_search;

import interface_adapter.ViewModel;

/**
 * The View Model for the Filter Search View.
 */

public class FilterSearchViewModel extends ViewModel<FilterSearchState> {

    public FilterSearchViewModel() {
        super("Filter Search");
        setState(new FilterSearchState());
    }
}
