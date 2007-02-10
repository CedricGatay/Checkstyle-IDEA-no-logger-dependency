package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.puppycrawl.tools.checkstyle.Checker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Main class for the CheckStyle static scanning plug-n.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CheckStylePlugin implements ProjectComponent, Configurable {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            CheckStylePlugin.class);

    private final Project project;
    private ToolWindow toolWindow;

    private String toolWindowId;

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(final Project project) {
        if (project != null) {
            LOG.info("CheckStyle Plugin loaded with project: \"" + project.getProjectFilePath() + "\"");
        } else {
            LOG.info("CheckStyle Plugin loaded with no project.");
        }

        this.project = project;
    }

    /**
     * Get the ID for the tool window.
     *
     * @return the ID for the tool window.
     */
    public String getToolWindowId() {
        return toolWindowId;
    }

    /**
     * Register the tool window with IDEA.
     */
    private void registerToolWindow() {
        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        toolWindowId = resources.getString("plugin.toolwindow.name");

        toolWindow = toolWindowManager.registerToolWindow(toolWindowId,
                new ToolWindowPanel(project), ToolWindowAnchor.BOTTOM);


        toolWindow.setIcon(IDEAUtilities.getIcon("/debugger/watches.png"));
        toolWindow.setType(ToolWindowType.DOCKED, null);
    }

    /**
     * Un-register the tool window from IDEA.
     */
    private void unregisterToolWindow() {
        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        toolWindowManager.unregisterToolWindow(toolWindowId);
    }

    /**
     * {@inheritDoc}
     */
    public void projectOpened() {
        registerToolWindow();
    }

    /**
     * {@inheritDoc}
     */
    public void projectClosed() {
        unregisterToolWindow();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getComponentName() {
        return CheckStyleConstants.ID_PLUGIN;
    }

    /**
     * {@inheritDoc}
     */
    public void initComponent() {

    }

    /**
     * {@inheritDoc}
     */
    public void disposeComponent() {

    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        return resources.getString("plugin.configuration-name");
    }

    /**
     * {@inheritDoc}
     */
    public Icon getIcon() {
        return IDEAUtilities.getIcon("/general/configurableErrorHighlighting.png");
    }

    /**
     * {@inheritDoc}
     */
    public String getHelpTopic() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JComponent createComponent() {
        // TODO configuration panel
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
        // TODO test if config is modified
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void apply() throws ConfigurationException {
        // TODO apply configuration
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        // TODO reset configuration
    }

    /**
     * {@inheritDoc}
     */
    public void disposeUIResources() {

    }

    /**
     * Produce a CheckStyle checker.
     *
     * @return a checker.
     */
    public Checker getChecker() {
        try {
            final Checker checker;
            final File configFile = null; // TODO load from configuration
            if (configFile == null) {
                final InputStream in = CheckStyleInspection.class.getResourceAsStream(
                        CheckStyleConstants.DEFAULT_CONFIG);
                checker = CheckerFactory.getInstance().getChecker(in);
                in.close();
                
            } else {
                checker = CheckerFactory.getInstance().getChecker(configFile);
            }

            return checker;

        } catch (Exception e) {
            LOG.error("Error", e);
            throw new RuntimeException("Couldn't create Checker", e);
        }
    }

    /**
     * Run a scan on the currently selected file.
     *
     * @param event the event that triggered this action.
     */
    public void checkCurrentFile(final AnActionEvent event) {
        LOG.info("Scanning current file(s).");

        // TODO this picks up the open file. We need to have the intelligence
        // to pick up files selected in the project/package view and, if none,
        // fall back on the current open file.
        final VirtualFile[] selectedFiles
                = FileEditorManager.getInstance(project).getSelectedFiles();
        
        if (selectedFiles == null) {
            LOG.debug("No selected files found.");
            return;
        }

        // build flattened list of selected files
        final List<VirtualFile> fileList = new ArrayList<VirtualFile>();
        for (final VirtualFile element : selectedFiles) {
            fileList.addAll(flattenFiles(element));
        }

        final Map<PsiFile, List<ProblemDescriptor>> fileResults
                = new HashMap<PsiFile, List<ProblemDescriptor>>();

        for (final VirtualFile virtualFile : fileList) {
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            final List<ProblemDescriptor> results = checkPsiFile(psiFile);

            // add results if necessary
            if (results != null && results.size() > 0) {
                // only PsiFiles will have returned results
                fileResults.put(psiFile, results);
            }
        }

        getToolWindowPanel().displayResults(fileResults);
        getToolWindowPanel().expandTree();
    }

    /**
     * Get the tool window panel for result display.
     *
     * @return the tool window panel.
     */
    private ToolWindowPanel getToolWindowPanel() {
        return ((ToolWindowPanel) toolWindow.getComponent());
    }

    /**
     * Scan a PSI file with CheckStyle.
     *
     * @param element the PSI element to scan. This will be ignored if not
     *                a java file.
     * @return a list of tree nodes representing the result tree for this
     *         file, an empty list or null if this file is invalid or has no errors.
     */
    private List<ProblemDescriptor> checkPsiFile(final PsiElement element) {
        if (element == null || !element.isValid() || !element.isPhysical()
                || !PsiFile.class.isAssignableFrom(element.getClass())) {
            return null;
        }

        final PsiFile psiFile = (PsiFile) element;
        LOG.debug("Scanning " + psiFile.getName());

        if (!CheckStyleConstants.FILETYPE_JAVA.equals(psiFile.getFileType())) {
            LOG.debug(psiFile.getName() + " is not a Java file.");
            return null;
        }

        File tempFile = null;
        try {
            final Checker checker = getChecker();

            // we need to copy to a file as IntelliJ may not have saved the file recently...
            tempFile = File.createTempFile(CheckStyleConstants.TEMPFILE_NAME,
                    CheckStyleConstants.TEMPFILE_EXTENSION);
            final BufferedWriter tempFileOut = new BufferedWriter(
                    new FileWriter(tempFile));
            tempFileOut.write(psiFile.getText());
            tempFileOut.flush();
            tempFileOut.close();

            final InspectionManager manager
                    = InspectionManager.getInstance(psiFile.getProject());

            final CheckStyleAuditListener listener
                    = new CheckStyleAuditListener(psiFile, manager, true);
            checker.addListener(listener);
            checker.process(new File[]{tempFile});
            checker.destroy();

            return listener.getProblems();

        } catch (IOException e) {
            LOG.error("Failure when creating temp file", e);
            throw new RuntimeException("Couldn't create temp file", e);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Flatten the tree structure represented by a virtual file.
     *
     * @param file the tree to flatten.
     * @return a list of flattened files.
     */
    private List<VirtualFile> flattenFiles(final VirtualFile file) {
        final List<VirtualFile> elementList = new ArrayList<VirtualFile>();
        elementList.add(file);

        if (file.getChildren() != null) {
            for (final VirtualFile childFile : file.getChildren()) {
                elementList.addAll(flattenFiles(childFile));
            }
        }

        return elementList;
    }


    /**
     * Run a scan on all files in the current module.
     *
     * @param event the event that triggered this action.
     */
    public void checkCurrentModuleFiles(final AnActionEvent event) {
        LOG.info("Scanning current module.");
        // TODO
    }


    /**
     * Run a scan on all project files.
     *
     * @param event the event that triggered this action.
     */
    public void checkProjectFiles(final AnActionEvent event) {
        LOG.info("Scanning current project.");
        // TODO
    }

}
