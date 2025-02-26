package de.flur4.roomiefunds.infrastructure.webclient.enablebanking;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Instant;

@Provider
@RequiredArgsConstructor
public class EnableBankingAuthenticationInjector implements ClientRequestFilter {

    private final EnableBankingKeyProvider enableBankingKeyProvider;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        /*
            Creating keys:

            // Create RSA-key in PKCS1 format (header "-----BEGIN RSA PRIVATE KEY-----")
            openssl genrsa -out private_key_in_pkcs1.pem 512

            // Convert to PKCS8 format (header "-----BEGIN PRIVATE KEY-----")
            openssl pkcs8 -topk8 -in private_key_in_pkcs1.pem -outform pem -nocrypt -out private_key_in_pkcs8.pem

            // Extract public key:
            openssl rsa -in private_key_in_pkcs8.pem -pubout > public.pub
         */

        var keyProvider = enableBankingKeyProvider.getRsaKeyProvider();
        Instant issueInstant = Instant.now();
        Instant expirationInstant = issueInstant.plusSeconds(3600);

        Algorithm algorithm = Algorithm.RSA256(keyProvider);
        String jwt = JWT.create()
                .withIssuer("enablebanking.com")
                .withAudience("api.enablebanking.com")
                .withIssuedAt(issueInstant)
                .withExpiresAt(expirationInstant)
                .sign(algorithm);

        requestContext.getHeaders().add("Authorization", "Bearer %s".formatted(jwt));
    }
}
