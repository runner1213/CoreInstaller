package org.cats.installers;

import java.io.*;
import java.net.*;
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

    private static final String RELEASES_URL = "https://api.papermc.io/v2/projects/velocity";
    private static final String DOWNLOAD_URL_TEMPLATE = "https://api.papermc.io/v2/projects/velocity/versions/%s/builds/%s/downloads/%s";
    private static final String JAR_FILE = "server.jar";

    @Override
    public void init() {
        try {
            logger.info("{}Получение информации о версиях...{}", CYAN, RESET);
            JSONObject projectData = getJSON(RELEASES_URL);
            if (projectData == null) {
                logger.error("{}Ошибка при получении данных о версиях.{}", RED, RESET);
                return;
            }

            String selectedVersion = selectVersion(projectData);
            if (selectedVersion == null) return;

            logger.info("{}Получение информации о сборках...{}", CYAN, RESET);
            String buildsUrl = RELEASES_URL + "/versions/" + selectedVersion + "/builds";
            JSONObject buildsData = getJSON(buildsUrl);
            if (buildsData == null) {
                logger.error("{}Ошибка при получении данных о сборках.{}", RED, RESET);
                return;
            }

            int latestBuild = getLatestBuild(buildsData);
            String fileName = getFileName(selectedVersion, latestBuild);
            if (fileName == null) {
                logger.error("{}Не удалось определить имя файла для скачивания.{}", RED, RESET);
                return;
            }

            String downloadUrl = String.format(DOWNLOAD_URL_TEMPLATE, selectedVersion, latestBuild, fileName);

            logger.info("{}Скачивание Velocity {} (build #{})...{}", CYAN, selectedVersion, latestBuild, RESET);
            downloadWithProgress(downloadUrl, JAR_FILE);

            createEulaFile();

            logger.info("{}Velocity успешно установлен!{}", GREEN, RESET);

        } catch (Exception e) {
            logger.error("{}Ошибка: {}{}", RED, e.getMessage(), RESET);
            e.printStackTrace();
        }
    }

    private String selectVersion(JSONObject projectData) {
        JSONArray versions = projectData.getJSONArray("versions");
        List<String> versionList = new ArrayList<>();

        for (int i = versions.length() - 1; i >= 0; i--) {
            versionList.add(versions.getString(i));
        }

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

    private int getLatestBuild(JSONObject buildsData) {
        JSONArray builds = buildsData.getJSONArray("builds");
        int latest = 0;
        for (int i = 0; i < builds.length(); i++) {
            JSONObject buildObj = builds.getJSONObject(i);
            int build = buildObj.getInt("build");
            if (build > latest) latest = build;
        }
        return latest;
    }

    private String getFileName(String version, int buildNumber) {
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
}
