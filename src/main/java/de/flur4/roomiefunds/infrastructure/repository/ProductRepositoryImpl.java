package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.api.product.ProductNotFoundException;
import de.flur4.roomiefunds.domain.spi.ProductRepository;
import de.flur4.roomiefunds.models.product.CreateProductDto;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.product.UpdateProductDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.PRODUCT;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    final DSLContext jooq;

    @Override
    public Optional<Product> getProductById(long productId) {
        return jooq.select(
                        PRODUCT.ID,
                        PRODUCT.NAME,
                        PRODUCT.PRICE,
                        PRODUCT.PRINT
                ).from(PRODUCT).where(PRODUCT.ID.eq(productId))
                .fetchOptional(mapping(Product::new));
    }

    @Override
    public List<Product> getAllProducts() {
        return jooq.select(
                        PRODUCT.ID,
                        PRODUCT.NAME,
                        PRODUCT.PRICE,
                        PRODUCT.PRINT
                ).from(PRODUCT)
                .fetch(mapping(Product::new));
    }

    @Override
    public Product createProduct(CreateProductDto createProductDto) {
        return jooq.insertInto(PRODUCT)
                .columns(PRODUCT.NAME, PRODUCT.PRICE, PRODUCT.PRINT)
                .values(createProductDto.name(), createProductDto.price(), createProductDto.print())
                .returningResult(PRODUCT.ID, PRODUCT.NAME, PRODUCT.PRICE, PRODUCT.PRINT)
                .fetchOne(mapping(Product::new));
    }

    @Override
    public Product updateProduct(long productId, UpdateProductDto updateProductDto) throws ProductNotFoundException {
        var product = jooq.selectFrom(PRODUCT).where(PRODUCT.ID.eq(productId)).fetchOne();
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }
        if (updateProductDto.name().isPresent()) {
            product.setName(updateProductDto.name().get());
        }
        if (updateProductDto.price().isPresent()) {
            product.setPrice(updateProductDto.price().get());
        }
        if (updateProductDto.print().isPresent()) {
            product.setPrint(updateProductDto.print().get());
        }
        product.store();
        return new Product(product.getId(), product.getName(), product.getPrice(), product.getPrint());
    }

    @Override
    public void deleteProduct(long productId) {
        jooq.deleteFrom(PRODUCT).where(PRODUCT.ID.eq(productId)).execute();
    }
}
