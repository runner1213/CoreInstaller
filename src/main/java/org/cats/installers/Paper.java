package org.cats.installers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.Installer;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.util.Colors.*;
import static org.cats.util.Eula.createEulaFile;

public class Paper implements Installer {
    private static final Logger logger = LogManager.getLogger(Paper.class);

    private static final String PROJECT_URL = "https://fill.papermc.io/v3/projects/paper";
    private static final String USER_AGENT = "CoreInstaller/3.2 (https://github.com/runner1213/CoreInstaller)";
    private static final String JAR_FILE = "server.jar";

    @Override
    public void init() {
        try {
            logger.info("{}Получение списка версий Paper...{}", CYAN, RESET);
            JSONObject projectData = getPaperJSON(PROJECT_URL);
            if (projectData == null) {
                logger.error("{}Ошибка при получении списка версий Paper.{}", RED, RESET);
                return;
            }

            Scanner scanner = new Scanner(System.in);
            logger.info("Введите версию Paper или ветку Minecraft (1.21, 1.21.11, 1.16.5):");
            logger.info(">> ");
            String requestedVersion = scanner.next().trim();

            String minecraftVersion = resolveMinecraftVersion(projectData, requestedVersion);
            if (minecraftVersion == null) {
                logger.error("{}Версия Paper {} не найдена.{}", RED, requestedVersion, RESET);
                return;
            }

            if (!requestedVersion.equals(minecraftVersion)) {
                logger.info("{}Для ветки {} выбрана последняя версия {}{}", CYAN, requestedVersion, minecraftVersion, RESET);
            }

            logger.info("{}Получение информации о сборках Paper {}...{}", CYAN, minecraftVersion, RESET);
            JSONArray buildsData = getPaperJSONArray(PROJECT_URL + "/versions/" + minecraftVersion + "/builds");
            if (buildsData == null) {
                logger.error("{}Ошибка при получении сборок Paper {}.{}", RED, minecraftVersion, RESET);
                return;
            }

            BuildInfo latestBuild = getLatestBuild(buildsData);
            if (latestBuild == null) {
                logger.error("{}Не удалось найти сборку Paper {} для скачивания.{}", RED, minecraftVersion, RESET);
                return;
            }

            logger.info("{}Скачивание Paper {} (build #{})...{}", CYAN, minecraftVersion, latestBuild.number, RESET);
            downloadWithProgress(latestBuild.downloadUrl, JAR_FILE);

            createEulaFile();

            logger.info("{}Paper успешно установлен для Minecraft {}{}", GREEN, minecraftVersion, RESET);
        } catch (Exception e) {
            logger.error("{}Ошибка при установке Paper{}", RED, RESET, e);
        }
    }

    static String resolveMinecraftVersion(JSONObject projectData, String requestedVersion) {
        String normalizedVersion = requestedVersion.trim();
        JSONObject versions = projectData.getJSONObject("versions");

        if (versions.has(normalizedVersion)) {
            JSONArray branchVersions = versions.getJSONArray(normalizedVersion);
            return branchVersions.length() == 0 ? null : branchVersions.getString(0);
        }

        List<String> availableVersions = getAvailableMinecraftVersions(projectData);
        return availableVersions.contains(normalizedVersion) ? normalizedVersion : null;
    }

    static List<String> getAvailableMinecraftVersions(JSONObject projectData) {
        JSONObject versions = projectData.getJSONObject("versions");
        List<String> versionList = new ArrayList<>();

        for (String versionGroup : versions.keySet()) {
            JSONArray groupVersions = versions.getJSONArray(versionGroup);
            for (int i = 0; i < groupVersions.length(); i++) {
                versionList.add(groupVersions.getString(i));
            }
        }

        versionList = new ArrayList<>(new LinkedHashSet<>(versionList));
        versionList.sort(Paper::compareVersionsDescending);
        return versionList;
    }

