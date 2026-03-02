package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.api.product.ProductNotFoundException;
import de.flur4.roomiefunds.domain.spi.ProductRepository;
import de.flur4.roomiefunds.models.product.CreateProductDto;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.product.UpdateProductDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

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
                        PRODUCT.PRINT,
                        PRODUCT.SORT_ORDER
                ).from(PRODUCT).where(PRODUCT.ID.eq(productId))
                .fetchOptional(mapping(Product::new));
    }

    @Override
    public List<Product> getAllProducts() {
        return jooq.select(
                        PRODUCT.ID,
                        PRODUCT.NAME,
                        PRODUCT.PRICE,
                        PRODUCT.PRINT,
                        PRODUCT.SORT_ORDER
                ).from(PRODUCT)
                .orderBy(PRODUCT.SORT_ORDER)
                .fetch(mapping(Product::new));
    }

    @Override
    public List<Product> getProductsToPrintOnTallyList() {
        return jooq.select(
                        PRODUCT.ID,
                        PRODUCT.NAME,
                        PRODUCT.PRICE,
                        PRODUCT.PRINT,
                        PRODUCT.SORT_ORDER
                ).from(PRODUCT)
                .where(PRODUCT.PRINT.eq(true))
                .orderBy(PRODUCT.SORT_ORDER)
                .fetch(mapping(Product::new));
    }

    @Override
    public Product createProduct(CreateProductDto createProductDto) {
        var maxSortOrder = jooq.select(DSL.coalesce(DSL.max(PRODUCT.SORT_ORDER), 0))
                .from(PRODUCT)
                .fetchOne(0, int.class);

        return jooq.insertInto(PRODUCT)
                .columns(PRODUCT.NAME, PRODUCT.PRICE, PRODUCT.PRINT, PRODUCT.SORT_ORDER)
                .values(createProductDto.name(), createProductDto.price(), createProductDto.print(), maxSortOrder + 1)
                .returningResult(PRODUCT.ID, PRODUCT.NAME, PRODUCT.PRICE, PRODUCT.PRINT, PRODUCT.SORT_ORDER)
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
        return new Product(product.getId(), product.getName(), product.getPrice(), product.getPrint(), product.getSortOrder());
    }

    @Override
    public void deleteProduct(long productId) {
        jooq.deleteFrom(PRODUCT).where(PRODUCT.ID.eq(productId)).execute();
    }

    @Override
    public void swapProductSortOrder(long productId1, long productId2) {
        jooq.transaction(ctx -> {
            var dsl = ctx.dsl();

            var sortOrder1 = dsl.select(PRODUCT.SORT_ORDER).from(PRODUCT)
                    .where(PRODUCT.ID.eq(productId1)).fetchOne(PRODUCT.SORT_ORDER);
            var sortOrder2 = dsl.select(PRODUCT.SORT_ORDER).from(PRODUCT)
                    .where(PRODUCT.ID.eq(productId2)).fetchOne(PRODUCT.SORT_ORDER);

            dsl.update(PRODUCT).set(PRODUCT.SORT_ORDER, sortOrder2)
                    .where(PRODUCT.ID.eq(productId1)).execute();
            dsl.update(PRODUCT).set(PRODUCT.SORT_ORDER, sortOrder1)
                    .where(PRODUCT.ID.eq(productId2)).execute();
        });
    }
}
