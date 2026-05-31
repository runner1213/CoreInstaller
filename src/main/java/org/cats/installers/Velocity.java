package org.cats.installers;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.Installer;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.util.Colors.*;
import static org.cats.util.Eula.*;

public class Velocity implements Installer {
    private static final Logger logger = LogManager.getLogger(Velocity.class);

    private static final String RELEASES_URL = "https://fill.papermc.io/v3/projects/velocity";
    private static final String USER_AGENT = "CoreInstaller/3.2 (https://github.com/runner1213/CoreInstaller)";
    private static final String JAR_FILE = "server.jar";

    @Override
    public void init() {
        try {
            logger.info("{}Получение информации о версиях...{}", CYAN, RESET);
            JSONObject projectData = getVelocityJSON(RELEASES_URL);
            if (projectData == null) {
                logger.error("{}Ошибка при получении данных о версиях.{}", RED, RESET);
                return;
            }

            String selectedVersion = selectVersion(projectData);
            if (selectedVersion == null) return;

            logger.info("{}Получение информации о сборках...{}", CYAN, RESET);
            String buildsUrl = RELEASES_URL + "/versions/" + selectedVersion + "/builds";
            JSONArray buildsData = getVelocityJSONArray(buildsUrl);
            if (buildsData == null) {
                logger.error("{}Ошибка при получении данных о сборках.{}", RED, RESET);
                return;
            }

            BuildInfo latestBuild = getLatestBuild(buildsData);
            if (latestBuild == null) {
                logger.error("{}Не удалось определить сборку для скачивания.{}", RED, RESET);
                return;
            }

            logger.info("{}Скачивание Velocity {} (build #{})...{}", CYAN, selectedVersion, latestBuild.number, RESET);
            downloadWithProgress(latestBuild.downloadUrl, JAR_FILE);

            createEulaFile();

            logger.info("{}Velocity успешно установлен!{}", GREEN, RESET);

        } catch (Exception e) {
            logger.error("{}Ошибка при установке Velocity{}", RED, RESET, e);
        }
    }

    private String selectVersion(JSONObject projectData) {
        List<String> versionList = getAvailableVersions(projectData);
        int limit = Math.min(10, versionList.size());
        List<String> lastTen = versionList.subList(0, limit);

        Scanner scanner = new Scanner(System.in);
        logger.info("\n{}Доступные версии Velocity:{}", YELLOW, RESET);
        for (int i = 0; i < lastTen.size(); i++) {
            logger.info("{}{}. {}{}", CYAN, i + 1, lastTen.get(i), RESET);
        }

        logger.info("\nВыберите версию Velocity (1-{}):", lastTen.size());
        System.out.print(">> ");
        int choice = scanner.nextInt();

        if (choice < 1 || choice > lastTen.size()) {
            logger.warn("{}Некорректный выбор.{}", RED, RESET);
            return null;
        }

        return lastTen.get(choice - 1);
    }

    static List<String> getAvailableVersions(JSONObject projectData) {
        JSONObject versions = projectData.getJSONObject("versions");
        List<String> versionList = new ArrayList<>();

        for (String versionGroup : versions.keySet()) {
            JSONArray groupVersions = versions.getJSONArray(versionGroup);
            for (int i = 0; i < groupVersions.length(); i++) {
                versionList.add(groupVersions.getString(i));
            }
        }

        versionList = new ArrayList<>(new LinkedHashSet<>(versionList));
        versionList.sort(Velocity::compareVersionsDescending);
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

        int stabilityCompare = Boolean.compare(isSnapshot(first), isSnapshot(second));
        if (stabilityCompare != 0) {
            return stabilityCompare;
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

    private static boolean isSnapshot(String version) {
        return version.toUpperCase(Locale.ROOT).contains("SNAPSHOT");
    }

    private JSONObject getVelocityJSON(String url) {
        String response = getVelocityResponse(url);
        return response != null ? new JSONObject(response) : null;
    }

    private JSONArray getVelocityJSONArray(String url) {
        String response = getVelocityResponse(url);
        return response != null ? new JSONArray(response) : null;
    }

    private String getVelocityResponse(String url) {
        try {
            HttpURLConnection connection = openVelocityConnection(url);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (Exception e) {
            logger.error("{}Ошибка HTTP запроса: {}{}", RED, e.getMessage(), RESET);
            return null;
        }
    }

    private HttpURLConnection openVelocityConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    @Override
    public void downloadWithProgress(String fileURL, String saveFile) {
        try {
            HttpURLConnection connection = openVelocityConnection(fileURL);
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
            logger.error("{}Ошибка загрузки: {}{}", RED, e.getMessage(), RESET);
            throw new IllegalStateException("Не удалось скачать Velocity", e);
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
