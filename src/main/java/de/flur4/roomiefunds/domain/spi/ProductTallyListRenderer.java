package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.infrastructure.renderer.tallylistrenderer.EmptyTallyListException;

public interface ProductTallyListRenderer {
    byte[] renderTallyList() throws EmptyTallyListException;
}
