package view;

import data_access.FilterSearchDataAccessObject;
import entity.Stock;
import interface_adapter.filter_search.FilterSearchController;
import interface_adapter.filter_search.FilterSearchState;
import interface_adapter.filter_search.FilterSearchViewModel;
import use_case.filter_search.FilterSearchInputBoundary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The View for when the user is in the Filter Search Page.
 */

public class FilterSearchView extends JPanel implements PropertyChangeListener {
    private FilterSearchController filterSearchController;

    private String ex;
    private String mi;
    private String sec;
    private String curr;

    private static final int COL_SYMBOL = 0;
    private static final int COL_DESCRIPTION  = 1;
    private static final int COL_CURRENCY    = 2;
    private static final int COL_DISPLAY   = 3;
    private static final int COL_FIGI = 4;
    private static final int COL_MIC  = 5;
    private static final int COL_TYPE = 6;



    public FilterSearchView(FilterSearchViewModel filterSearchViewModel) {
        filterSearchViewModel.addPropertyChangeListener(this);

        final String[] exchangeOptions = FilterSearchInputBoundary.EXCHANGE_OPTIONS;
        final String[] micOptions = FilterSearchInputBoundary.MIC_OPTIONS;
        final String[] securityOptions = FilterSearchInputBoundary.SECURITY_OPTIONS;
        final String[] currencyOptions = FilterSearchInputBoundary.CURRENCY_OPTIONS;


        final JLabel title = new JLabel("Filter Search");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JComboBox<String> exchangeDrop = new JComboBox<String>(exchangeOptions);
        final JComboBox<String> micDrop = new JComboBox<String>(micOptions);
        final JComboBox<String> securityDrop = new JComboBox<String>(securityOptions);
        final JComboBox<String> currencyDrop = new JComboBox<String>(currencyOptions);
        JButton search = new JButton("Search");

        this.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        this.add(topPanel, BorderLayout.NORTH);

        JPanel panel_e = new JPanel();
        JLabel exchange = new JLabel("Exchange");
        panel_e.add(exchange);
        panel_e.add(exchangeDrop);

        JPanel panel_m = new JPanel();
        JLabel mic = new JLabel("MIC:");
        panel_m.add(mic);
        panel_m.add(micDrop);

        JPanel panel_s = new JPanel();
        JLabel securityType = new JLabel("Security Type:");
        panel_s.add(securityType);
        panel_s.add(securityDrop);

        JPanel panel_c = new JPanel();
        JLabel currency = new JLabel("Currency:");
        panel_c.add(currency);
        panel_c.add(currencyDrop);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backToHomeButton = new JButton("Back");
        leftPanel.add(backToHomeButton);
        topPanel.add(leftPanel, BorderLayout.WEST);

        topPanel.add(panel_e, BorderLayout.PAGE_START);
        topPanel.add(panel_m, BorderLayout.PAGE_START);
        topPanel.add(panel_s, BorderLayout.PAGE_START);
        topPanel.add(panel_c, BorderLayout.PAGE_START);
        topPanel.add(search, BorderLayout.PAGE_START);

        JPanel mainPanel = new JPanel();

        String[] columnNames = {"Symbol", "Description", "Currency", "Display Symbol", "FIGI", "MIC", "Security Type"};

        // Home Button
        backToHomeButton.addActionListener(e -> {
            // TODO: implement navigation back to the main/home view.
            // For now, you can leave this empty or show a message:
            JOptionPane.showMessageDialog(
                    this,
                    "Back to Home is not implemented yet.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });


        // Search Button
        search.addActionListener(e -> {
            String ex = exchangeDrop.getSelectedItem().toString();
            String mi = null;
            String sec = null;
            String curr = null;
            if (micDrop.getSelectedItem() != null) {
                mi = micDrop.getSelectedItem().toString();
            }

            if (securityDrop.getSelectedItem() != null) {
                sec = securityDrop.getSelectedItem().toString();
            }

            if (currencyDrop.getSelectedItem() != null) {
                curr = currencyDrop.getSelectedItem().toString();
            }

            filterSearchController.loadStocks(ex, mi, sec, curr);
            data_access.FilterSearchDataAccessObject obj = new
                    FilterSearchDataAccessObject("d4lpdgpr01qr851prp30d4lpdgpr01qr851prp3g");
            List<Stock> res;
            try {
                res = obj.loadStocks(ex, mi, sec, curr);

                Object[][] data = new Object[res.size()][7];

                for (int i = 0; i < res.size(); i++) {
                    Object[] temp = new Object[7];
                    temp[0] = res.get(i).getSymbol();
                    temp[1] = res.get(i).getDescription();
                    temp[2] = res.get(i).getCurrency();
                    temp[3] = res.get(i).getDisplaySymbol();
                    temp[4] = res.get(i).getFigi();
                    temp[5] = res.get(i).getMic();
                    temp[6] = res.get(i).getType();


                    data[i] =  temp;
                }

                JTable table = new JTable(data, columnNames);
                table.setPreferredScrollableViewportSize(new Dimension(800, 800));
                table.setFillsViewportHeight(true);

                JScrollPane scrollPane = new JScrollPane(table);

                mainPanel.add(scrollPane, BorderLayout.CENTER);

                this.add(mainPanel, BorderLayout.CENTER);
            } catch (Exception exc) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error while loading stocks: " + exc.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }



        });




    }

    public String getViewName() {
        String viewName = "Filter Search";
        return viewName;
    }

    public void setFilterSearchController(FilterSearchController filterSearchController) {
        this.filterSearchController = filterSearchController;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final FilterSearchState state = (FilterSearchState) evt.getNewValue();
    }


}
