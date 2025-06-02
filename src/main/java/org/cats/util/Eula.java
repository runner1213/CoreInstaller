package org.cats.util;

import java.io.FileWriter;
import java.io.IOException;

import static org.cats.Main.scanner;
import static org.cats.util.Colors.*;
import static org.cats.util.Colors.RESET;

public class Eula {
    public static final String EULA_FILE = "eula.txt";
    public static boolean acceptEula() {
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
                System.out.println(YELLOW + "Файл " + EULA_FILE + " создан" + RESET);
            } catch (IOException e) {
                System.err.println(RED + "Ошибка создания EULA: " + e.getMessage() + RESET);
            }
        }
    }
}
