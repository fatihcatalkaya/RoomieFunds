package de.flur4.roomiefunds.domain.api.product;

public interface ReorderProduct {
    void moveProductUp(long productId) throws ProductNotFoundException;
    void moveProductDown(long productId) throws ProductNotFoundException;
}
