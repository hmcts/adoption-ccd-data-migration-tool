package uk.gov.hmcts.reform.domain.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigParserTest {

    @Test
    void shouldThrowExceptionOnEmptyConfig() {
        String config = "";

        assertThatThrownBy(() -> ConfigParser.parseConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Could not parse entry ''");
    }

    @Test
    void shouldThrowExceptionOnInvalidConfig() {
        String config = "DFPL-1234";

        assertThatThrownBy(() -> ConfigParser.parseConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Could not parse entry 'DFPL-1234'");
    }

    @Test
    void shouldThrowExceptionOnMissingValue() {
        String config = "DFPL-1234=>";

        assertThatThrownBy(() -> ConfigParser.parseConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Could not parse entry 'DFPL-1234=>'");
    }

    @Test
    void shouldParseSingleEntry() {
        String config = "DFPL-test=>12345";

        Map<String, List<String>> mapping = ConfigParser.parseConfig(config);
        Map<String, List<String>> expected = Map.of("DFPL-test", List.of("12345"));

        assertThat(mapping).isEqualTo(expected);
    }

    @Test
    void shouldParseSingleEntryWithMultipleIds() {
        String config = "DFPL-test=>12|34|56|78|90";

        Map<String, List<String>> mapping = ConfigParser.parseConfig(config);
        Map<String, List<String>> expected = Map.of("DFPL-test", List.of("12", "34", "56", "78", "90"));

        assertThat(mapping).isEqualTo(expected);
    }

    @Test
    void shouldParseMultipleEntriesWithMultipleIds() {
        String config = "DFPL-test=>12|34|56|78|90;DFPL-othertest=>1|2|3";

        Map<String, List<String>> mapping = ConfigParser.parseConfig(config);
        Map<String, List<String>> expected = Map.of("DFPL-test", List.of("12", "34", "56", "78", "90"),
            "DFPL-othertest", List.of("1", "2", "3"));

        assertThat(mapping).isEqualTo(expected);
    }


}
