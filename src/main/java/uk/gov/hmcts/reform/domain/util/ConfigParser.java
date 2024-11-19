package uk.gov.hmcts.reform.domain.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
public class ConfigParser {

    private ConfigParser() {

    }

    public static Map<String, List<String>> parseConfig(String config) {
        HashMap<String, List<String>> map = new HashMap<>();
        Arrays.stream(config.split(";"))
            .map(ConfigParser::parseEntry)
            .filter(e -> !isEmpty(e))
            .forEach(p -> map.put(p.getKey(), p.getValue()));
        return map;
    }

    public static Pair<String, List<String>> parseEntry(String entry) {
        String[] split = entry.split("=>");
        if (split.length == 2) {
            List<String> ids = Arrays.asList(split[1].split("\\|"));
            return Pair.of(split[0], ids);
        } else {
            throw new IllegalArgumentException("Could not parse entry '" + entry + "'");
        }
    }

}
