package uk.gov.hmcts.reform.domain.exception;

public class CaseMigrationSkippedException extends RuntimeException {

    public CaseMigrationSkippedException(String message) {
        super(message);
    }

}
