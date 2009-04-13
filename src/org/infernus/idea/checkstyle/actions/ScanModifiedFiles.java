package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;

import java.util.List;

/**
 * Scan modified files.
 * <p/>
 * If the project is not setup to use VCS then no files will be scanned.
 *
 * @author jgchristopher
 * @version 1.0
 */
public class ScanModifiedFiles extends BaseAction {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            ScanModifiedFiles.class);

    /**
     * {@inheritDoc}
     */
    public final void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            project.getComponent(CheckStylePlugin.class).checkFiles(changeListManager.getAffectedFiles(), event);
        } catch (Throwable e) {
            final CheckStylePluginException processed = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Modified files scan failed", processed);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                return;
            }

            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }

            final Presentation presentation = event.getPresentation();

            // disable if no files are modified
            final List<VirtualFile> modifiedFiles = ChangeListManager.getInstance(project).getAffectedFiles();
            if (modifiedFiles.size() == 0) {
                presentation.setEnabled(false);

            } else {
                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            }

        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Button update failed.", processed);
            }
        }
    }
}
