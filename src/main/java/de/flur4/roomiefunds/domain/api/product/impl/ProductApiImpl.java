package de.flur4.roomiefunds.domain.api.product.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.product.*;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.ProductRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.product.CreateProductDto;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.product.UpdateProductDto;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProductApiImpl implements CreateProduct, GetProduct, UpdateProduct, DeleteProduct {

    final ProductRepository productRepository;
    final LogRepository logRepository;

    @Override
    public Product createProduct(ModifyingPersonDto modifiyingPerson, CreateProductDto createProductDto) throws JsonProcessingException {
        Product createdProduct = productRepository.createProduct(createProductDto);
        logRepository.insertLogEntry(modifiyingPerson, new InsertLogEntryDto(
                LogOperations.create,
                "product",
                Optional.empty(),
                Optional.of(createdProduct)
        ));
        return createdProduct;
    }

    @Override
    public void deleteProduct(ModifyingPersonDto modifiyingPerson, long productId) throws ProductNotFoundException, JsonProcessingException {
        Optional<Product> productToDelete = productRepository.getProductById(productId);
        if (productToDelete.isEmpty()) {
            throw new ProductNotFoundException(productId);
        }

        productRepository.deleteProduct(productId);
        logRepository.insertLogEntry(modifiyingPerson, new InsertLogEntryDto(
                LogOperations.delete,
                "product",
                Optional.of(productToDelete.get()),
                Optional.empty()
        ));
    }

    @Override
    public Optional<Product> getProduct(long productId) {
        return productRepository.getProductById(productId);
    }

    @Override
    public List<Product> getProducts() {
        return productRepository.getAllProducts();
    }

    @Override
    public Product updateProduct(ModifyingPersonDto modifiyingPerson, long productId, UpdateProductDto updateProductDto) throws ProductNotFoundException, JsonProcessingException {
        Optional<Product> productToUpdate = productRepository.getProductById(productId);
        if (productToUpdate.isEmpty()) {
            throw new ProductNotFoundException(productId);
        }
        Product updatedProduct = productRepository.updateProduct(productId, updateProductDto);
        logRepository.insertLogEntry(modifiyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "product",
                Optional.of(productToUpdate.get()),
                Optional.of(updatedProduct)
        ));
        return updatedProduct;
    }
}
