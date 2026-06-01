package org.cats.io;

public interface UserInput {
    String readLine();

    default int readInt() {
        return Integer.parseInt(readLine().trim());
    }
}
