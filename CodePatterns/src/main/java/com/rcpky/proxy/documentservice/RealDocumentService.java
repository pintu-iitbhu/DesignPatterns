package com.rcpky.proxy.documentservice;

public class RealDocumentService implements  DocumentService {
    @Override
    public Document getDocument(String id) {
        System.out.println("Fetching document from storage...");
        return new Document(id, "Confidential Content");    }
}
