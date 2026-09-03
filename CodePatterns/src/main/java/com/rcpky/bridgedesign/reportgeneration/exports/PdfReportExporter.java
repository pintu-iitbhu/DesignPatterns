package com.rcpky.bridgedesign.reportgeneration.exports;

import com.rcpky.bridgedesign.reportgeneration.ReportExporter;

public class PdfReportExporter implements ReportExporter {

    @Override
    public void export(String title, String data) {
        System.out.println(
                "Exporting [" + title + "] to PDF: " + data
        );
    }
}
