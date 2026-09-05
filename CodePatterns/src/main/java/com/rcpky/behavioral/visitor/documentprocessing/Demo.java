package com.rcpky.behavioral.visitor.documentprocessing;

import com.rcpky.behavioral.visitor.documentprocessing.documents.ExcelDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.PdfDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.WordDocument;
import com.rcpky.behavioral.visitor.documentprocessing.operations.MetadataExportOperation;
import com.rcpky.behavioral.visitor.documentprocessing.operations.SecurityScanOperation;
import com.rcpky.behavioral.visitor.documentprocessing.operations.StorageSizeOperation;

import java.util.ArrayList;
import java.util.List;

public class Demo {

    public  static void main(String[] args) {
        Document pdfDocument = new PdfDocument();
        Document wordDocument = new WordDocument();
        Document excelDocument = new ExcelDocument();

        List<Operation> operations = new ArrayList<>();
        operations.add(new StorageSizeOperation());
        operations.add(new MetadataExportOperation());
        operations.add(new SecurityScanOperation());

        pdfDocument.accept(operations);
        System.out.println("-------------------");
        wordDocument.accept(operations);
        System.out.println("-------------------");
        excelDocument.accept(operations);
    }
}
