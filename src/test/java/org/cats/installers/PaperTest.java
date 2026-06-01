package org.cats.installers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaperTest {
    @Test
    void resolveMinecraftVersionReturnsLatestVersionForRequestedBranch() {
        JSONObject projectData = new JSONObject("""
                {
                  "versions": {
                    "1.21": ["1.21.11", "1.21.10", "1.21.9"],
                    "1.20": ["1.20.6", "1.20.5"]
                  }
                }
                """);

        assertEquals("1.21.11", Paper.resolveMinecraftVersion(projectData, "1.21"));
    }

    @Test
    void resolveMinecraftVersionKeepsExactVersionWhenItExists() {
        JSONObject projectData = new JSONObject("""
                {
                  "versions": {
                    "1.21": ["1.21.11", "1.21.10", "1.21.9"],
                    "1.20": ["1.20.6", "1.20.5"]
                  }
                }
                """);

        assertEquals("1.21.10", Paper.resolveMinecraftVersion(projectData, "1.21.10"));
    }

    @Test
    void resolveMinecraftVersionReturnsNullForUnknownVersion() {
        JSONObject projectData = new JSONObject("""
                {
                  "versions": {
                    "1.21": ["1.21.11", "1.21.10"]
                  }
                }
                """);

        assertNull(Paper.resolveMinecraftVersion(projectData, "1.19.4"));
    }

    @Test
    void getAvailableMinecraftVersionsFlattensGroupsDeduplicatesAndSortsNewestFirst() {
        JSONObject projectData = new JSONObject("""
                {
                  "versions": {
                    "1.21": ["1.21.10", "1.21.11"],
                    "1.20": ["1.20.6", "1.21.10"]
                  }
                }
                """);

        List<String> versions = Paper.getAvailableMinecraftVersions(projectData);

        assertEquals(List.of("1.21.11", "1.21.10", "1.20.6"), versions);
    }

    @Test
    void getLatestBuildReturnsHighestDownloadableBuild() {
        JSONArray builds = new JSONArray("""
                [
                  {
                    "id": 69,
                    "downloads": {
                      "server:default": {
                        "name": "paper-1.21.11-69.jar",
                        "url": "https://example.test/paper-69.jar"
                      }
                    }
                  },
                  {
                    "id": 133,
                    "downloads": {}
                  },
                  {
                    "id": 132,
                    "downloads": {
                      "server:default": {
                        "name": "paper-1.21.11-132.jar",
                        "url": "https://example.test/paper-132.jar"
                      }
                    }
                  }
                ]
                """);

        Paper.BuildInfo latest = Paper.getLatestBuild(builds);

        assertEquals(132, latest.number);
        assertEquals("https://example.test/paper-132.jar", latest.downloadUrl);
    }
}
