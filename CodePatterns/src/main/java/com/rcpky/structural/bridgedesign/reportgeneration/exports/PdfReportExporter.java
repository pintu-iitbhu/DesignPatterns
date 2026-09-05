package com.rcpky.structural.bridgedesign.reportgeneration.exports;

import com.rcpky.structural.bridgedesign.reportgeneration.ReportExporter;

public class PdfReportExporter implements ReportExporter {

    @Override
    public void export(String title, String data) {
        System.out.println(
                "Exporting [" + title + "] to PDF: " + data
        );
    }
}
