package uk.gov.hmcts.reform.migration.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthUtilTest {

    @Test
   void shouldGetBearToken() {
        assertThat(AuthUtil.getBearerToken("aaaa")).isEqualTo("Bearer aaaa");
    }

    @Test
    void shouldReturnGetBearToken() {
        assertThat(AuthUtil.getBearerToken("Bearer aaaa")).isEqualTo("Bearer aaaa");
    }

    @Test
    void shouldReturnBlankToken() {
        assertThat(AuthUtil.getBearerToken("")).isEqualTo("");
    }
}
