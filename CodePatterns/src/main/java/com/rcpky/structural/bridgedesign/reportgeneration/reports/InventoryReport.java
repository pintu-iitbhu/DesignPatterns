package com.rcpky.structural.bridgedesign.reportgeneration.reports;

import com.rcpky.structural.bridgedesign.reportgeneration.Report;
import com.rcpky.structural.bridgedesign.reportgeneration.ReportExporter;

public class InventoryReport extends Report {

    public InventoryReport(ReportExporter exporter) {
        super(exporter);
    }

    @Override
    public void generate() {
        String data = "Items In Stock: 500 | Low Stock: 30 | Out of Stock: 5";
        exporter.export("Inventory Report", data);
    }
}
