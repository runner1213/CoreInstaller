package org.cats.installers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.util.Colors.*;
import static org.cats.util.Eula.*;

public class Vanilla {
    private static final Logger logger = LogManager.getLogger(Vanilla.class);
    public static void VanillaInstaller() {
        logger.info("Получение списка версий...");
        animateLoading(10);

        JSONObject manifest = getJSON("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        if (manifest == null) {
            logger.error("{}Ошибка при получении списка версий.{}", RED, RESET);
            return;
        }

        Map<String, String> latestVersions = getLatestVersions(manifest);
        JSONArray versions = manifest.getJSONArray("versions");

        Scanner scanner = new Scanner(System.in);
        System.out.println("\nВыберите тип ядра:");
        System.out.println("1. "+ GREEN + "Release" + RESET);
        System.out.println("2. "+ CYAN + "Snapshot" + RESET);
        System.out.println("3. "+  YELLOW + "Beta" + RESET);
        System.out.println("4. " + RED + "Alpha" + RESET);
        System.out.print("Введите номер: ");
        System.out.print(">> ");
        int typeChoice = scanner.nextInt();
        String type = getTypeFromChoice(typeChoice);

        List<String> filteredVersions = filterVersionsByType(versions, type);
        if (filteredVersions.isEmpty()) {
            logger.warn(RED + "Нет доступных версий для выбранного типа." + RESET);
            return;
        }

        // Вывод 10 последних версий
        int limit = Math.min(10, filteredVersions.size());
        System.out.println("\n" + YELLOW + limit + " доступных версий:" + RESET);
        for (int i = 0; i < limit; i++) {
            System.out.println((i + 1) + ". " + filteredVersions.get(i));
        }
        scanner.nextLine();

        System.out.println(GREEN + "Введите полную версию (1.21.5, 1.17, 1.12.2)" + RESET);
        System.out.print(">> ");
        String selectedVersion = scanner.nextLine().trim();

        /*
        if (selectedVersion == null) {
            System.out.println(RED + "Некорректный ввод." + RESET);
            return;
        }
         */

        logger.info("{}Получение ссылки для версии {}...{}", CYAN, selectedVersion, RESET);
        animateLoading(3);

        String serverJarURL = getServerJarURL(manifest, selectedVersion);
        if (serverJarURL != null) {
            logger.info("{}Ссылка получена!{}", GREEN, RESET);
            logger.info("{}Начало скачивания server.jar...{}", GREEN, RESET);
            downloadWithProgress(serverJarURL, "server-" + selectedVersion + ".jar");
        } else {
            logger.error("{}Ошибка при получении ссылки.{}", RED, RESET);
        }
    }

    private static List<String> filterVersionsByType(JSONArray versions, String type) {
        List<String> filtered = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            if (type.equals("snapshot") && version.getString("type").equals("snapshot") ||
                    type.equals("release") && version.getString("type").equals("release") ||
                    type.equals("beta") && version.getString("id").startsWith("b") ||
                    type.equals("alpha") && version.getString("id").startsWith("a")) {
                filtered.add(version.getString("id"));
            }
        }
        return filtered;
    }

    private static String getTypeFromChoice(int choice) {
        return switch (choice) {
            case 2 -> "snapshot";
            case 3 -> "beta";
            case 4 -> "alpha";
            default -> "release";
        };
    }

    private static Map<String, String> getLatestVersions(JSONObject manifest) {
        Map<String, String> latest = new HashMap<>();
        latest.put("release", manifest.getJSONObject("latest").getString("release"));
        latest.put("snapshot", manifest.getJSONObject("latest").getString("snapshot"));
        latest.put("beta", "b1.9-pre6");
        latest.put("alpha", "a1.2.6");
        return latest;
    }

    public static String getServerJarURL(JSONObject manifest, String version) {
        JSONArray versions = manifest.getJSONArray("versions");
        for (int i = 0; i < versions.length(); i++) {
            JSONObject obj = versions.getJSONObject(i);
            if (obj.getString("id").equals(version)) {
                JSONObject versionData = getJSON(obj.getString("url"));
                return versionData != null ? versionData.getJSONObject("downloads").getJSONObject("server").getString("url") : null;
            }
        }
        return null;
    }

    public static JSONObject getJSON(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return new JSONObject(response.toString());
        } catch (Exception e) {
            logger.error("Ошибка HTTP запроса: " + e.getMessage());
            return null;
        }
    }

    public static void downloadWithProgress(String fileURL, String saveFile) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            int fileSize = connection.getContentLength();

            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(saveFile);

            byte[] buffer = new byte[4096];
            int bytesRead, downloaded = 0;

            logger.info("\uD83D\uDCE5 Скачивание {}", saveFile);
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                printProgress(downloaded, fileSize);
            }

            inputStream.close();
            outputStream.close();
            logger.info("\n{}Скачивание завершено!{}", GREEN, RESET);


            File downloadedFile = new File(saveFile);
            File serverJar = new File("server.jar");

            if (downloadedFile.renameTo(serverJar)) {
                logger.info("{}Файл успешно переименован в server.jar!{}", GREEN, RESET);
                System.out.println(GREEN + "Команду запуска менять не нужно. Оставьте команду запуска для Java Edition" + RESET);
            } else {
                logger.warn("{}Ошибка при переименовании файла!{}", RED, RESET);
            }


            logger.info("{}Файл успешно сохранён как server.jar!{}", GREEN, RESET);



        } catch (Exception e) {
            logger.error("{}Ошибка загрузки: {}{}", RED, e.getMessage(), RESET);
        }
        createEulaFile();
    }

    public static void printProgress(int downloaded, int totalSize) {
        int percent = totalSize > 0 ? (downloaded * 100) / totalSize : -1;
        int progressWidth = 30;
        int filled = (percent * progressWidth) / 100;

        StringBuilder progressBar = new StringBuilder("\r[");
        for (int i = 0; i < progressWidth; i++) {
            progressBar.append(i < filled ? "=" : "-");
        }
        progressBar.append("] ").append(percent >= 0 ? percent + "%" : formatSize(downloaded));

        System.out.print(progressBar);
    }

    public static String formatSize(int bytes) {
        String[] units = {"B", "KB", "MB", "GB"};
        double size = bytes;
        int unitIndex = 0;
        while (size > 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    public static void animateLoading(int steps) {
        String[] frames = {"⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈"};
        for (int i = 0; i < steps; i++) {
            System.out.print("\r⏳ Загрузка " + frames[i % frames.length]);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
        System.out.print("\r");
    }
}
