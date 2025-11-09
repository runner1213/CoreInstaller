package org.cats.installers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.Installer;
import org.json.JSONArray;
import org.json.JSONObject;

public class Paper implements Installer {
    private static final Logger logger = LogManager.getLogger(Paper.class);

    @Override
    public void init() {
        Scanner scanner = new Scanner(System.in);
        logger.info("Введите версию ядра Paper (1.21, 1.16.5, 1.12.2):");
        logger.info(">> ");
        String version = scanner.next();
        logger.debug(version);
        String build = String.valueOf(getLatestBuild(version));
        String fileName = "paper-" + version + "-" + build + ".jar";
        String url = "https://api.papermc.io/v2/projects/paper/versions/" + version + "/builds/" + build + "/downloads/" + fileName;

        downloadWithProgress(url, fileName);
    }

    private int getLatestBuild(String version) {
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
