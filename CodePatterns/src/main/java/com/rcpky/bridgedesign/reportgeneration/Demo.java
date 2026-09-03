package com.rcpky.bridgedesign.reportgeneration;

import com.rcpky.bridgedesign.reportgeneration.exports.ExcelReportExporter;
import com.rcpky.bridgedesign.reportgeneration.exports.PdfReportExporter;
import com.rcpky.bridgedesign.reportgeneration.reports.FinancialReport;
import com.rcpky.bridgedesign.reportgeneration.reports.InventoryReport;
import com.rcpky.bridgedesign.reportgeneration.reports.SalesReport;

public class Demo {

    public static void main(String[] args) {
        Report salesPdf =
                new SalesReport(new PdfReportExporter());
        salesPdf.generate();

        Report financialExcel =
                new FinancialReport(new ExcelReportExporter());
        financialExcel.generate();

        Report inventoryPdf =
                new InventoryReport(new PdfReportExporter());
        inventoryPdf.generate();

        Report salesExcel =
                new SalesReport(new ExcelReportExporter());
        salesExcel.generate();
    }
}
