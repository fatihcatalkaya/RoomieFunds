package de.flur4.roomiefunds.domain.api.product;

import de.flur4.roomiefunds.infrastructure.tallylistrenderer.EmptyTallyListException;

public interface GetProductTallySheet {
    byte[] getProductTallySheet() throws EmptyTallyListException;
}
