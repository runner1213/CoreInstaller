package org.cats.installers;

import java.io.*;
import java.net.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.util.colors.*;

public class Fabric {
    private static final String GAME_VERSIONS_URL = "https://meta.fabricmc.net/v2/versions/game";
    private static final String LOADER_VERSIONS_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String INSTALLER_URL = "https://meta.fabricmc.net/v2/versions/installer";
    private static final String INSTALLER_FILE = "fabric-installer.jar";

    public static void installFabric() {
        try {
            // Шаг 1: Получить список версий Minecraft
            System.out.println(CYAN + "Получение списка версий Minecraft..." + RESET);
            JSONArray gameVersions = getJSONArray(GAME_VERSIONS_URL);
            if (gameVersions == null) {
                System.out.println(RED + "Ошибка при получении списка версий Minecraft." + RESET);
                return;
            }

            // Выбор версии Minecraft
            String minecraftVersion = selectMinecraftVersion(gameVersions);
            if (minecraftVersion == null) {
                return;
            }

            // Шаг 2: Получить версии загрузчика Fabric
            System.out.println(CYAN + "\nПолучение версий загрузчика Fabric..." + RESET);
            JSONArray loaderVersions = getJSONArray(LOADER_VERSIONS_URL);
            if (loaderVersions == null) {
                System.out.println(RED + "Ошибка при получении версий загрузчика." + RESET);
                return;
            }

            // Выбор версии загрузчика
            String loaderVersion = selectLoaderVersion(loaderVersions);
            if (loaderVersion == null) {
                return;
            }

            // Шаг 3: Получить версию установщика
            System.out.println(CYAN + "\nПолучение версии установщика..." + RESET);
            JSONArray installerVersions = getJSONArray(INSTALLER_URL);
            if (installerVersions == null) {
                System.out.println(RED + "Ошибка при получении версии установщика." + RESET);
                return;
            }

            String installerVersion = getLatestInstallerVersion(installerVersions);
            if (installerVersion == null) {
                System.out.println(RED + "Не удалось найти версию установщика." + RESET);
                return;
            }

            // Формирование URL установщика
            String installerUrl = String.format(
                    "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar",
                    installerVersion, installerVersion
            );

            System.out.println(CYAN + "\nСкачивание установщика Fabric..." + RESET);
            downloadWithProgress(installerUrl);

            System.out.println(YELLOW + "\nЗапуск установщика Fabric..." + RESET);
            runInstaller(minecraftVersion, loaderVersion);

            createEulaFile();

            System.out.println(GREEN + "\nFabric успешно установлен для Minecraft " + minecraftVersion + RESET);

        } catch (Exception e) {
            System.err.println(RED + "Ошибка: " + e.getMessage() + RESET);
            e.printStackTrace();
        } finally {
            deleteFile();
        }
    }

    private static String selectMinecraftVersion(JSONArray gameVersions) {
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

        System.out.print("\nВыберите версию Minecraft (1.21.5 т.д.) ");
        System.out.print(">> ");

        return scanner.nextLine();
    }

    private static String selectLoaderVersion(JSONArray loaderVersions) {
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

        System.out.print("\nВыберите версию загрузчика (1-" + lastFive.size() + "): ");
        System.out.print(">> ");
        int choice = scanner.nextInt();

        if (choice < 1 || choice > lastFive.size()) {
            System.out.println(RED + "Некорректный выбор." + RESET);
            return null;
        }

        return lastFive.get(choice - 1);
    }

    private static String getLatestInstallerVersion(JSONArray installerVersions) {
        for (int i = 0; i < installerVersions.length(); i++) {
            JSONObject installer = installerVersions.getJSONObject(i);
            if (installer.getBoolean("stable")) {
                return installer.getString("version");
            }
        }
        return null;
    }

    private static JSONArray getJSONArray(String url) {
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

    private static void runInstaller(String mcVersion, String loaderVersion) {
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

    private static void downloadWithProgress(String fileURL) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int fileSize = connection.getContentLength();
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(Fabric.INSTALLER_FILE)) {

                byte[] buffer = new byte[4096];
                int bytesRead, downloaded = 0;

                System.out.println("📥 Скачивание " + Fabric.INSTALLER_FILE);
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    printProgress(downloaded, fileSize);
                }
                System.out.println("\n" + GREEN + "Скачивание завершено!" + RESET);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки: " + e.getMessage());
        }
    }

    private static void printProgress(int downloaded, int totalSize) {
        int percent = totalSize > 0 ? (downloaded * 100) / totalSize : -1;
        int progressWidth = 30;
        int filled = progressWidth * percent / 100;

        StringBuilder bar = new StringBuilder("\r[");
        for (int i = 0; i < progressWidth; i++) {
            bar.append(i < filled ? "=" : "-");
        }

        String progressInfo;
        if (percent >= 0) {
            progressInfo = percent + "% (" + formatSize(downloaded) + "/" + formatSize(totalSize) + ")";
        } else {
            progressInfo = formatSize(downloaded);
        }

        System.out.print(bar + "] " + progressInfo);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }

    private static void createEulaFile() {
        try (FileWriter writer = new FileWriter("eula.txt")) {
            writer.write("eula=true\n");
            System.out.println(YELLOW + "Файл eula.txt создан" + RESET);
        } catch (IOException e) {
            System.err.println(RED + "Ошибка создания EULA: " + e.getMessage() + RESET);
        }
    }

    private static void deleteFile() {
        File file = new File(Fabric.INSTALLER_FILE);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println(RED + "Временный файл " + Fabric.INSTALLER_FILE + " удалён" + RESET);
            } else {
                System.err.println(RED + "Ошибка удаления: " + Fabric.INSTALLER_FILE + RESET);
            }
        }
    }
}