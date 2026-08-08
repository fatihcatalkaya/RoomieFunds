package de.flur4.roomiefunds.domain.api.transaction;

public class InvalidReceiptContentTypeException extends Exception {
    public InvalidReceiptContentTypeException(String contentType) {
        super("Receipt content type '%s' is not allowed, only images and PDFs are supported".formatted(contentType));
    }
}
