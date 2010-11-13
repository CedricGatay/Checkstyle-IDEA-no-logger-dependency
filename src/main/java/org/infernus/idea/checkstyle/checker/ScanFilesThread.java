package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.log4j.Logger;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NonNls;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class ScanFilesThread extends AbstractCheckerThread {

    /**
     * Logger for this class.
     */
    @NonNls
    private static final Logger LOG = Logger.getLogger(ScanFilesThread.class);

    /**
     * Scan Files and store results.
     * @param checkStylePlugin reference to the CheckStylePlugin
     * @param vFiles files to scan 
     * @param results Map to store scan results
     */
    public ScanFilesThread(final CheckStylePlugin checkStylePlugin,
                           final List<VirtualFile> vFiles, 
                           final Map<PsiFile, List<ProblemDescriptor>> results) {
        super(checkStylePlugin, vFiles);
        this.setFileResults(results);
    }

    /**
     * Run scan against files.
     */
    public void run() {
        setRunning(true);

        try {
            this.processFilesForModuleInfoAndScan();

        } catch (final Throwable e) {
            final CheckStylePluginException processedError = CheckStylePlugin.processError(
                    "An error occurred during a file scan.", e);

            if (processedError != null) {
                LOG.error("An error occurred while scanning a file.",
                        processedError);
            }
        }
    }


    public void runFileScanner(FileScanner fileScanner) throws InterruptedException, InvocationTargetException {
        fileScanner.run();
    }

}
