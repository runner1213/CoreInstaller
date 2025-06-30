package org.cats.util;

import java.util.Arrays;

public class Colors {
    public static String RESET = "\u001B[0m";
    public static String BLACK = "\u001B[30m";
    public static String RED = "\u001B[31m";
    public static String GREEN = "\u001B[32m";
    public static String YELLOW = "\u001B[33m";
    public static String BLUE = "\u001B[34m";
    public static String PURPLE = "\u001B[35m";
    public static String CYAN = "\u001B[36m";
    public static String WHITE = "\u001B[37m";

    public static String BLACK_BG = "\u001B[40m";
    public static String RED_BG = "\u001B[41m";
    public static String GREEN_BG = "\u001B[42m";
    public static String YELLOW_BG = "\u001B[43m";
    public static String BLUE_BG = "\u001B[44m";
    public static String PURPLE_BG = "\u001B[45m";
    public static String CYAN_BG = "\u001B[46m";
    public static String WHITE_BG = "\u001B[47m";

    public static String BOLD = "\u001B[1m";
    public static String UNDERLINE = "\u001B[4m";

    public static void colorful(boolean answer) {
        if (!answer) {
            RESET = BLACK = RED = GREEN = YELLOW = BLUE = PURPLE = CYAN = WHITE =
                    BLACK_BG = RED_BG = GREEN_BG = YELLOW_BG = BLUE_BG = PURPLE_BG = CYAN_BG = WHITE_BG =
                            BOLD = UNDERLINE = "";
        }
    }
}
