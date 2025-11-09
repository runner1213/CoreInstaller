package org.cats.installers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.Installer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import static org.cats.util.Colors.*;
import static org.cats.util.Eula.createEulaFile;

public class Forge implements Installer {
    private static final Logger logger = LogManager.getLogger(Forge.class);

    private static final String FILE_URL = "https://www.curseforge.com/api/v1/mods/525582/files/5253323/download";
    private static final String FILE_NAME = "installer.jar";

    @Override
    public void init() {
        logger.info("{}Скачивание " + FILE_NAME + "...{}", CYAN, RESET);

        if (downloadFile(FILE_URL, FILE_NAME)) {
            logger.info("{}Файл скачан успешно!{}", GREEN, RESET);

            if (runInstaller(FILE_NAME)) {
                logger.info("{}Установка завершена успешно!{}", GREEN, RESET);
                createEulaFile();
            } else {
                System.out.println(YELLOW + "Keeping installer for manual inspection" + RESET);
            }
            deleteFile(FILE_NAME);
        } else {
            logger.warn("{}Не получилось установить{}", RED, RESET);
        }
    }

    public static boolean downloadFile(String fileURL, String saveFile) {
        try {
            URL url = new URL(fileURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, Path.of(saveFile), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            logger.error("Ошибка скачивания: {}", e.getMessage());
            return false;
        }
    }

    public static boolean runInstaller(String fileName) {
        System.out.println(YELLOW + "Запуск " + fileName + "..." + RESET);

        try {
            Process process = new ProcessBuilder("java", "-Xms128M", "-Xmx1G", "-jar", fileName)
                    .inheritIO()
                    .start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("{}IУстановщик закрыт с кодом выхода: {}{}", RED, exitCode, RESET);
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.error("{}Ошибка выполнения: {}{}", RED, e.getMessage(), RESET);
            return false;
        } catch (InterruptedException e) {
            logger.error("{}Установка прервана{}", RED, RESET);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}