package com.rcpky.proxy.documentservice;

public class Document {
    private String id;
    private String content;

    public Document(String id, String content) {
        this.id = id;
        this.content = content;
    }


    public boolean isDocumentPublic(String documentId){
        System.out.println("Checking if document is public...");
        return documentId.equals(id);
    }

    public String getContent() {
        return content;
    }
}
