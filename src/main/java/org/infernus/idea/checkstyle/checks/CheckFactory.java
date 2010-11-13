package org.infernus.idea.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for producing various check modifications.
 */
public final class CheckFactory {

    private static final Logger LOG = Logger.getLogger(CheckFactory.class);

    private static final Class[] CHECK_CLASSES = {JavadocPackageCheck.class, PackageHtmlCheck.class};

    public static List<Check> getChecks(final Configuration config) {
        final List<Check> checks = new ArrayList<Check>();

        for (final Class checkClass : CHECK_CLASSES) {
            try {
                final Check check = (Check) checkClass.newInstance();
                check.configure(config);
                checks.add(check);

            } catch (Exception e) {
                LOG.error("Couldn't instantiate check class " + checkClass, e);
            }
        }

        return checks;
    }

}
