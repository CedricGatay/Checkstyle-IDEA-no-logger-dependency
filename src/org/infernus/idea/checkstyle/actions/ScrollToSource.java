package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;

/**
 * Toggle the scroll to source setting.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class ScrollToSource extends ToggleAction {

    /**
     * {@inheritDoc}
     */
    public boolean isSelected(final AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return false;
        }

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(CheckStyleConstants.ID_TOOLWINDOW);

        // toggle value
        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null) {
            final ToolWindowPanel panel = (ToolWindowPanel) content.getComponent();
            return panel.isScrollToSource();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setSelected(final AnActionEvent event, final boolean selected) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return;
        }

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(CheckStyleConstants.ID_TOOLWINDOW);

        // toggle value
        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null) {
            final ToolWindowPanel panel = (ToolWindowPanel) content.getComponent();
            panel.setScrollToSource(selected);
        }
    }
}
