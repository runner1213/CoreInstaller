package org.cats.installers;

import java.io.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.Installer;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.cats.util.Colors.*;

public class Vanilla implements Installer {
    private static final Logger logger = LogManager.getLogger(Vanilla.class);

    @Override
    public void init() {
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
        logger.info("\nВыберите тип ядра:");
        logger.info("1. {}Release{}", GREEN, RESET);
        logger.info("2. {}Snapshot{}", CYAN, RESET);
        logger.info("3. {}Beta{}", YELLOW, RESET);
        logger.info("4. {}Alpha{}", RED, RESET);
        logger.info("Введите номер: ");
        logger.info(">> ");
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
}