    static BuildInfo getLatestBuild(JSONArray buildsData) {
        BuildInfo latest = null;
        for (int i = 0; i < buildsData.length(); i++) {
            JSONObject buildObj = buildsData.getJSONObject(i);
            int buildNumber = getBuildNumber(buildObj);
            String downloadUrl = getDownloadUrl(buildObj);

            if (buildNumber > 0 && downloadUrl != null && (latest == null || buildNumber > latest.number)) {
                latest = new BuildInfo(buildNumber, downloadUrl);
            }
        }
        return latest;
    }

    private static int getBuildNumber(JSONObject buildObj) {
        if (buildObj.has("id")) {
            return buildObj.getInt("id");
        }
        if (buildObj.has("build")) {
            return buildObj.getInt("build");
        }
        if (buildObj.has("number")) {
            return buildObj.getInt("number");
        }
        return -1;
    }

    private static String getDownloadUrl(JSONObject buildObj) {
        JSONObject downloads = buildObj.optJSONObject("downloads");
        if (downloads == null) {
            return null;
        }

        JSONObject serverDefault = downloads.optJSONObject("server:default");
        if (serverDefault != null) {
            return serverDefault.optString("url", null);
        }

        JSONObject application = downloads.optJSONObject("application");
        return application != null ? application.optString("url", null) : null;
    }

    private static int compareVersionsDescending(String first, String second) {
        int numberCompare = compareVersionNumbers(first, second);
        if (numberCompare != 0) {
            return -numberCompare;
        }
        return second.compareTo(first);
    }

    private static int compareVersionNumbers(String first, String second) {
        List<Integer> firstParts = extractVersionNumbers(first);
        List<Integer> secondParts = extractVersionNumbers(second);
        int max = Math.max(firstParts.size(), secondParts.size());

        for (int i = 0; i < max; i++) {
            int firstPart = i < firstParts.size() ? firstParts.get(i) : 0;
            int secondPart = i < secondParts.size() ? secondParts.get(i) : 0;
            int compare = Integer.compare(firstPart, secondPart);
            if (compare != 0) {
                return compare;
            }
        }

        return 0;
    }

    private static List<Integer> extractVersionNumbers(String version) {
        List<Integer> numbers = new ArrayList<>();
        for (String part : version.split("[^0-9]+")) {
            if (!part.isEmpty()) {
                numbers.add(Integer.parseInt(part));
            }
        }
        return numbers;
    }

    private JSONObject getPaperJSON(String url) {
        String response = getPaperResponse(url);
        return response != null ? new JSONObject(response) : null;
    }

    private JSONArray getPaperJSONArray(String url) {
        String response = getPaperResponse(url);
        return response != null ? new JSONArray(response) : null;
    }

    private String getPaperResponse(String url) {
        try {
            HttpURLConnection connection = openPaperConnection(url);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (Exception e) {
            logger.error("{}Ошибка HTTP запроса Paper: {}{}", RED, e.getMessage(), RESET);
            return null;
        }
    }

    private HttpURLConnection openPaperConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    @Override
    public void downloadWithProgress(String fileURL, String saveFile) {
        try {
            HttpURLConnection connection = openPaperConnection(fileURL);
            int fileSize = connection.getContentLength();

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(saveFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                int downloaded = 0;

                logger.info("Скачивание {}", saveFile + "\n");
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    printProgress(downloaded, fileSize);
                }
            }

            logger.info("\n{}Скачивание завершено!{}", GREEN, RESET);
            logger.info("{}Файл успешно сохранён как server.jar!{}", GREEN, RESET);
        } catch (Exception e) {
            logger.error("{}Ошибка загрузки Paper: {}{}", RED, e.getMessage(), RESET);
            throw new IllegalStateException("Не удалось скачать Paper", e);
        }
    }

    static final class BuildInfo {
        final int number;
        final String downloadUrl;

        private BuildInfo(int number, String downloadUrl) {
            this.number = number;
            this.downloadUrl = downloadUrl;
        }
    }
}
