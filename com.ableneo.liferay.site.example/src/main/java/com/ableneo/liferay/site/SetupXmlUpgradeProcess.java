package com.ableneo.liferay.site;

import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

/**
 * Runs a single setup XML file of this bundle as a Liferay upgrade step.
 *
 * <p>
 * Liferay records the schema version a module reached in the {@code Release_} table, so each step
 * runs exactly once per virtual instance. That makes upgrade steps the natural place for setup
 * files: adding a new file with a new version number applies only the new data, while already
 * applied files are skipped.
 * </p>
 *
 * <p>
 * The library also ships {@code BasicSetupUpgradeProcess}, but it resolves the setup file through
 * {@code new File(...)} against its own class loader, which does not work for a file packaged in
 * an OSGi bundle. Inside a bundle read the file as a stream and pass the bundle to
 * {@code LiferaySetup}, which is what {@link DbSetupUtil} does.
 * </p>
 */
public class SetupXmlUpgradeProcess extends UpgradeProcess {

    private final String setupFilePath;

    public SetupXmlUpgradeProcess(String setupFilePath) {
        this.setupFilePath = setupFilePath;
    }

    @Override
    protected void doUpgrade() throws UpgradeException {
        DbSetupUtil.runDbSetupConfiguration(setupFilePath);
    }
}
