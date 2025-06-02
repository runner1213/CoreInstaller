package org.cats.installers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import static org.cats.util.Colors.*;
import static org.cats.util.Eula.createEulaFile;

public class Forge {
    private static final Logger logger = LogManager.getLogger(Forge.class);

    private static final String FILE_URL = "https://www.curseforge.com/api/v1/mods/525582/files/5253323/download";
    private static final String FILE_NAME = "installer.jar";

    public static void installForge() {
        logger.info(CYAN + "Downloading " + FILE_NAME + "..." + RESET);

        if (downloadFile(FILE_URL, FILE_NAME)) {
            logger.info(GREEN + "File downloaded successfully!" + RESET);

            if (runInstaller(FILE_NAME)) {
                logger.info(GREEN + "Installation completed successfully!" + RESET);
                createEulaFile();
            } else {
                System.out.println(YELLOW + "Keeping installer for manual inspection" + RESET);
            }
            deleteFile(FILE_NAME);
        } else {
            logger.warn(RED + "Download failed." + RESET);
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
            logger.error("Download error: {}", e.getMessage());
            return false;
        }
    }

    public static boolean runInstaller(String fileName) {
        System.out.println(YELLOW + "Launching " + fileName + "..." + RESET);

        try {
            Process process = new ProcessBuilder("java", "-Xms128M", "-Xmx1G", "-jar", fileName)
                    .inheritIO()
                    .start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn(RED + "Installer failed with exit code: {}" + RESET, exitCode);
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.error(RED + "Execution error: {}" + RESET, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            logger.error(RED + "Installation interrupted" + RESET);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            if (file.delete()) {
                logger.info(RED + "File {} deleted." + RESET, fileName);
            } else {
                logger.error(RED + "Failed to delete file: {}" + RESET, fileName);
            }
        }
    }
}