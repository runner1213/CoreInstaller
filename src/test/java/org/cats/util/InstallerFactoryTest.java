package org.cats.util;

import org.cats.Installer;
import org.cats.installers.Fabric;
import org.cats.installers.Forge;
import org.cats.installers.NeoForge;
import org.cats.installers.Paper;
import org.cats.installers.Vanilla;
import org.cats.installers.Velocity;
import org.cats.io.UserInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class InstallerFactoryTest {
    private final UserInput input = () -> "1";
    private final InstallerFactory factory = new InstallerFactory(input, new Eula(input));

    @Test
    void createInstallerReturnsMatchingInstallerType() {
        assertInstaller(1, Vanilla.class);
        assertInstaller(2, Paper.class);
        assertInstaller(3, Velocity.class);
        assertInstaller(4, Forge.class);
        assertInstaller(5, Fabric.class);
        assertInstaller(6, NeoForge.class);
    }

    @Test
    void createInstallerReturnsNullForUnknownChoice() {
        assertNull(factory.createInstaller(0));
        assertNull(factory.createInstaller(7));
    }

    private void assertInstaller(int choice, Class<? extends Installer> type) {
        assertInstanceOf(type, factory.createInstaller(choice));
    }
}
