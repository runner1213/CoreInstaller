package org.cats.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;

import static org.cats.Main.scanner;
import static org.cats.util.Colors.*;
import static org.cats.util.Colors.RESET;

public class Eula {
    private static final Logger logger = LogManager.getLogger(Eula.class);
    public static final String EULA_FILE = "eula.txt";
    public static boolean acceptEula() {
        scanner.nextLine();
        while (true) {
            System.out.println();
            System.out.println("Вы должны принять лицензионное соглашение Mojang (EULA), чтобы продолжить установку");
            System.out.println("Полный текст: https://aka.ms/MinecraftEULA");
            System.out.println("Принимаете ли Вы условия EULA? [Д/д/Y/y = Да, Н/н/N/n = Нет]: ");

            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("д") || input.equals("да") || input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("н") || input.equals("нет") || input.equals("n") || input.equals("no")) {
                return false;
            } else {
                System.out.println("Введите корректный ответ: Д[а] или Н[ет] (Y/N)");
            }
        }
    }

    public static void createEulaFile() {
        boolean eula = Eula.acceptEula();
        if (eula) {
            try (FileWriter writer = new FileWriter(EULA_FILE)) {
                writer.write("eula=true\n");
                logger.info("{}Файл " + EULA_FILE + " создан{}", YELLOW, RESET);
            } catch (IOException e) {
                logger.error("{}Ошибка создания EULA: {}{}", RED, e.getMessage(), RESET);
            }
        }
    }
}
