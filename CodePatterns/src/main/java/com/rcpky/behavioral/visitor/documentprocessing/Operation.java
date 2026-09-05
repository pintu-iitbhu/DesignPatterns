package com.rcpky.behavioral.visitor.documentprocessing;

import com.rcpky.behavioral.visitor.documentprocessing.documents.ExcelDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.PdfDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.WordDocument;

public interface Operation {
    void apply(PdfDocument document);
    void apply(WordDocument document);
    void apply(ExcelDocument document);
}
