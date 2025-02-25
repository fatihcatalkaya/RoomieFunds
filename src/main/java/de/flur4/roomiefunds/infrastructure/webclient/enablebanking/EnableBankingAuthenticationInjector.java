package de.flur4.roomiefunds.infrastructure.webclient.enablebanking;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

@Provider
public class EnableBankingAuthenticationInjector implements ClientRequestFilter {

    @ConfigProperty(name = "app.enablebanking.applicationid")
    String applicationId;

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
        String privateKeyFile = "/home/fatih/Downloads/enablebanking/394e693a-5257-4ff9-a194-105916f9d3af.pem";
        String publicKeyFile = "/home/fatih/Downloads/enablebanking/signing_public.pub";

        //Calculate JWT and inject here
        RSAPrivateKey privateKey = (RSAPrivateKey) PemUtils.readPrivateKeyFromFile(privateKeyFile, "RSA");
        RSAPublicKey publicKey = (RSAPublicKey) PemUtils.readPublicKeyFromFile(publicKeyFile, "RSA");
        RSAKeyProvider keyProvider = new RSAKeyProvider() {
            @Override
            public RSAPublicKey getPublicKeyById(String s) {
                return publicKey;
            }

            @Override
            public RSAPrivateKey getPrivateKey() {
                return privateKey;
            }

            @Override
            public String getPrivateKeyId() {
                return applicationId; // Hier muss die Anwendungs ID hin
            }
        };
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
