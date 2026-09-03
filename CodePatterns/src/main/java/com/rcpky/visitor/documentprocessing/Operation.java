package com.rcpky.visitor.documentprocessing;

import com.rcpky.visitor.documentprocessing.documents.ExcelDocument;
import com.rcpky.visitor.documentprocessing.documents.PdfDocument;
import com.rcpky.visitor.documentprocessing.documents.WordDocument;

public interface Operation {
    void apply(PdfDocument document);
    void apply(WordDocument document);
    void apply(ExcelDocument document);
}
