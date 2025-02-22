package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.product.*;
import de.flur4.roomiefunds.domain.api.product.impl.ProductService;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.ProductRepository;
import de.flur4.roomiefunds.domain.spi.ProductTallyListRenderer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class ProductContext {
    @Produces
    @ApplicationScoped
    public CreateProduct createProduct(ProductRepository productRepository, LogRepository logRepository, ProductTallyListRenderer tallyListRenderer) {
        return new ProductService(productRepository, logRepository, tallyListRenderer);
    }

    @Produces
    @ApplicationScoped
    public GetProductTallySheet getProductTallySheet(ProductRepository productRepository, LogRepository logRepository, ProductTallyListRenderer tallyListRenderer) {
        return new ProductService(productRepository, logRepository, tallyListRenderer);
    }

    @Produces
    @ApplicationScoped
    public UpdateProduct updateProduct(ProductRepository productRepository, LogRepository logRepository, ProductTallyListRenderer tallyListRenderer) {
        return new ProductService(productRepository, logRepository, tallyListRenderer);
    }

    @Produces
    @ApplicationScoped
    public DeleteProduct deleteProduct(ProductRepository productRepository, LogRepository logRepository, ProductTallyListRenderer tallyListRenderer) {
        return new ProductService(productRepository, logRepository, tallyListRenderer);
    }

    @Produces
    @ApplicationScoped
    public GetProduct getProduct(ProductRepository productRepository, LogRepository logRepository, ProductTallyListRenderer tallyListRenderer) {
        return new ProductService(productRepository, logRepository, tallyListRenderer);
    }
}
