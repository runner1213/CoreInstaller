package org.cats.installers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VelocityTest {
    @Test
    void getAvailableVersionsFlattensGroupsDeduplicatesAndSortsNewestFirst() {
        JSONObject projectData = new JSONObject("""
                {
                  "versions": {
                    "3.0.0": ["3.4.0-SNAPSHOT", "3.5.0-SNAPSHOT", "3.4.0"],
                    "2.0.0": ["2.7.2", "3.4.0"]
                  }
                }
                """);

        List<String> versions = Velocity.getAvailableVersions(projectData);

        assertEquals(
                List.of("3.5.0-SNAPSHOT", "3.4.0", "3.4.0-SNAPSHOT", "2.7.2"),
                versions
        );
    }

    @Test
    void getLatestBuildReturnsHighestBuildWithServerDefaultDownload() {
        JSONArray builds = new JSONArray("""
                [
                  {
                    "id": 599,
                    "downloads": {
                      "server:default": {
                        "name": "velocity-3.5.0-SNAPSHOT-599.jar",
                        "url": "https://example.test/velocity-599.jar"
                      }
                    }
                  },
                  {
                    "id": 601,
                    "downloads": {}
                  },
                  {
                    "id": 600,
                    "downloads": {
                      "server:default": {
                        "name": "velocity-3.5.0-SNAPSHOT-600.jar",
                        "url": "https://example.test/velocity-600.jar"
                      }
                    }
                  }
                ]
                """);

        Velocity.BuildInfo latest = Velocity.getLatestBuild(builds);

        assertEquals(600, latest.number);
        assertEquals("https://example.test/velocity-600.jar", latest.downloadUrl);
    }

    @Test
    void getLatestBuildSupportsLegacyBuildNumberAndApplicationDownloadFallback() {
        JSONArray builds = new JSONArray("""
                [
                  {
                    "build": 10,
                    "downloads": {
                      "application": {
                        "name": "velocity-legacy-10.jar",
                        "url": "https://example.test/velocity-legacy-10.jar"
                      }
                    }
                  },
                  {
                    "number": 9,
                    "downloads": {
                      "application": {
                        "name": "velocity-legacy-9.jar",
                        "url": "https://example.test/velocity-legacy-9.jar"
                      }
                    }
                  }
                ]
                """);

        Velocity.BuildInfo latest = Velocity.getLatestBuild(builds);

        assertEquals(10, latest.number);
        assertEquals("https://example.test/velocity-legacy-10.jar", latest.downloadUrl);
    }

    @Test
    void getLatestBuildReturnsNullWhenNoBuildHasDownloadUrl() {
        JSONArray builds = new JSONArray("""
                [
                  {"id": 600, "downloads": {}},
                  {"id": 599}
                ]
                """);

        assertNull(Velocity.getLatestBuild(builds));
    }
}
