package de.flur4.roomiefunds.domain.api.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.product.Product;
import de.flur4.roomiefunds.models.product.CreateProductDto;

public interface CreateProduct {
    Product createProduct(ModifyingPersonDto modifiyingPerson, CreateProductDto createProductDto) throws JsonProcessingException;
}
