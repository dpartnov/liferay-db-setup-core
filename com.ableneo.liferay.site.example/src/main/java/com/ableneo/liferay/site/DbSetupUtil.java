package com.ableneo.liferay.site;

import com.ableneo.liferay.portal.setup.LiferaySetup;
import com.ableneo.liferay.portal.setup.MarshallUtil;
import com.ableneo.liferay.portal.setup.domain.Setup;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Loads a setup XML file of this bundle and applies it.
 */
public final class DbSetupUtil {

    private DbSetupUtil() {}

    public static void runDbSetupConfiguration(String dbSetupConfigurationFilePath) throws UpgradeException {
        Bundle bundle = FrameworkUtil.getBundle(DbSetupUtil.class);
        boolean setupSuccess;

        try (InputStream setupFile = getInputStream(bundle, dbSetupConfigurationFilePath)) {
            Setup setup = MarshallUtil.unmarshall(setupFile);

            // Passing the bundle lets the library resolve every "path" attribute of the setup
            // file as an entry of this bundle.
            setupSuccess = LiferaySetup.setup(setup, bundle);
        } catch (Exception e) {
            throw new UpgradeException(
                String.format("Failed to load or process file: %1$s", dbSetupConfigurationFilePath),
                e
            );
        }

        if (!setupSuccess) {
            throw new UpgradeException(
                String.format(
                    "Failed to apply %1$s, review the errors logged above, fix the data and try again.",
                    dbSetupConfigurationFilePath
                )
            );
        }
    }

    /**
     * Reads the file as a bundle entry. The thread context class loader is not the class loader
     * of this bundle while an upgrade step runs, so it cannot be used here.
     */
    private static InputStream getInputStream(Bundle bundle, String path) throws IOException {
        URL entry = bundle.getEntry(path);

        if (entry == null) {
            throw new IOException(
                String.format("File %1$s not found in bundle %2$s", path, bundle.getSymbolicName())
            );
        }

        return entry.openStream();
    }
}
