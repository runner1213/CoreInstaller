package org.cats.util;

import org.cats.Installer;
import org.cats.installers.Fabric;
import org.cats.installers.Forge;
import org.cats.installers.NeoForge;
import org.cats.installers.Paper;
import org.cats.installers.Vanilla;
import org.cats.installers.Velocity;
import org.cats.io.UserInput;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class InstallerFactory {
    private final Map<Integer, Supplier<Installer>> installers = new HashMap<>();

    public InstallerFactory(UserInput input, Eula eula) {
        installers.put(1, () -> new Vanilla(input, eula));
        installers.put(2, () -> new Paper(input, eula));
        installers.put(3, () -> new Velocity(input, eula));
        installers.put(4, () -> new Forge(eula));
        installers.put(5, () -> new Fabric(input, eula));
        installers.put(6, () -> new NeoForge(input, eula));
    }

    public Installer createInstaller(int choice) {
        Supplier<Installer> installer = installers.get(choice);
        return installer != null ? installer.get() : null;
    }
}
