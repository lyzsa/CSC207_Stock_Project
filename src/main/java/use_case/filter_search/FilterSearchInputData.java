package use_case.filter_search;

/**
 * The Input Data for the Filter Search Use Case.
 */

public class FilterSearchInputData {

    private final String exchange;
    private final String mic;
    private final String securityType;
    private final String currency;

    public FilterSearchInputData(String exchange, String mic, String securityType, String currency) {
        this.exchange = exchange;
        this.mic = mic;
        this.securityType = securityType;
        this.currency = currency;
    }

    String getExchange() { return this.exchange; }

    String getMic() { return this.mic; }

    String getSecurityType() { return this.securityType; }

    String getCurrency() { return this.currency; }

}
