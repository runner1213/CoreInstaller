package org.cats.util;

import java.io.*;

public class LauncherWrapper {
    public static void launchJar() {
        try {
            File jarFile = new File("server.jar");
            if (!jarFile.exists()) {
                System.err.println("Ошибка: server.jar не найден!");
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xms128M", "-jar", "server.jar");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Ожидаем завершения процесса
            int exitCode = process.waitFor();
            System.out.println("Сервер завершил работу с кодом: " + exitCode);
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка запуска сервера.");
            System.err.println(e.getMessage());
        }
    }
}
