package org.cats.util;

import org.cats.Installer;
import org.cats.installers.*;

import java.util.HashMap;
import java.util.Map;

public class InstallerFactory {
    private static final Map<Integer, Installer> INSTALLERS = new HashMap<>();

    static {
        INSTALLERS.put(1, new Vanilla());
        INSTALLERS.put(2, new Paper());
        INSTALLERS.put(3, new Velocity());
        INSTALLERS.put(4, new Forge());
        INSTALLERS.put(5, new Fabric());
        INSTALLERS.put(6, new NeoForge());
    }

    public static Installer createInstaller(int choice) {
        return INSTALLERS.get(choice);
    }
}
