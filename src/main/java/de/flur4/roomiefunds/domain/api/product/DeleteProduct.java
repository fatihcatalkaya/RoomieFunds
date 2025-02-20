package de.flur4.roomiefunds.domain.api.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface DeleteProduct {
    void deleteProduct(ModifyingPersonDto modifiyingPerson, long productId) throws ProductNotFoundException, JsonProcessingException;
}
