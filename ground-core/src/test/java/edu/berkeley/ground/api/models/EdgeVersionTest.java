/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.berkeley.ground.api.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.ground.api.versions.GroundType;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;

public class EdgeVersionTest {
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

    @Test
    public void serializesToJSON() throws Exception {
        Map<String, Tag> tagsMap = new HashMap<>();
        tagsMap.put("testtag", new Tag("abcd", "testtag", "tag", GroundType.STRING));

        Map<String, String> parametersMap = new HashMap<>();
        parametersMap.put("http", "GET");

        EdgeVersion edgeVersion = new EdgeVersion("abcd", tagsMap, null, "http://www.google.com", parametersMap, "Edges.test", "123", "456");

        final String expected = MAPPER.writeValueAsString(MAPPER.readValue(fixture("fixtures/models/edge_version.json"), EdgeVersion.class));
        assertThat(MAPPER.writeValueAsString(edgeVersion)).isEqualTo(expected);
    }

    @Test
    public void deserializesFromJSON() throws Exception {
        Map<String, Tag> tagsMap = new HashMap<>();
        tagsMap.put("testtag", new Tag("abcd", "testtag", "tag", GroundType.STRING));

        Map<String, String> parametersMap = new HashMap<>();
        parametersMap.put("http", "GET");

        EdgeVersion edgeVersion = new EdgeVersion("abcd", tagsMap, null, "http://www.google.com", parametersMap, "Edges.test", "123", "456");

        assertThat(MAPPER.readValue(fixture("fixtures/models/edge_version.json"), EdgeVersion.class)).isEqualToComparingFieldByField(edgeVersion);
    }
}
