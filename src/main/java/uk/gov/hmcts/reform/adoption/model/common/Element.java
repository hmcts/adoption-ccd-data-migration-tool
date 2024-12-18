package uk.gov.hmcts.reform.adoption.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Element<T> {
    private UUID id;

    @NotNull
    @Valid
    private T value;

    public static <T> Element<T> newElement(T value) {
        return Element.<T>builder().value(value).build();
    }
}
