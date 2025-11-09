package org.cats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.cats.util.Colors.GREEN;
import static org.cats.util.Colors.RED;
import static org.cats.util.Colors.RESET;
import static org.cats.util.Eula.createEulaFile;

public interface Installer {
    Logger logger = LogManager.getLogger(Installer.class);

    void init();

    default Map<String, String> getLatestVersions(JSONObject manifest) {
        Map<String, String> latest = new HashMap<>();
        latest.put("release", manifest.getJSONObject("latest").getString("release"));
        latest.put("snapshot", manifest.getJSONObject("latest").getString("snapshot"));
        latest.put("beta", "b1.9-pre6");
        latest.put("alpha", "a1.2.6");
        return latest;
    }

    default String getServerJarURL(JSONObject manifest, String version) {
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

    default JSONObject getJSON(String url) {
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
            logger.error("Ошибка HTTP запроса: {}", e.getMessage());
            return null;
        }
    }

    default void downloadWithProgress(String fileURL, String saveFile) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            int fileSize = connection.getContentLength();

            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(saveFile);

            byte[] buffer = new byte[4096];
            int bytesRead, downloaded = 0;

            logger.info("Скачивание {}", saveFile + "\n");
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
                logger.info("{}Команду запуска менять не нужно. Оставьте команду запуска для Java Edition{}", GREEN, RESET);
            } else {
                logger.warn("{}Ошибка при переименовании файла!{}", RED, RESET);
            }


            logger.info("{}Файл успешно сохранён как server.jar!{}", GREEN, RESET);



        } catch (Exception e) {
            logger.error("{}Ошибка загрузки: {}{}", RED, e.getMessage(), RESET);
        }
        createEulaFile();
    }

    default void printProgress(int downloaded, int totalSize) {
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

    default String formatSize(int bytes) {
        String[] units = {"B", "KB", "MB", "GB"};
        double size = bytes;
        int unitIndex = 0;
        while (size > 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    default void animateLoading(int steps) {
        String[] frames = {"⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈"};
        for (int i = 0; i < steps; i++) {
            System.out.print("\rЗагрузка " + frames[i % frames.length]);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
        System.out.print("\r");
    }

    default void deleteFile(String fileName) { // Для Forge, Fabric и NeoForge
        File file = new File(fileName);
        if (file.exists()) {
            if (file.delete()) {
                logger.info("{}Временный файл {} удалён{}", RED, fileName, RESET);
            } else {
                logger.error("{}Не удалось удалить временный файл: {}{}", RED, fileName, RESET);
            }
        }
    }
}
