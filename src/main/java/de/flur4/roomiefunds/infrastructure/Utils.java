package de.flur4.roomiefunds.infrastructure;

import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jooq.tools.StringUtils;

import java.util.Optional;

public class Utils {
    public static ModifyingPersonDto createModifyingPersonDtoFromSecurityIdentity(JsonWebToken jwt) {
        Optional<String> name = Optional.empty();
        if (!StringUtils.isBlank(jwt.getName())) {
            name = Optional.of(jwt.getName());
        }
        return new ModifyingPersonDto(jwt.getSubject(), name);
    }
}
