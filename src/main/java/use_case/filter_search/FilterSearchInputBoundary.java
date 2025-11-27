package use_case.filter_search;

/**
 * Input Boundary for the Filter Search Use Case.
 */

public interface FilterSearchInputBoundary {
    /**
     * Executes the filter search use case.
     * @param request the input data
     */
    void execute(FilterSearchRequest request);
}
