package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.domain.exception.AuthenticationException;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.util.Objects;

@Repository
@Slf4j
public class IdamRepository {

    private final IdamClient idamClient;

    private final String idamUsername;

    private final String idamPassword;

    @Autowired
    public IdamRepository(@Value("${migration.idam.username}") String idamUsername,
                          @Value("${migration.idam.password}") String idamPassword,
                          IdamClient idamClient) {
        this.idamUsername = idamUsername;
        this.idamPassword = idamPassword;
        this.idamClient = idamClient;
    }

    public String generateUserToken() {
        if (Objects.isNull(idamUsername) || idamUsername.isBlank()) {
            throw new AuthenticationException("idam.username property can't be empty");
        }
        if (Objects.isNull(idamPassword) || idamPassword.isBlank()) {
            throw new AuthenticationException("idam.password property can't be empty");
        }
        log.info("Authenticating user name {}", this.idamUsername);
        return idamClient.authenticateUser(idamUsername, idamPassword);
    }
}
