package com.ableneo.liferay.site;

import com.liferay.portal.kernel.upgrade.UpgradeException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * The simpler alternative to {@link DemoUpgradeStepRegistrator}: run the setup on every bundle
 * activation.
 *
 * <p>
 * Disabled by default so that the demo does not apply its data twice. Useful while developing a
 * setup file, because redeploying the bundle re-applies it without having to touch the
 * {@code Release_} table. Enable it by setting {@code enabled = true} below, and disable the
 * upgrade step registrator in exchange.
 * </p>
 */
@Component(enabled = false, immediate = true, service = Activator.class)
public class Activator {

    @Activate
    public void activate() {
        try {
            DbSetupUtil.runDbSetupConfiguration("setup/01-security.xml");
            DbSetupUtil.runDbSetupConfiguration("setup/02-content.xml");
            DbSetupUtil.runDbSetupConfiguration("setup/03-pages.xml");
        } catch (UpgradeException e) {
            throw new IllegalStateException("Failed to apply the demo setup files", e);
        }
    }
}
