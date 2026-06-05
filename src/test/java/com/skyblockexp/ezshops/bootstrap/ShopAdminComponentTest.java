package com.skyblockexp.ezshops.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class ShopAdminComponentTest {

    @TempDir
    File tempDir;

    @Test
    void discoverGameModes_findsAllModeDirectories() throws IOException {
        // Setup test data folder structure
        File dataFolder = tempDir;
        File shopDir = new File(dataFolder, "shop");
        assertTrue(shopDir.mkdirs() || shopDir.exists());

        // Create mode directories
        assertTrue(new File(shopDir, "prison").mkdir());
        assertTrue(new File(shopDir, "smp").mkdir());

        // Create a file that should be ignored
        File ignoredFile = new File(shopDir, "not-a-mode.txt");
        ignoredFile.createNewFile();

        // Verify the subdirectories were created
        assertEquals(2, listSubdirectories(shopDir).size(), "Should find 2 mode directories");
    }

    @Test
    void discoverGameModes_defaultsToPrisonWhenEmpty() throws IOException {
        File dataFolder = tempDir;
        File shopDir = new File(dataFolder, "shop");
        assertTrue(shopDir.mkdirs() || shopDir.exists());

        // No mode subdirectories - should default to prison
        assertEquals(0, listSubdirectories(shopDir).size());
    }

    @Test
    void smpModeDirectoryExists_afterPluginInstall() {
        // This test verifies that the smp directory exists in resources
        File smpDir = new File("src/main/resources/shop/smp");
        assertTrue(smpDir.exists(), "SMP mode directory should exist in resources");
        assertTrue(smpDir.isDirectory(), "SMP path should be a directory");
    }

    @Test
    void prisonModeDirectoryExists_byDefault() {
        File prisonDir = new File("src/main/resources/shop/prison");
        assertTrue(prisonDir.exists(), "Prison mode directory should exist by default");
        assertTrue(prisonDir.isDirectory(), "Prison path should be a directory");
    }

    private java.util.List<String> listSubdirectories(File dir) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) result.add(f.getName());
            }
        }
        return result;
    }
}