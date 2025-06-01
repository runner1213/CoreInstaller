package org.cats.installers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import static org.cats.util.colors.*;

public class Forge {
    private static final String FILE_URL = "https://www.curseforge.com/api/v1/mods/525582/files/5253323/download";
    private static final String FILE_NAME = "installer.jar";
    private static final String EULA_FILE = "eula.txt";

    public static void installForge() {
        System.out.println(CYAN + "Downloading " + FILE_NAME + "..." + RESET);

        if (downloadFile(FILE_URL, FILE_NAME)) {
            System.out.println(GREEN + "File downloaded successfully!" + RESET);

            if (runInstaller(FILE_NAME)) {
                System.out.println(GREEN + "Installation completed successfully!" + RESET);
                createEulaFile();
            } else {
                System.out.println(YELLOW + "Keeping installer for manual inspection" + RESET);
            }
            deleteFile(FILE_NAME);
        } else {
            System.out.println(RED + "Download failed." + RESET);
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
            System.err.println("Download error: " + e.getMessage());
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
                System.err.println(RED + "Installer failed with exit code: " + exitCode + RESET);
                return false;
            }
            return true;
        } catch (IOException e) {
            System.err.println(RED + "Execution error: " + e.getMessage() + RESET);
            return false;
        } catch (InterruptedException e) {
            System.err.println(RED + "Installation interrupted" + RESET);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void createEulaFile() {
        try (FileWriter writer = new FileWriter(EULA_FILE)) {
            writer.write("eula=true\n");
            System.out.println(YELLOW + "EULA file created at: " + new File(EULA_FILE).getAbsolutePath() + RESET);
        } catch (IOException e) {
            System.err.println(RED + "Error creating EULA file: " + e.getMessage() + RESET);
        }
    }

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println(RED + "File " + fileName + " deleted." + RESET);
            } else {
                System.err.println(RED + "Failed to delete file: " + fileName + RESET);
            }
        }
    }
}