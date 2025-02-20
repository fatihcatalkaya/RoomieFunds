package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.product.CreateProduct;
import de.flur4.roomiefunds.domain.api.product.DeleteProduct;
import de.flur4.roomiefunds.domain.api.product.GetProduct;
import de.flur4.roomiefunds.domain.api.product.UpdateProduct;
import de.flur4.roomiefunds.domain.api.product.impl.ProductApiImpl;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class ProductContext {
    @Produces
    @ApplicationScoped
    public CreateProduct createProduct(ProductRepository productRepository, LogRepository logRepository) {
        return new ProductApiImpl(productRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdateProduct updateProduct(ProductRepository productRepository, LogRepository logRepository) {
        return new ProductApiImpl(productRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteProduct deleteProduct(ProductRepository productRepository, LogRepository logRepository) {
        return new ProductApiImpl(productRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public GetProduct getProduct(ProductRepository productRepository, LogRepository logRepository) {
        return new ProductApiImpl(productRepository, logRepository);
    }
}
