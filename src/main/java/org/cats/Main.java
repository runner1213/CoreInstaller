package org.cats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.io.ScannerUserInput;
import org.cats.io.UserInput;
import org.cats.util.Eula;
import org.cats.util.InstallerFactory;

import java.util.Scanner;

import static org.cats.util.Colors.GREEN;
import static org.cats.util.Colors.RED;
import static org.cats.util.Colors.RESET;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    private final UserInput input;
    private final InstallerFactory installerFactory;

    public Main(UserInput input, InstallerFactory installerFactory) {
        this.input = input;
        this.installerFactory = installerFactory;
    }

    public static void main(String[] args) {
        UserInput input = new ScannerUserInput(new Scanner(System.in));
        Eula eula = new Eula(input);
        InstallerFactory installerFactory = new InstallerFactory(input, eula);

        new Main(input, installerFactory).application();
    }

    public void application() {
        logger.info("{}Успешная инициализация{}", GREEN, RESET);

        while (true) {
            logger.info("Выберите ядро для установки:");
            logger.info("1. Vanilla 2. Paper 3. Velocity 4. Forge 5. Fabric 6. NeoForge");
            logger.info(">> ");

            try {
                int choice = input.readInt();
                Installer installer = installerFactory.createInstaller(choice);

                if (installer == null) {
                    logger.warn("{}Некорректный выбор — попробуйте снова{}", RED, RESET);
                    continue;
                }

                installer.init();
                return;
            } catch (NumberFormatException e) {
                logger.warn("Введено нецелое число: {}", e.getMessage());
                logger.error("{}Нужно ввести число от 1 до 6!{}", RED, RESET);
            }
        }
    }
}
