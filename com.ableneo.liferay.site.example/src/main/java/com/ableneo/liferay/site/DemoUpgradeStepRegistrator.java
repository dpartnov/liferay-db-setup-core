package com.ableneo.liferay.site;

import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import org.osgi.service.component.annotations.Component;

/**
 * Applies the demo setup files as versioned upgrade steps.
 *
 * <p>
 * Each step runs once per virtual instance and Liferay remembers the version reached in the
 * {@code Release_} table. To ship new data, add a setup file and register it under a new version
 * instead of changing a file that has already been released - the already applied steps are not
 * run again.
 * </p>
 *
 * <p>
 * The setup files themselves are idempotent, so re-running them against an existing instance
 * (for example after deleting the {@code Release_} row) updates the data rather than failing.
 * </p>
 *
 * <p>
 * Disabled in this demo. Upgrade steps run while the portal is still starting up, and the demo
 * data needs modules that are registered later, see
 * {@link DemoPortalInstanceLifecycleListener}. Use upgrade steps for data that only depends on
 * the portal core, and enable this component in exchange for the listener.
 * </p>
 */
@Component(enabled = false, service = UpgradeStepRegistrator.class)
public class DemoUpgradeStepRegistrator implements UpgradeStepRegistrator {

    @Override
    public void register(Registry registry) {
        // Do not call registry.registerInitialization() here. It tells Liferay that a module
        // being installed for the first time can jump straight to the final schema version,
        // which is right for a module whose steps only migrate old data, but it would skip the
        // steps below on a fresh instance.

        registry.register("0.0.0", "1.0.0", new SetupXmlUpgradeProcess("setup/01-security.xml"));
        registry.register("1.0.0", "1.1.0", new SetupXmlUpgradeProcess("setup/02-content.xml"));
        registry.register("1.1.0", "1.2.0", new SetupXmlUpgradeProcess("setup/03-pages.xml"));
    }
}
