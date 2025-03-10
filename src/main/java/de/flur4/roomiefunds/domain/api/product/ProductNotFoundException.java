package de.flur4.roomiefunds.domain.api.product;

public class ProductNotFoundException extends Exception {
    public ProductNotFoundException(long productId) {
        super("Could not find product with id %d".formatted(productId));
    }
}
