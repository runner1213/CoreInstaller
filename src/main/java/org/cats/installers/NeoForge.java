package org.cats.installers;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.Installer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import static org.cats.util.Eula.createEulaFile;
import static org.cats.util.Colors.*;

public class NeoForge implements Installer {
    private static final Logger logger = LogManager.getLogger(NeoForge.class);

    private static final String MAVEN_METADATA_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String MINECRAFT_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String INSTALLER_URL_TEMPLATE = "https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar";
    private static final String INSTALLER_FILE = "neoforge-installer.jar";

    @Override
    public void init() {
        try {
            logger.info("{}Получение списка версий Minecraft...{}", CYAN, RESET);
            JSONObject minecraftManifest = getMinecraftManifest();
            if (minecraftManifest == null) {
                logger.error("{}Ошибка при получении списка версий Minecraft.{}", RED, RESET);
                return;
            }

            String minecraftVersion = selectMinecraftVersion(minecraftManifest);
            if (minecraftVersion == null) {
                return;
            }

            logger.info("{}\nПолучение совместимых версий NeoForge для Minecraft {}...{}", CYAN, minecraftVersion, RESET);
            animateLoading(5);

            Document metadata = getXML();
            if (metadata == null) {
                logger.error(RED + "Ошибка при получении метаданных NeoForge." + RESET);
                return;
            }

            List<String> allVersions = getAllVersions(metadata);
            List<String> compatibleVersions = filterCompatibleVersions(allVersions, minecraftVersion);

            if (compatibleVersions.isEmpty()) {
                logger.warn(RED + "Не найдено совместимых версий NeoForge для Minecraft {}" + RESET, minecraftVersion);
                return;
            }

            String selectedNeoForgeVersion = selectNeoForgeVersion(compatibleVersions);
            if (selectedNeoForgeVersion == null) {
                return;
            }

            String installerUrl = String.format(INSTALLER_URL_TEMPLATE, selectedNeoForgeVersion, selectedNeoForgeVersion);
            logger.info(CYAN + "\nСкачивание установщика {}..." + RESET, selectedNeoForgeVersion);
            downloadWithProgress(installerUrl, INSTALLER_FILE);

            logger.info(YELLOW + "\nЗапуск установщика..." + RESET);
            runInstaller(INSTALLER_FILE);

            createEulaFile();
            deleteFile(INSTALLER_FILE);

            logger.info("{}\nNeoForge успешно установлен для Minecraft {}{}", GREEN, minecraftVersion, RESET);

        } catch (Exception e) {
            logger.error(RED + "Ошибка: {}" + RESET, e.getMessage());
            e.printStackTrace();
        }
    }

    private JSONObject getMinecraftManifest() {
        try {
            URL url = new URL(MINECRAFT_MANIFEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return new JSONObject(response.toString());
            }
        } catch (Exception e) {
            logger.error("Ошибка получения манифеста Minecraft: {}", e.getMessage());
            return null;
        }
    }

    private String selectMinecraftVersion(JSONObject manifest) {
        JSONArray versions = manifest.getJSONArray("versions");
        List<String> releaseVersions = new ArrayList<>();

        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            if ("release".equals(version.getString("type"))) {
                releaseVersions.add(version.getString("id"));
            }
        }

        int limit = Math.min(10, releaseVersions.size());
        List<String> lastTen = new ArrayList<>(releaseVersions.subList(0, limit));

        Scanner scanner = new Scanner(System.in);
        System.out.println("\n" + YELLOW + "Доступные версии Minecraft:" + RESET);
        for (int i = 0; i < lastTen.size(); i++) {
            System.out.println((i + 1) + ". " + lastTen.get(i));
        }

        System.out.println("\nВыберите версию Minecraft (1-" + lastTen.size() + "): ");
        System.out.print(">> ");
        int choice = scanner.nextInt();

        if (choice < 1 || choice > lastTen.size()) {
            System.out.println(RED + "Некорректный выбор." + RESET);
            return null;
        }

        return lastTen.get(choice - 1);
    }

    private List<String> filterCompatibleVersions(List<String> allVersions, String minecraftVersion) {
        List<String> compatible = new ArrayList<>();
        for (String version : allVersions) {
            // <minecraft_version>-<neoforge_version>
            if (version.startsWith(minecraftVersion + "-")) {
                compatible.add(version);
            }
        }
        compatible.sort(Collections.reverseOrder());
        return compatible;
    }

    private String selectNeoForgeVersion(List<String> versions) {
        if (versions.isEmpty()) {
            return null;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("\n" + YELLOW + "Доступные версии NeoForge:" + RESET);
        for (int i = 0; i < versions.size(); i++) {
            System.out.println((i + 1) + ". " + versions.get(i));
        }

        System.out.println("\nВыберите версию NeoForge (1-" + versions.size() + "): ");
        System.out.print(">> ");
        int choice = scanner.nextInt();

        if (choice < 1 || choice > versions.size()) {
            System.out.println(RED + "Некорректный выбор." + RESET);
            return null;
        }

        return versions.get(choice - 1);
    }

    private Document getXML() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(NeoForge.MAVEN_METADATA_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(connection.getInputStream()));
        } catch (Exception e) {
            System.err.println("Ошибка XML: " + e.getMessage());
            return null;
        }
    }

    private List<String> getAllVersions(Document doc) {
        List<String> versions = new ArrayList<>();
        NodeList versionNodes = doc.getElementsByTagName("version");

        for (int i = 0; i < versionNodes.getLength(); i++) {
            versions.add(versionNodes.item(i).getTextContent());
        }
        return versions;
    }

    private void runInstaller(String fileName) {
        try {
            Process process = new ProcessBuilder("java", "-jar", fileName, "--installServer")
                    .inheritIO()
                    .start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Установщик завершился с кодом ошибки: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка запуска: " + e.getMessage());
        }
    }
}