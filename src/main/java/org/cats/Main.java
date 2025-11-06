package org.cats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.InputMismatchException;
import java.util.Scanner;

import static org.cats.installers.Fabric.installFabric;
import static org.cats.installers.Forge.installForge;
import static org.cats.installers.NeoForge.installNeoForge;
import static org.cats.installers.Paper.paperinstall;
import static org.cats.installers.Vanilla.VanillaInstaller;
import static org.cats.installers.Velocity.installVelocity;
import static org.cats.util.Colors.*;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        application();
    }
    protected static void application() {
        logger.info("{}Успешная инициализация{}", GREEN, RESET);

        // Проверка зрения
        /*
        logger.info("{}Текст для проверки не имеющий смысла{}", CYAN, RESET);
        logger.info("-------------------------");
        logger.info("'1' - Не вижу текст\n* (любой символ в поле ввода) - вижу текст");


        System.out.print(">> ");

        try {
            String colors = scanner.nextLine();
            if (colors.trim().equals("1")) {
                colorful(false);
                logger.info("Цветной текст успешно отключен!");
            }
            else if (colors.isEmpty()) { throw new InputMismatchException(); }
        } catch (InputMismatchException ignored) {}

         */

        // Выбор ядра и всё такое
        logger.info("Выберите ядро для установки:");
        logger.info("1. Vanilla\n 2. Paper\n 3. Velocity\n 4. Forge\n 5. Fabric\n 6. NeoForge");
        logger.info(">> ");
        try {
            switch (scanner.nextInt()) {
                case 1 -> VanillaInstaller();
                case 2 -> paperinstall();
                case 3 -> installVelocity();
                case 4 -> installForge();
                case 5 -> installFabric();
                case 6 -> installNeoForge();
                default -> {
                    logger.warn("{}Некорректный выбор{}", RED, RESET);
                    logger.warn("{}Попробуйте снова{}", YELLOW, RESET);
                    application();
                }
            }
        } catch (InputMismatchException e) {
            logger.warn("Введено нецелое число: {}", e.getMessage());
            logger.error("{}Нужно ввести число от 1 до 6!{}", RED, RESET);
            scanner.nextLine();
            application();
        }
    }
}