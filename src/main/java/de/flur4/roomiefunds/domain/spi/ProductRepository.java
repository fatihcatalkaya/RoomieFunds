package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.product.CreateProductDto;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.product.UpdateProductDto;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> getProductById(long productId);

    List<Product> getAllProducts();

    Product createProduct(CreateProductDto createProductDto);

    Product updateProduct(long productId, UpdateProductDto updateProductDto);

    void deleteProduct(long productId);
}
