package org.cats.installers;

import java.io.*;
import java.net.URL;
import static org.cats.util.colors.*;

public class Forge {
    private static final String FILE_URL = "https://www.curseforge.com/api/v1/mods/525582/files/5253323/download";
    private static final String FILE_NAME = "installer.jar";

    public static void installForge() {
        System.out.println(CYAN + "Скачивание " + FILE_NAME + "..." + RESET);
        if (downloadFile(FILE_URL, FILE_NAME)) {
            System.out.println(GREEN + "Файл успешно загружен!" + RESET);
            runInstaller(FILE_NAME);
            deleteFile(FILE_NAME);
            try (FileWriter writer = new FileWriter("eula.txt")) {
                writer.write("eula=true\n");
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            System.out.println(YELLOW + "Файл eula.txt создан.");
        } else {
            System.out.println(RED + "Ошибка скачивания." + RESET);
        }
    }

    public static boolean downloadFile(String fileURL, String saveFile) {
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(fileURL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer, 0, 4096)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            return true;

        } catch (IOException e) {
            System.err.println("Ошибка загрузки: " + e.getMessage());
            return false;
        }
    }

    public static void runInstaller(String fileName) {
        System.out.println(YELLOW + "Запуск " + fileName + "..." + RESET);
        try {
            Process process = new ProcessBuilder("java", "-Xms128M", "-Xmx1G", "-jar", fileName)
                    .inheritIO()
                    .start();
            process.waitFor();
            System.out.println(GREEN + "Установка завершена!" + RESET);
        } catch (IOException | InterruptedException e) {
            System.err.println(RED + "Ошибка запуска: " + e.getMessage() + RESET);
        }
    }

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.delete()) {
            System.out.println(RED + "Файл " + fileName + " удалён." + RESET);
        } else {
            System.err.println(RED + "Ошибка удаления файла." + RESET);
        }
    }
}
