package com.rcpky.structural.proxy.documentservice;

public class SecureDocumentServiceProxy implements DocumentService {

    private final DocumentService documentService;

    public SecureDocumentServiceProxy(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public Document getDocument(String documentId) {

        User user = SecurityContext.getCurrentUser();

        if (user == null) {
            throw new SecurityException(
                    "User is not authenticated"
            );
        }

        authorize(user, documentId);

        return documentService.getDocument(documentId);
    }

    private void authorize(User user, String documentId) {

        // ADMIN can access everything
        if ("ADMIN".equals(user.getRole())) {
            return;
        }

        // USER can access only public documents
        if ("USER".equals(user.getRole())
                && isPublicDocument(documentId)) {
            return;
        }

        throw new SecurityException(
                "User does not have permission to access this document"
        );
    }

    private boolean isPublicDocument(String documentId) {
        // Example implementation
        return documentId.startsWith("PUBLIC");
    }
}
