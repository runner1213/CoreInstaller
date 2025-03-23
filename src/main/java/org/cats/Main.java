package org.cats;

import org.cats.installers.Forge;

import java.util.Scanner;

import static org.cats.installers.Forge.installForge;
import static org.cats.installers.Mohist.MohistInstaller;
import static org.cats.installers.Paper.paperinstall;
import static org.cats.installers.Vanilla.VanillaInstaller;
import static org.cats.util.colors.*;

public class Main {
    public static void main(String[] args) {
        application();
    }
    protected static void application() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Выберите ядро для установки:");
        System.out.println("1. Vanilla 2. Paper 3. Forge 4. NeoForge 5. Mohist");
        switch (scanner.nextInt()) {
            case 1 -> VanillaInstaller();
            case 2 -> paperinstall();
            case 3 -> installForge();
            case 4 -> System.out.println("Скоро...");
            case 5 -> MohistInstaller();
            default -> {
                System.out.println(RED + "Упс.. Ты ввёл что-то не то" + RESET);
                System.out.println("Попробуй снова :3");
                application();
            }
        }
    }
}