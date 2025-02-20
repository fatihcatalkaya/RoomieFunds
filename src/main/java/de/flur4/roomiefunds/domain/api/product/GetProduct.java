package de.flur4.roomiefunds.domain.api.product;

import de.flur4.roomiefunds.models.product.Product;

import java.util.List;
import java.util.Optional;

public interface GetProduct {
    Optional<Product> getProduct(long productId);
    List<Product> getProducts();
}
