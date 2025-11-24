package use_case.News;

/**
 * Interface implemented by the presenter
 */
public interface NewsOutputBoundary {

    void prepareSuccessView(NewsResponseModel responseModel);

    void prepareFailView(String errorMessage);
}
