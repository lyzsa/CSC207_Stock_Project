package use_case.filter_search;

import use_case.login.LoginInputData;

/**
 * Input Boundary for the Filter Search Use Case.
 */

public interface FilterSearchInputBoundary {
    /**
     * Executes the filter search use case.
     * @param filterSearchInputData the input data
     */
    void execute(FilterSearchInputData filterSearchInputData);
}
