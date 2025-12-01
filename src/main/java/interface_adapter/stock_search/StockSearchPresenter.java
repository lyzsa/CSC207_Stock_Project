package interface_adapter.stock_search;

import use_case.stock_search.StockSearchOutputBoundary;
import use_case.stock_search.StockSearchResponseModel;

public class StockSearchPresenter implements StockSearchOutputBoundary {

    private final StockSearchViewModel viewModel;

    public StockSearchPresenter(StockSearchViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(StockSearchResponseModel responseModel) {
        StringBuilder sb = new StringBuilder();
        sb.append(responseModel.getSymbol()).append("\n");
        sb.append("Current: ").append(responseModel.getCurrentPrice()).append("\n");
        sb.append("Open:    ").append(responseModel.getOpen()).append("\n");
        sb.append("High:    ").append(responseModel.getHigh()).append("\n");
        sb.append("Low:     ").append(responseModel.getLow()).append("\n");
        sb.append("Prev:    ").append(responseModel.getPreviousClose()).append("\n");

        viewModel.setInfoText(sb.toString());
        viewModel.setErrorMessage(null);
        viewModel.setLoading(false);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setInfoText(null);
        viewModel.setErrorMessage(errorMessage);
        viewModel.setLoading(false);
        viewModel.firePropertyChanged();
    }
}
