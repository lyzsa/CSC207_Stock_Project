package interface_adapter.filter_search;

import entity.Stock;
import interface_adapter.ViewModel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

/**
 * The View Model for the Filter Search View.
 */

public class FilterSearchViewModel extends ViewModel<FilterSearchState> {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private List<Stock> stocks;
    private String errorMessage;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public FilterSearchViewModel() {
        super("Filter Search");
        setState(new FilterSearchState());

    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
