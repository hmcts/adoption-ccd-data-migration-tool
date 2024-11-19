package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.migration.service.DataMigrationService.MIGRATION_ID_KEY;


@ExtendWith(MockitoExtension.class)
class DataMigrationServiceImplTest {

    private static final String INVALID_MIGRATION_ID = "NOT_A_MIGRATION";

    private DataMigrationServiceImpl dataMigrationService;

    CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        dataMigrationService = new DataMigrationServiceImpl();

        Map<String, String> court = Map.of("code", "344",
            "name", "Family Court sitting at Swansea",
            "email", "FamilyPublicLaw+sa@gmail.com"
        );

        caseDetails = CaseDetails.builder()
            .data(Map.of("court", court))
            .build();
    }

    @Test
    void shouldReturnTrueWhenCourtPresent() {
        assertThat(dataMigrationService.accepts().test(caseDetails)).isTrue();
    }


    @Test
    void shouldThrowExceptionWhenMigrationKeyIsNotSet() {
        assertThatThrownBy(() -> dataMigrationService.migrate(Map.of(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Migration ID must not be null");
    }

    @Test
    void shouldThrowExceptionWhenMigrationKeyIsInvalid() {
        Map<String, Object> data = new HashMap<>();
        assertThatThrownBy(() -> dataMigrationService.migrate(data, INVALID_MIGRATION_ID))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("No migration mapped to " + INVALID_MIGRATION_ID);
        assertThat(data.get(MIGRATION_ID_KEY)).isNull();
    }

}
