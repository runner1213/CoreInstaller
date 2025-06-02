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
        logger.info(GREEN + "Успешная инициализация" + RESET);
        System.out.println("Выберите ядро для установки:");
        System.out.println("1. Vanilla 2. Paper 3. Velocity 4. Forge 5. Fabric 6. NeoForge");
        try {
            switch (scanner.nextInt()) {
                case 1 -> VanillaInstaller();
                case 2 -> paperinstall();
                case 3 -> installVelocity();
                case 4 -> installForge();
                case 5 -> installFabric();
                case 6 -> installNeoForge();
                default -> {
                    System.out.println(RED + "Упс.. Ты ввёл что-то не то" + RESET);
                    System.out.println("Попробуй снова :3");
                    application();
                }
            }
        } catch (InputMismatchException e) {
            logger.warn("Введено нецелое число: {}", e.getMessage());
            System.out.println(RED + "Нужно ввести число от 1 до 7!" + RESET);
            scanner.nextLine();
            application();
        }
    }
}