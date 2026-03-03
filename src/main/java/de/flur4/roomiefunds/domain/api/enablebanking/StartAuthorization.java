package de.flur4.roomiefunds.domain.api.enablebanking;

import de.flur4.roomiefunds.models.webclient.enablebanking.AuthorizeSessionResponse;

public interface StartAuthorization {
    void completeAuthorization(AuthorizeSessionResponse response);
}
