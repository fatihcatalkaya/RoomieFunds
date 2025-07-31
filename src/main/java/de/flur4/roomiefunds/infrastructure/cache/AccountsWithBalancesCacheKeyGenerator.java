package de.flur4.roomiefunds.infrastructure.cache;

import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CompositeCacheKey;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.reflect.Method;

@ApplicationScoped
public class AccountsWithBalancesCacheKeyGenerator implements CacheKeyGenerator {
    @Override
    public Object generate(Method method, Object... methodParams) {
        return new CompositeCacheKey("default-accounts-with-balances");
    }
}
