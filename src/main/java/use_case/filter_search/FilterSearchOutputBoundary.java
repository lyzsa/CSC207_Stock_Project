package use_case.filter_search;

import use_case.filter_search.FilterSearchOutputData;

/**
 * The output boundary for the Filter Search Use Case.
 */

public interface FilterSearchOutputBoundary {
    /**
     * Prepares the success view for the Filter Search Use Case.
     * @param outputData the output data
     */
    void prepareSuccessView(FilterSearchOutputData outputData);
}
