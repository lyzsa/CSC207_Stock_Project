package interface_adapter.NewsPage;

import use_case.News.NewsInputBoundary;
import use_case.News.NewsOutputBoundary;
import use_case.News.NewsResponseModel;

/**
 * receives output data from interactor
 * writes the data through ViewModel nad then update the state
 */
public class NewsPresenter implements NewsOutputBoundary {

    private final NewsViewModel viewModel;

    public  NewsPresenter(NewsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(NewsResponseModel responseModel) {
        viewModel.setArticles(responseModel.getArticles());
        viewModel.setErrorMessage(null);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setArticles(null);
        viewModel.setErrorMessage(errorMessage);
        viewModel.firePropertyChanged();
    }
}

