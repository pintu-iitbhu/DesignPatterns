package com.rcpky.structural.bridgedesign.reportgeneration.reports;

import com.rcpky.structural.bridgedesign.reportgeneration.Report;
import com.rcpky.structural.bridgedesign.reportgeneration.ReportExporter;

public class SalesReport extends Report {

    public SalesReport(ReportExporter exporter) {
        super(exporter);
    }

    @Override
    public void generate() {
        String data = "Total Sales: $50,000 | Units Sold: 1200";
        exporter.export("Sales Report", data);
    }
}
