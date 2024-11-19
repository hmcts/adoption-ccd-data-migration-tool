package uk.gov.hmcts.reform.query;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.migration.query.MatchQuery;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchQueryTest {

    @Test
    void shouldRequireNonNullFieldAndValue() {
        assertThatThrownBy(() -> MatchQuery.of(null, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldWrapValueInField() {
        final MatchQuery query = MatchQuery.of("field", "value");

        final Map<String, Object> queryMap = query.toMap();
        final Map<String, Object> expectedMap = Map.of("match", Map.of("field", Map.of("query", "value")));

        assertThat(queryMap).isEqualTo(expectedMap);
    }
}
