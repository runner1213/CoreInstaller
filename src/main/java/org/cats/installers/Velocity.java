package org.cats.installers;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.installers.Vanilla.downloadWithProgress;
import static org.cats.util.Colors.*;
import static org.cats.util.Eula.*;

public class Velocity {
    private static final Logger logger = LogManager.getLogger(Velocity.class);

    private static final String RELEASES_URL = "https://api.papermc.io/v2/projects/velocity";
    private static final String DOWNLOAD_URL_TEMPLATE = "https://api.papermc.io/v2/projects/velocity/versions/%s/builds/%s/downloads/%s";
    private static final String JAR_FILE = "server.jar";

    public static void installVelocity() {
        try {
            logger.info(CYAN + "Получение информации о версиях..." + RESET);
            JSONObject projectData = getJSON(RELEASES_URL);
            if (projectData == null) {
                System.out.println(RED + "Ошибка при получении данных о версиях." + RESET);
                return;
            }

            String selectedVersion = selectVersion(projectData);
            if (selectedVersion == null) return;

            logger.info(CYAN + "Получение информации о сборках..." + RESET);
            String buildsUrl = RELEASES_URL + "/versions/" + selectedVersion + "/builds";
            JSONObject buildsData = getJSON(buildsUrl);
            if (buildsData == null) {
                logger.error(RED + "Ошибка при получении данных о сборках." + RESET);
                return;
            }

            int latestBuild = getLatestBuild(buildsData);
            String fileName = getFileName(selectedVersion, latestBuild);
            if (fileName == null) {
                logger.error(RED + "Не удалось определить имя файла для скачивания." + RESET);
                return;
            }

            String downloadUrl = String.format(DOWNLOAD_URL_TEMPLATE, selectedVersion, latestBuild, fileName);

            logger.info(CYAN + "Скачивание Velocity {} (build #{})..." + RESET, selectedVersion, latestBuild);
            downloadWithProgress(downloadUrl, JAR_FILE);

            createEulaFile();

            logger.info(GREEN + "\nVelocity успешно установлен!" + RESET);

        } catch (Exception e) {
            logger.error(RED + "Ошибка: {}" + RESET, e.getMessage());
            e.printStackTrace();
        }
    }

    private static String selectVersion(JSONObject projectData) {
        JSONArray versions = projectData.getJSONArray("versions");
        List<String> versionList = new ArrayList<>();

        for (int i = versions.length() - 1; i >= 0; i--) {
            versionList.add(versions.getString(i));
        }

        int limit = Math.min(10, versionList.size());
        List<String> lastTen = versionList.subList(0, limit);

        Scanner scanner = new Scanner(System.in);
        System.out.println("\n" + YELLOW + "Доступные версии Velocity:" + RESET);
        for (int i = 0; i < lastTen.size(); i++) {
            System.out.println((i + 1) + ". " + lastTen.get(i));
        }

        System.out.println("\nВыберите версию Velocity (1-" + lastTen.size() + "): ");
        System.out.print(">> ");
        int choice = scanner.nextInt();

        if (choice < 1 || choice > lastTen.size()) {
            logger.warn(RED + "Некорректный выбор." + RESET);
            return null;
        }

        return lastTen.get(choice - 1);
    }

    private static int getLatestBuild(JSONObject buildsData) {
        JSONArray builds = buildsData.getJSONArray("builds");
        int latest = 0;
        for (int i = 0; i < builds.length(); i++) {
            JSONObject buildObj = builds.getJSONObject(i);
            int build = buildObj.getInt("build");
            if (build > latest) latest = build;
        }
        return latest;
    }


    private static String getFileName(String version, int buildNumber) {
        String url = "https://api.papermc.io/v2/projects/velocity/versions/" + version + "/builds/" + buildNumber;
        JSONObject buildData = getJSON(url);
        if (buildData == null) return null;

        JSONObject downloads = buildData.getJSONObject("downloads");
        if (downloads.has("application")) {
            JSONObject application = downloads.getJSONObject("application");
            return application.getString("name");
        }

        return null;
    }


    private static JSONObject getJSON(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (connection.getResponseCode() != 200) {
                System.err.println("HTTP ошибка: " + connection.getResponseCode());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return new JSONObject(response.toString());
            }
        } catch (Exception e) {
            System.err.println("Ошибка JSON запроса: " + e.getMessage());
            return null;
        }
    }
}