package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.domain.api.product.ProductNotFoundException;
import de.flur4.roomiefunds.models.product.CreateProductDto;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.product.UpdateProductDto;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> getProductById(long productId);

    List<Product> getAllProducts();

    List<Product> getProductsToPrintOnTallyList();

    Product createProduct(CreateProductDto createProductDto);

    Product updateProduct(long productId, UpdateProductDto updateProductDto) throws ProductNotFoundException;

    void deleteProduct(long productId) throws DataAccessException;
}
