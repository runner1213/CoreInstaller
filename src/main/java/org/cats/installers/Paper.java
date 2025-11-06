package org.cats.installers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.util.Colors.*;
import static org.cats.util.Eula.createEulaFile;

public class Paper {
    private static final Logger logger = LogManager.getLogger(Paper.class);

    public static void paperinstall() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите версию:");
        System.out.print(">> ");
        String version = scanner.next();
        String build = String.valueOf(getLatestBuild(version));
        String fileName = "paper-" + version + "-" + build + ".jar";
        String url = "https://api.papermc.io/v2/projects/paper/versions/" + version + "/builds/" + build + "/downloads/" + fileName;

        downloadWithProgress(url, fileName);
    }

    public static void downloadWithProgress(String fileURL, String saveFile) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            long fileSize = connection.getContentLengthLong();
            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(saveFile);

            byte[] buffer = new byte[4096];
            int bytesRead, downloaded = 0;

            logger.info("Скачивание {}", saveFile);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                if (fileSize > 0) {
                    printProgress(downloaded, fileSize);
                } else {
                    printUnknownSizeProgress(downloaded);
                }
            }

            inputStream.close();
            outputStream.close();
            logger.info("\n{}Скачивание завершено!{}", GREEN, RESET);

            File downloadedFile = new File(saveFile);
            File serverJar = new File("server.jar");

            if (serverJar.exists()) {
                serverJar.delete();
            }

            if (downloadedFile.renameTo(serverJar)) {
                logger.info("{}Файл успешно переименован в server.jar!{}", GREEN, RESET);
                System.out.println(GREEN + "Команду запуска менять не нужно. Оставьте команду запуска для Java Edition" + RESET);
            } else {
                logger.warn("{}Ошибка при переименовании файла!{}", RED, RESET);
            }
            createEulaFile();

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }


    public static void printProgress(long downloaded, long totalSize) {
        int percent = (int) ((double) downloaded / totalSize * 100);
        int progressBars = percent / 2;

        StringBuilder bar = new StringBuilder("\r" + percent + "% ");
        for (int i = 0; i < 50; i++) {
            if (i < progressBars) bar.append("=");
            else bar.append("-");
        }
        System.out.print(bar);
    }

    public static void printUnknownSizeProgress(long downloaded) {
        logger.info("\r\n{} MB", downloaded / 1024 / 1024);
    }

    public static int getLatestBuild(String version) {
        try {
            String apiUrl = "https://api.papermc.io/v2/projects/paper/versions/" + version;
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray builds = json.getJSONArray("builds");

            return builds.getInt(builds.length() - 1);

        } catch (Exception e) {
            return -1; // Ошибка
        }
    }
}
