package org.cats.installers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.util.colors.*;

public class Vanilla {
    public static void VanillaInstaller() {
        System.out.println("Получение списка версий...");
        animateLoading(10);

        JSONObject manifest = getJSON("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        if (manifest == null) {
            System.out.println(RED + "Ошибка при получении списка версий." + RESET);
            return;
        }

        Map<String, String> latestVersions = getLatestVersions(manifest);
        JSONArray versions = manifest.getJSONArray("versions");

        // Запрос выбора типа версии
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nВыберите тип ядра:");
        System.out.println("1. "+ GREEN + "Release" + RESET);
        System.out.println("2. "+ CYAN + "Snapshot" + RESET);
        System.out.println("3. "+  YELLOW + "Beta" + RESET);
        System.out.println("4. " + RED + "Alpha" + RESET);
        System.out.print("Введите номер: ");
        int typeChoice = scanner.nextInt();
        String type = getTypeFromChoice(typeChoice);

        // Получение списка версий по выбранному типу
        List<String> filteredVersions = filterVersionsByType(versions, type);
        if (filteredVersions.isEmpty()) {
            System.out.println(RED + "Нет доступных версий для выбранного типа." + RESET);
            return;
        }

        // Выводим 10 последних версий
        int limit = Math.min(10, filteredVersions.size());
        System.out.println("\n" + YELLOW +"Доступные версии:" + RESET);
        for (int i = 0; i < limit; i++) {
            System.out.println((i + 1) + ". " + filteredVersions.get(i));
        }

        // Выбор версии
        System.out.print("Введите номер версии (или 0 для последней): ");
        int choice = scanner.nextInt();
        scanner.close();

        String selectedVersion = choice == 0 ? latestVersions.get(type) :
                (choice > 0 && choice <= limit ? filteredVersions.get(choice - 1) : null);

        if (selectedVersion == null) {
            System.out.println(RED + "Некорректный ввод." + RESET);
            return;
        }

        System.out.println(CYAN + "Получение ссылки для версии " + selectedVersion + "..." + RESET);
        animateLoading(5);

        String serverJarURL = getServerJarURL(manifest, selectedVersion);
        if (serverJarURL != null) {
            System.out.println(GREEN + "Ссылка получена!" + RESET);
            System.out.println("💾 Начинаем скачивание server.jar...");
            downloadWithProgress(serverJarURL, "server-" + selectedVersion + ".jar");
        } else {
            System.out.println("❌ Ошибка при получении ссылки.");
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

    // Определение типа по выбору
    private static String getTypeFromChoice(int choice) {
        return switch (choice) {
            case 2 -> "snapshot";
            case 3 -> "beta";
            case 4 -> "alpha";
            default -> "release";
        };
    }

    // Получение последней версии каждого типа
    private static Map<String, String> getLatestVersions(JSONObject manifest) {
        Map<String, String> latest = new HashMap<>();
        latest.put("release", manifest.getJSONObject("latest").getString("release"));
        latest.put("snapshot", manifest.getJSONObject("latest").getString("snapshot"));
        latest.put("beta", "b1.9-pre6"); // Пример для бета-версии
        latest.put("alpha", "a1.2.6");  // Пример для альфа-версии
        return latest;
    }

    // Получение ссылки на server.jar
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

    // HTTP-запрос
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
            System.err.println("Ошибка HTTP запроса: " + e.getMessage());
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

            System.out.println("📥 Скачивание " + saveFile);
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                printProgress(downloaded, fileSize);
            }

            inputStream.close();
            outputStream.close();
            System.out.println("\n" + GREEN + "Скачивание завершено!" + RESET);

            // Переименование в server.jar
            File downloadedFile = new File(saveFile);
            File serverJar = new File("server.jar");

            if (serverJar.exists()) {
                serverJar.delete();
            }

            if (downloadedFile.renameTo(serverJar)) {
                System.out.println(GREEN + "Файл успешно переименован в server.jar!" + RESET);
            } else {
                System.err.println(RED + "Ошибка при переименовании файла!" + RESET);
            }

        } catch (Exception e) {
            System.err.println(RED + "Ошибка загрузки: " + e.getMessage() + RESET);
        }
    }

    // Прогресс-бар загрузки
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

    // Форматирование размера
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

    // Анимация загрузки
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
