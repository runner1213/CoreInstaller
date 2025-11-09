package org.cats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.util.InstallerFactory;

import java.util.InputMismatchException;
import java.util.Scanner;

import static org.cats.util.Colors.*;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        application();
    }

    protected static void application() {
        logger.info("{}Успешная инициализация{}", GREEN, RESET);

        // Выбор ядра и всё такое
        logger.info("Выберите ядро для установки:");
        logger.info("1. Vanilla 2. Paper 3. Velocity 4. Forge 5. Fabric 6. NeoForge");
        logger.info(">> ");
        try {
            int choice = scanner.nextInt();

            Installer installer = InstallerFactory.createInstaller(choice);

            if (installer != null) {
                installer.init();
            } else {
                logger.warn("{}Некорректный выбор — попробуйте снова{}", RED, RESET);
                application();
            }
        } catch (InputMismatchException e) {
            logger.warn("Введено нецелое число: {}", e.getMessage());
            logger.error("{}Нужно ввести число от 1 до 6!{}", RED, RESET);
            scanner.nextLine();
            application();
        }
    }
}