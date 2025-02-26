package de.flur4.roomiefunds.infrastructure.webclient.enablebanking;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ApplicationScoped
public class EnableBankingKeyProvider {

    @ConfigProperty(name = "app.enablebanking.applicationid")
    String applicationId;

    @ConfigProperty(name = "app.enablebanking.privatekey.path")
    String privateKeyPath;

    @ConfigProperty(name = "app.enablebanking.publickey.path")
    String publicKeyPath;

    @CacheResult(cacheName = "enablebanking-keyprovider")
    public RSAKeyProvider getRsaKeyProvider() throws IOException {
        RSAPrivateKey privateKey = (RSAPrivateKey) PemUtils.readPrivateKeyFromFile(privateKeyPath, "RSA");
        RSAPublicKey publicKey = (RSAPublicKey) PemUtils.readPublicKeyFromFile(publicKeyPath, "RSA");

        return new RSAKeyProvider() {
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
                return applicationId;
            }
        };
    }

}
