package com.rcpky.structural.bridgedesign.reportgeneration;

public abstract class Report {

    protected final ReportExporter exporter;

    protected Report(ReportExporter exporter) {
        this.exporter = exporter;
    }

    public abstract void generate();
}
