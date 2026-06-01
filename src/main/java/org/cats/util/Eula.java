package org.cats.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cats.io.UserInput;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import static org.cats.util.Colors.RED;
import static org.cats.util.Colors.RESET;
import static org.cats.util.Colors.YELLOW;

public class Eula {
    private static final Logger logger = LogManager.getLogger(Eula.class);
    public static final String EULA_FILE = "eula.txt";

    private final UserInput input;

    public Eula(UserInput input) {
        this.input = input;
    }

    public boolean acceptEula() {
        while (true) {
            System.out.println();
            logger.info("Вы должны принять лицензионное соглашение Mojang (EULA), чтобы продолжить установку");
            logger.info("Полный текст: https://aka.ms/MinecraftEULA");
            logger.info("Принимаете ли Вы условия EULA? [Д/д/Y/y = Да, Н/н/N/n = Нет]: ");

            String answer = input.readLine().trim().toLowerCase(Locale.ROOT);

            if (answer.equals("д") || answer.equals("да") || answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("н") || answer.equals("нет") || answer.equals("n") || answer.equals("no")) {
                return false;
            }

            System.out.println("Введите корректный ответ: Д[а] или Н[ет] (Y/N)");
        }
    }

    public void createEulaFile() {
        if (!acceptEula()) {
            return;
        }

        try (FileWriter writer = new FileWriter(EULA_FILE)) {
            writer.write("eula=true\n");
            logger.info("{}Файл " + EULA_FILE + " создан{}", YELLOW, RESET);
        } catch (IOException e) {
            logger.error("{}Ошибка создания EULA: {}{}", RED, e.getMessage(), RESET);
        }
    }
}
