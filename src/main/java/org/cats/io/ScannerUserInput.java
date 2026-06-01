package org.cats.io;

import java.util.Scanner;

public final class ScannerUserInput implements UserInput {
    private final Scanner scanner;

    public ScannerUserInput(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public String readLine() {
        return scanner.nextLine();
    }
}
