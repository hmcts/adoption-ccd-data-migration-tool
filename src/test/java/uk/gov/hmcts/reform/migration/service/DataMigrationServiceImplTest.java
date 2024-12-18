package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.adoption.model.common.Element;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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
    CaseDetails caseDetailsInDraftState;
    CaseDetails caseDetailsInAwaitingPaymentState;
    CaseDetails caseDetailsInSubmittedState;
    CaseDetails caseDetailsInLaSubmittedState;

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

        caseDetailsInDraftState = CaseDetails.builder()
            .state("Draft")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of("court", court))
            .build();

        caseDetailsInAwaitingPaymentState = CaseDetails.builder()
            .state("AwaitingPayment")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of(
                "court", court,
                "application", Map.of(
                    "dateSubmitted", LocalDateTime.of(2024, 1, 15, 0, 0),
                    "applicationPayments",
                            List.of(Element.newElement(Map.of(
                                "created", LocalDateTime.of(2024, 1, 16, 0, 0),
                                "amount", 250
                            )))
                )
            ))
            .build();

        caseDetailsInSubmittedState = CaseDetails.builder()
            .state("Submitted")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of(
                "court", court,
                "application", Map.of(
                    "dateSubmitted", LocalDateTime.of(2024, 1, 15, 0, 0)
                )
            ))
            .build();

        caseDetailsInLaSubmittedState = CaseDetails.builder()
            .state("LaSubmitted")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .lastModified(LocalDateTime.of(2024, 2, 1, 0, 0))
            .data(Map.of("court", court))
            .build();
    }

    @Test
    void shouldReturnTrueWhenCourtPresent() {
        assertThat(dataMigrationService.accepts().test(caseDetails)).isTrue();
    }


    @Test
    void shouldThrowExceptionWhenMigrationKeyIsNotSet() {
        assertThatThrownBy(() -> dataMigrationService.migrate(caseDetails, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Migration ID must not be null");
    }

    @Test
    void shouldThrowExceptionWhenMigrationKeyIsInvalid() {
        Map<String, Object> data = new HashMap<>();
        assertThatThrownBy(() -> dataMigrationService.migrate(caseDetails, INVALID_MIGRATION_ID))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("No migration mapped to " + INVALID_MIGRATION_ID);
        assertThat(data.get(MIGRATION_ID_KEY)).isNull();
    }

    @Test
    void shouldPopulateTtlOnCaseInDraftState() {
        LocalDate expectedSystemTtl = LocalDate.of(2024, 1, 1).plusDays(90);
        Map<String, Object> expectedTtl = new HashMap<>();
        expectedTtl.put("OverrideTTL", null);
        expectedTtl.put("Suspend", "NO");
        expectedTtl.put("SystemTTL", expectedSystemTtl);

        assertThat(dataMigrationService.triggerTtlMigration(caseDetailsInDraftState).get("TTL")).isEqualTo(expectedTtl);
    }
}
