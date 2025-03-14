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
    CaseDetails caseDetailsInAwaitingPaymentStateNoApplicationPayment;
    CaseDetails caseDetailsInSubmittedStateNoDateSubmitted;
    CaseDetails caseDetailsWithTtl;

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
            .id(1L)
            .state("Draft")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of("court", court))
            .build();

        caseDetailsInAwaitingPaymentState = CaseDetails.builder()
            .id(1L)
            .state("AwaitingPayment")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of(
                "court", court,
                "dateSubmitted", LocalDate.of(2024, 1, 15)
                    .toString(),
                "applicationPayments", List.of(
                    Element.newElement(Map.of(
                        "created", "2024-01-17T00:00:00.000Z",
                        "amount", 350
                    )),
                    Element.newElement(Map.of(
                        "created", "2024-01-16T00:00:00.000Z",
                        "amount", 250
                    )),
                    Element.newElement(Map.of(
                        "created", "2024-01-18T00:00:00.000Z",
                        "amount", 450
                    ))
                )
            ))
            .build();

        caseDetailsInSubmittedState = CaseDetails.builder()
            .id(1L)
            .state("Submitted")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of(
                "court", court,
                "dateSubmitted", LocalDate.of(2024, 1, 15)
                    .toString()
            ))
            .build();

        caseDetailsInLaSubmittedState = CaseDetails.builder()
            .id(1L)
            .state("LaSubmitted")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .lastModified(LocalDateTime.of(2024, 2, 1, 0, 0))
            .data(Map.of("court", court))
            .build();

        caseDetailsInAwaitingPaymentStateNoApplicationPayment = CaseDetails.builder()
            .id(1L)
            .state("AwaitingPayment")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of(
                "court", court,
                "dateSubmitted", LocalDate.of(2024, 1, 15)
                    .toString()
            ))
            .build();

        caseDetailsInSubmittedStateNoDateSubmitted = CaseDetails.builder()
            .id(1L)
            .state("Submitted")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of("court", court))
            .build();

        caseDetailsWithTtl = CaseDetails.builder()
            .id(1L)
            .state("Draft")
            .createdDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .data(Map.of(
                "court", court,
                "TTL", Map.of(
                    "Suspended", "No",
                    "SystemTTL", "2025-12-31"
                )
            ))
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
        expectedTtl.put("Suspended", "No");
        expectedTtl.put("SystemTTL", expectedSystemTtl.toString());

        assertThat(dataMigrationService.triggerTtlMigration(caseDetailsInDraftState).get("TTL"))
            .isEqualTo(expectedTtl);
    }

    @Test
    void shouldPopulateTtlOnCaseInAwaitingPaymentState() {
        LocalDate expectedSystemTtl = LocalDate.of(2024, 1, 16).plusDays(36524);
        Map<String, Object> expectedTtl = new HashMap<>();
        expectedTtl.put("OverrideTTL", null);
        expectedTtl.put("Suspended", "No");
        expectedTtl.put("SystemTTL", expectedSystemTtl.toString());

        assertThat(dataMigrationService.triggerTtlMigration(caseDetailsInAwaitingPaymentState).get("TTL"))
            .isEqualTo(expectedTtl);
    }

    @Test
    void shouldPopulateTtlOnCaseSubmittedState() {
        LocalDate expectedSystemTtl = LocalDate.of(2024, 1, 15).plusDays(36524);
        Map<String, Object> expectedTtl = new HashMap<>();
        expectedTtl.put("OverrideTTL", null);
        expectedTtl.put("Suspended", "No");
        expectedTtl.put("SystemTTL", expectedSystemTtl.toString());

        assertThat(dataMigrationService.triggerTtlMigration(caseDetailsInSubmittedState).get("TTL"))
            .isEqualTo(expectedTtl);
    }

    @Test
    void shouldPopulateTtlOnCaseInLaSubmittedState() {
        LocalDate expectedSystemTtl = LocalDate.of(2024, 2, 1).plusDays(36524);
        Map<String, Object> expectedTtl = new HashMap<>();
        expectedTtl.put("OverrideTTL", null);
        expectedTtl.put("Suspended", "No");
        expectedTtl.put("SystemTTL", expectedSystemTtl.toString());

        assertThat(dataMigrationService.triggerTtlMigration(caseDetailsInLaSubmittedState).get("TTL"))
            .isEqualTo(expectedTtl);
    }

    @Test
    void shouldRemoveTtlOnCaseWhenRemoveMigrationIsRun() {
        assertThat(dataMigrationService.triggerRemoveMigrationTtl(caseDetailsWithTtl).get("TTL"))
            .isEqualTo(new HashMap<>());
    }

    @Test
    void shouldThrowExceptionWhenCaseInAwaitingPaymentStateHasNoApplicationPayments() {
        assertThatThrownBy(() -> dataMigrationService
                .triggerTtlMigration(caseDetailsInAwaitingPaymentStateNoApplicationPayment))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Migration 2555, case with id: 1 has no applicationPayments in case data as expected");
    }

    @Test
    void shouldThrowExceptionWhenCaseInSubmittedStateHasNoApplication() {
        assertThatThrownBy(() -> dataMigrationService
            .triggerTtlMigration(caseDetailsInSubmittedStateNoDateSubmitted))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Migration 2555, case with id: 1 has no dateSubmitted in case data as expected");
    }
}
