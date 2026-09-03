package com.rcpky.bridgedesign.reportgeneration.reports;

import com.rcpky.bridgedesign.reportgeneration.Report;
import com.rcpky.bridgedesign.reportgeneration.ReportExporter;

public class FinancialReport extends Report {

    public FinancialReport(ReportExporter exporter) {
        super(exporter);
    }

    @Override
    public void generate() {
        String data = "Revenue: $200,000 | Expenses: $120,000 | Net Profit: $80,000";
        exporter.export("Financial Report", data);
    }
}
