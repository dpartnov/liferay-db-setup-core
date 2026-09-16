package com.ableneo.liferay.site;

import com.liferay.portal.instance.lifecycle.BasePortalInstanceLifecycleListener;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the demo setup files once the virtual instance is fully started.
 *
 * <p>
 * This is the mechanism the demo uses, because a setup file can touch data owned by modules that
 * are not started yet while the portal boots. Search indexers, the data engine behind web content
 * structures and the service access policy service are all registered late, and a setup that runs
 * too early fails on them. {@code portalInstanceRegistered} is called after the instance is ready,
 * so everything the setup needs is available.
 * </p>
 *
 * <p>
 * The trade off is that it runs on every startup rather than once. The setup files are idempotent,
 * so this only costs a few seconds. When you need the run-once semantics, register the files as
 * upgrade steps instead, see {@link DemoUpgradeStepRegistrator}.
 * </p>
 */
@Component(service = PortalInstanceLifecycleListener.class)
public class DemoPortalInstanceLifecycleListener extends BasePortalInstanceLifecycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(DemoPortalInstanceLifecycleListener.class);

    /**
     * Delays the activation of this component until the portal is initialized. Without it the
     * listener is called while the portal still boots, and the setup fails on services that are
     * registered later, such as the expando permission checks or the layout page template
     * provider.
     */
    @Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED)
    private ModuleServiceLifecycle moduleServiceLifecycle;

    private static final String[] SETUP_FILES = {
        "setup/01-security.xml",
        "setup/02-content.xml",
        "setup/03-pages.xml",
    };

    @Override
    public void portalInstanceRegistered(Company company) throws Exception {
        for (String setupFile : SETUP_FILES) {
            LOG.info("Applying demo setup file {}", setupFile);

            try {
                DbSetupUtil.runDbSetupConfiguration(setupFile);
            } catch (UpgradeException e) {
                LOG.error("Failed to apply demo setup file {}", setupFile, e);
            }
        }
    }
}
