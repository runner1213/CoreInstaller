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
import static org.cats.util.Eula.createEulaFile;

public class Fabric implements Installer {
    private static final Logger logger = LogManager.getLogger(Fabric.class);

    private static final String GAME_VERSIONS_URL = "https://meta.fabricmc.net/v2/versions/game";
    private static final String LOADER_VERSIONS_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String INSTALLER_URL = "https://meta.fabricmc.net/v2/versions/installer";
    private static final String INSTALLER_FILE = "fabric-installer.jar";

    @Override
    public void init() {
        try {
            logger.info("{}Получение списка версий Minecraft...{}", CYAN, RESET);
            JSONArray gameVersions = getJSONArray(GAME_VERSIONS_URL);
            if (gameVersions == null) {
                System.out.println(RED + "Ошибка при получении списка версий Minecraft." + RESET);
                return;
            }

            String minecraftVersion = selectMinecraftVersion(gameVersions);
            if (minecraftVersion == null) {
                return;
            }

            logger.info("{}\nПолучение версий загрузчика Fabric...{}", CYAN, RESET);
            JSONArray loaderVersions = getJSONArray(LOADER_VERSIONS_URL);
            if (loaderVersions == null) {
                logger.error("{}Ошибка при получении версий загрузчика.{}", RED, RESET);
                return;
            }

            String loaderVersion = selectLoaderVersion(loaderVersions);
            if (loaderVersion == null) {
                return;
            }

            logger.info("{}\nПолучение версии установщика...{}", CYAN, RESET);
            JSONArray installerVersions = getJSONArray(INSTALLER_URL);
            if (installerVersions == null) {
                logger.info("{}Ошибка при получении версии установщика.{}", RED, RESET);
                return;
            }

            String installerVersion = getLatestInstallerVersion(installerVersions);
            if (installerVersion == null) {
                logger.error("{}Не удалось найти версию установщика.{}", RED, RESET);
                return;
            }

            String installerUrl = String.format(
                    "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar",
                    installerVersion, installerVersion
            );

            logger.info("{}\nСкачивание установщика Fabric...{}", CYAN, RESET);
            downloadWithProgress(installerUrl, "server.jar");

            logger.info("{}\nЗапуск установщика Fabric...{}", YELLOW, RESET);
            runInstaller(minecraftVersion, loaderVersion);

            createEulaFile();

            logger.info("{}\nFabric успешно установлен для Minecraft {}{}", GREEN, minecraftVersion, RESET);

        } catch (Exception e) {
            logger.error("Ошибка: {}", e.getMessage());
        } finally {
            deleteFile(INSTALLER_FILE);
        }
    }

    private String selectMinecraftVersion(JSONArray gameVersions) {
        List<String> versions = new ArrayList<>();

        for (int i = 0; i < gameVersions.length(); i++) {
            JSONObject version = gameVersions.getJSONObject(i);
            if (version.getBoolean("stable")) {
                versions.add(version.getString("version"));
            }
        }

        //int limit = Math.min(10, versions.size());
        //List<String> lastTen = versions.subList(0, limit);

        Scanner scanner = new Scanner(System.in);
        /*
        System.out.println("\n" + YELLOW + "Доступные версии Minecraft: " + RESET);
        for (int i = 0; i < lastTen.size(); i++) {
            System.out.println((i + 1) + ". " + lastTen.get(i));
        }
         */

        System.out.println("\nВыберите версию Minecraft (1.21.5 т.д.) ");
        System.out.print(">> ");

        return scanner.nextLine();
    }

    private String selectLoaderVersion(JSONArray loaderVersions) {
        List<String> versions = new ArrayList<>();

        for (int i = 0; i < loaderVersions.length(); i++) {
            JSONObject version = loaderVersions.getJSONObject(i);
            if (version.getBoolean("stable")) {
                versions.add(version.getString("version"));
            }
        }

        int limit = Math.min(5, versions.size());
        List<String> lastFive = versions.subList(0, limit);

        Scanner scanner = new Scanner(System.in);
        System.out.println("\n" + YELLOW + "Доступные версии загрузчика Fabric:" + RESET);
        for (int i = 0; i < lastFive.size(); i++) {
            System.out.println((i + 1) + ". " + lastFive.get(i));
        }

        System.out.println("\nВыберите версию загрузчика (1-" + lastFive.size() + "): ");
        System.out.print(">> ");
        int choice = scanner.nextInt();

        if (choice < 1 || choice > lastFive.size()) {
            logger.warn(RED + "Некорректный выбор." + RESET);
            return null;
        }

        return lastFive.get(choice - 1);
    }

    private String getLatestInstallerVersion(JSONArray installerVersions) {
        for (int i = 0; i < installerVersions.length(); i++) {
            JSONObject installer = installerVersions.getJSONObject(i);
            if (installer.getBoolean("stable")) {
                return installer.getString("version");
            }
        }
        return null;
    }

    private JSONArray getJSONArray(String url) {
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
                return new JSONArray(response.toString());
            }
        } catch (Exception e) {
            System.err.println("Ошибка JSON запроса: " + e.getMessage());
            return null;
        }
    }

    private void runInstaller(String mcVersion, String loaderVersion) {
        try {
            Process process = new ProcessBuilder(
                    "java", "-jar", Fabric.INSTALLER_FILE,
                    "server", "-mcversion", mcVersion,
                    "-loader", loaderVersion, "-downloadMinecraft"
            )
                    .inheritIO()
                    .start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Установщик завершился с кодом ошибки: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка запуска установщика: " + e.getMessage());
        }
    }
}