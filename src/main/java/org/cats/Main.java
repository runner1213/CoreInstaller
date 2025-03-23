package org.cats;

import java.util.Scanner;

import static org.cats.Forge.installForge;
import static org.cats.Mohist.MohistInstaller;
import static org.cats.Paper.getLatestBuild;
import static org.cats.Paper.paperinstall;
import static org.cats.Vanilla.VanillaInstaller;
import static org.cats.util.colors.*;

public class Main {
    public static void main(String[] args) {
        application();
    }
    protected static void application() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Выберите ядро для установки:");
        System.out.println("1. Vanilla 2. Paper 3. Forge 4. NeoForge 5. Mohist");
        int selection = scanner.nextInt();
        if (selection == 1) { // vanilla
            VanillaInstaller();
        }
        else if (selection == 2) { // paper
            System.out.println("Введите версию:");
            String version = scanner.next();
            String build = String.valueOf(getLatestBuild(version));
            paperinstall(version, Integer.parseInt(build));
        }
        else if (selection == 3) { // Forge
            installForge();
        }
        else if (selection == 4) { // NeoForge
            System.out.println("Скоро будет :3");
        }
        else if (selection == 5) { // Mohist
            MohistInstaller();
        }
        else {
            System.out.println(RED + "Упс.. Ты ввёл что-то не то" + RESET);
            System.out.println("Попробуй снова :3");
            application();
        }
    }
}