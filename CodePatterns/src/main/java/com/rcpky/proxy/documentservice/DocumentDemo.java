package com.rcpky.proxy.documentservice;

public class DocumentDemo {

    public  static void main(String[] args) {
        DocumentService service =
                new SecureDocumentServiceProxy(
                        new RealDocumentService()
                );

        User user = new User(
                "user-123",
                "USER"
        );

        try {
            SecurityContext.setUser(user);

            Document document =
                    service.getDocument("PUBLIC-001");

            System.out.println(document.getContent());

        } finally {
            SecurityContext.clear();
        }
    }


}
