package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.util.ExtendedProblemDescriptor;
import org.infernus.idea.checkstyle.util.IDEAUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The tool window for CheckStyle scans.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ToolWindowPanel extends JPanel {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(ToolWindowPanel.class);

    private static final String MAIN_ACTION_GROUP = "CheckStylePluginActions";
    private static final String TREE_ACTION_GROUP = "CheckStylePluginTreeActions";

    private static final Map<Pattern, String> CHECKSTYLE_ERROR_PATTERNS
            = new HashMap<Pattern, String>();

    private final Project project;
    private final JTree resultsTree;
    private final JToolBar progressPanel;
    private final JProgressBar progressBar;
    private final JLabel progressLabel;

    private boolean displayingErrors = true;
    private boolean displayingWarnings = true;
    private boolean displayingInfo = true;

    private ResultTreeModel treeModel;
    private boolean scrollToSource;

    static {
        try {
            CHECKSTYLE_ERROR_PATTERNS.put(
                    Pattern.compile("Property \\$\\{([^\\}]*)\\} has not been set"),
                    "plugin.results.error.missing-property");
            CHECKSTYLE_ERROR_PATTERNS.put(
                    Pattern.compile("Unable to instantiate (.*)"),
                    "plugin.results.error.instantiation-failed");

        } catch (Throwable t) {
            LOG.error("Pattern mappings could not be instantiated.", t);
        }
    }

    /**
     * Create a tool window for the given project.
     *
     * @param project the project.
     */
    public ToolWindowPanel(final Project project) {
        super(new BorderLayout());

        this.project = project;

        setBorder(new EmptyBorder(1, 1, 1, 1));

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        // Create the toolbar
        final ActionGroup mainActionGroup = (ActionGroup)
                ActionManager.getInstance().getAction(MAIN_ACTION_GROUP);
        final ActionToolbar mainToolbar = ActionManager.getInstance().createActionToolbar(
                CheckStyleConstants.ID_TOOLWINDOW, mainActionGroup, false);

        final ActionGroup treeActionGroup = (ActionGroup)
                ActionManager.getInstance().getAction(TREE_ACTION_GROUP);
        final ActionToolbar treeToolbar = ActionManager.getInstance().createActionToolbar(
                CheckStyleConstants.ID_TOOLWINDOW, treeActionGroup, false);

        final Box toolBarBox = Box.createHorizontalBox();
        toolBarBox.add(mainToolbar.getComponent());
        toolBarBox.add(treeToolbar.getComponent());

        add(toolBarBox, BorderLayout.WEST);

        // Create the tree
        treeModel = new ResultTreeModel();

        resultsTree = new JTree(treeModel);
        resultsTree.setRootVisible(false);

        final TreeSelectionListener treeSelectionListener = new ToolWindowSelectionListener();
        resultsTree.addTreeSelectionListener(treeSelectionListener);
        final MouseListener treeMouseListener = new ToolWindowMouseListener();
        resultsTree.addMouseListener(treeMouseListener);
        resultsTree.setCellRenderer(new ResultTreeRenderer());

        progressLabel = new JLabel(" ");
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        progressBar.setMinimum(0);
        final Dimension progressBarSize = new Dimension(
                100, progressBar.getPreferredSize().height);
        progressBar.setMinimumSize(progressBarSize);
        progressBar.setPreferredSize(progressBarSize);
        progressBar.setMaximumSize(progressBarSize);

        progressPanel = new JToolBar(JToolBar.HORIZONTAL);
        progressPanel.add(Box.createHorizontalStrut(4));
        progressPanel.add(progressLabel);
        progressPanel.add(Box.createHorizontalGlue());
        progressPanel.setFloatable(false);
        progressPanel.setBackground(UIManager.getColor("Panel.background"));
        progressPanel.setBorder(null);

        final JPanel toolPanel = new JPanel(new BorderLayout());

        toolPanel.add(new JScrollPane(resultsTree), BorderLayout.CENTER);
        toolPanel.add(progressPanel, BorderLayout.NORTH);

        add(toolPanel, BorderLayout.CENTER);

        expandTree();

        ToolTipManager.sharedInstance().registerComponent(resultsTree);
        mainToolbar.getComponent().setVisible(true);
    }

    /**
     * Update the progress text.
     *
     * @param text the new progress text, or null to clear.
     */
    public void setProgressText(final String text) {
        if (text == null || text.trim().length() == 0) {
            progressLabel.setText(" ");
        } else {
            progressLabel.setText(text);
        }
        progressLabel.validate();
    }

    /**
     * Show and reset the progress bar.
     */
    public void resetProgressBar() {
        progressBar.setValue(0);

        // show if necessary
        if (progressPanel.getComponentIndex(progressBar) == -1) {
            progressPanel.add(progressBar);
        }

        progressPanel.revalidate();
    }

    /**
     * Set the maxium limit, then show and reset the progress bar.
     *
     * @param max the maximum limit of the progress bar.
     */
    public void setProgressBarMax(final int max) {
        progressBar.setMaximum(max);

        resetProgressBar();
    }

    /**
     * Increment the progress of the progress bar.
     * <p/>
     * You should call {@link #setProgressBarMax(int)} first for useful semantics.
     */
    public void incrementProgressBar() {
        if (progressBar.getValue() < progressBar.getMaximum()) {

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressBar.setValue(progressBar.getValue() + 1);
                }
            });
        }
    }

    /**
     * Increment the progress of the progress bar by a given number.
     * <p/>
     * You should call {@link #setProgressBarMax(int)} first for useful semantics.
     */
    public void incrementProgressBarBy( final int size) {
        if (progressBar.getValue() < progressBar.getMaximum()) {

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressBar.setValue(progressBar.getValue() + size);
                }
            });
        }
    }

    /**
     * Hides the progress bar.
     */
    public void clearProgressBar() {
        final int progressIndex = progressPanel.getComponentIndex(progressBar);
        if (progressIndex != -1) {
            progressPanel.remove(progressIndex);
            progressPanel.revalidate();
            progressPanel.repaint();
        }
    }

    /**
     * Scroll to the error specified by the given tree path, or do nothing
     * if no error is specified.
     *
     * @param treePath the tree path to scroll to.
     */
    private void scrollToError(final TreePath treePath) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (treeNode == null || !(treeNode.getUserObject() instanceof ResultTreeNode)) {
            return;
        }

        final ResultTreeNode nodeInfo = (ResultTreeNode) treeNode.getUserObject();
        if (nodeInfo.getFile() == null || nodeInfo.getProblem() == null) {
            return; // no problem here :-)
        }

        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final FileEditor[] editor = fileEditorManager.openFile(
                nodeInfo.getFile().getVirtualFile(), true);

        if (editor != null && editor.length > 0 && editor[0] instanceof TextEditor) {
            final int column = nodeInfo.getProblem() instanceof ExtendedProblemDescriptor
                    ? ((ExtendedProblemDescriptor) nodeInfo.getProblem()).getColumn() : 0;
            final int line = nodeInfo.getProblem() instanceof ExtendedProblemDescriptor
                    ? ((ExtendedProblemDescriptor) nodeInfo.getProblem()).getLine()
                    : nodeInfo.getProblem().getLineNumber();
            final LogicalPosition problemPos = new LogicalPosition(
                    line - 1, column);

            ((TextEditor) editor[0]).getEditor().getCaretModel().moveToLogicalPosition(problemPos);
            ((TextEditor) editor[0]).getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    /**
     * Should we scroll to the selected error in the editor automatically?
     *
     * @param scrollToSource true if the error should be scrolled to automatically.
     */
    public void setScrollToSource(final boolean scrollToSource) {
        this.scrollToSource = scrollToSource;
    }

    /**
     * Should we scroll to the selected error in the editor automatically?
     *
     * @return true if the error should be scrolled to automatically.
     */
    public boolean isScrollToSource() {
        return scrollToSource;
    }


    /**
     * Listen for clicks and scroll to the error's source as necessary.
     */
    protected class ToolWindowMouseListener extends MouseAdapter {

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (!scrollToSource && e.getClickCount() < 2) {
                return;
            }

            final TreePath treePath = resultsTree.getPathForLocation(
                    e.getX(), e.getY());

            if (treePath != null) {
                scrollToError(treePath);
            }
        }

    }

    /**
     * Listen for tree selection events and scroll to the error's source as necessary.
     */
    protected class ToolWindowSelectionListener implements TreeSelectionListener {

        /**
         * {@inheritDoc}
         */
        public void valueChanged(final TreeSelectionEvent e) {
            if (!scrollToSource) {
                return;
            }

            if (e.getPath() != null) {
                scrollToError(e.getPath());
            }
        }

    }

    /**
     * Collapse the tree so that only the root node is visible.
     */
    public void collapseTree() {
        for (int i = 1; i < resultsTree.getRowCount(); ++i) {
            resultsTree.collapseRow(i);
        }
    }

    /**
     * Expand the error tree to the fullest.
     */
    public void expandTree() {
        expandTree(3);
    }

    /**
     * Expand the given tree to the given level, starting from the given node
     * and path.
     *
     * @param tree  The tree to be expanded
     * @param node  The node to start from
     * @param path  The path to start from
     * @param level The number of levels to expand to
     */
    private static void expandNode(final JTree tree,
                                   final TreeNode node,
                                   final TreePath path,
                                   final int level) {
        if (level <= 0) {
            return;
        }

        tree.expandPath(path);

        for (int i = 0; i < node.getChildCount(); ++i) {
            final TreeNode childNode = node.getChildAt(i);
            expandNode(tree, childNode, path.pathByAddingChild(childNode), level - 1);
        }
    }

    /**
     * Expand the error tree to the given level.
     *
     * @param level The level to expand to
     */
    private void expandTree(final int level) {
        expandNode(resultsTree, treeModel.getVisibleRoot(),
                new TreePath(treeModel.getPathToRoot(treeModel.getVisibleRoot())), level);
    }

    /**
     * Clear the results and display a 'scan in progress' notice.
     */
    public void displayInProgress() {
        treeModel.clear();
        treeModel.setRootMessage("plugin.results.in-progress");
    }

    /**
     * Clear the results and display notice to say an error occurred.
     *
     * @param error the error that occurred.
     */
    public void displayErrorResult(final Throwable error) {
        // match some friendly error messages.
        String errorText = null;
        if (error.getCause() != null
                && error.getCause() instanceof CheckstyleException) {

            for (final Pattern errorPattern
                    : CHECKSTYLE_ERROR_PATTERNS.keySet()) {
                final Matcher errorMatcher
                        = errorPattern.matcher(error.getCause().getMessage());
                if (errorMatcher.find()) {
                    final Object[] args = new Object[errorMatcher.groupCount()];

                    for (int i = 0; i < errorMatcher.groupCount(); ++i) {
                        args[i] = errorMatcher.group(i + 1);
                    }

                    errorText = new MessageFormat(IDEAUtilities.getResource(
                            CHECKSTYLE_ERROR_PATTERNS.get(errorPattern),
                            "An error occurred during the scan.")).format(args);
                }
            }
        }

        if (errorText == null) {
            errorText = IDEAUtilities.getResource("plugin.results.error",
                    "An error occurred during the scan.");
        }

        treeModel.clear();
        treeModel.setRootText(errorText);
    }

    private SeverityLevel[] getDisplayedSeverities() {
        final List<SeverityLevel> severityLevels = new ArrayList<SeverityLevel>();

        if (displayingErrors) {
            severityLevels.add(SeverityLevel.ERROR);
        }

        if (displayingWarnings) {
            severityLevels.add(SeverityLevel.WARNING);
        }

        if (displayingInfo) {
            severityLevels.add(SeverityLevel.INFO);
        }

        return severityLevels.toArray(new SeverityLevel[severityLevels.size()]);
    }

    /**
     * Refresh the displayed results based on the current filter settings.
     */
    public void filterDisplayedResults() {
        // TODO be a little nicer here, maintain display state

        treeModel.filter(getDisplayedSeverities());
    }

    /**
     * Display the passed results.
     *
     * @param results the map of checked files to problem descriptors.
     */
    public void displayResults(final Map<PsiFile, List<ProblemDescriptor>> results) {
        treeModel.setModel(results, getDisplayedSeverities());

        invalidate();
        repaint();
    }

    public boolean isDisplayingErrors() {
        return displayingErrors;
    }

    public void setDisplayingErrors(final boolean displayingErrors) {
        this.displayingErrors = displayingErrors;
    }

    public boolean isDisplayingWarnings() {
        return displayingWarnings;
    }

    public void setDisplayingWarnings(final boolean displayingWarnings) {
        this.displayingWarnings = displayingWarnings;
    }

    public boolean isDisplayingInfo() {
        return displayingInfo;
    }

    public void setDisplayingInfo(final boolean displayingInfo) {
        this.displayingInfo = displayingInfo;
    }
}
