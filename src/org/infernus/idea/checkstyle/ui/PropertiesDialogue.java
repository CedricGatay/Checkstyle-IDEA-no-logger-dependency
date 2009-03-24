package org.infernus.idea.checkstyle.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.IDEAUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Allows setting of file properties.
 */
public class PropertiesDialogue extends JDialog {

    private static final Log LOG = LogFactory.getLog(PropertiesDialogue.class);

    private final PropertiesTableModel propertiesModel
            = new PropertiesTableModel();
    /**
     * Properties table, hacked for enable/disable support.
     */
    private final JTable propertiesTable = new JTable(propertiesModel) {
        public Component prepareRenderer(final TableCellRenderer renderer,
                                         final int row, final int column) {
            final Component comp = super.prepareRenderer(renderer, row, column);
            comp.setEnabled(isEnabled());
            return comp;
        }
    };

    private boolean committed = true;
    private ConfigurationLocation configurationLocation;

    public PropertiesDialogue() {
        initialise();
    }

    public void initialise() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(300, 200));
        setModal(true);

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        propertiesTable.setToolTipText(resources.getString(
                "config.file.properties.tooltip"));

        final JScrollPane propertiesScrollPane = new JScrollPane(propertiesTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        propertiesScrollPane.setBorder(new EmptyBorder(8, 8, 4, 8));
        propertiesScrollPane.setPreferredSize(new Dimension(500, 400));

        final JButton okayButton = new JButton(new OkayAction());
        final JButton cancelButton = new JButton(new CancelAction());

        final JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(new EmptyBorder(4, 8, 8, 8));

        bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        if (IDEAUtilities.isMacOSX()) {
            bottomPanel.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
            bottomPanel.add(okayButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        } else {
            bottomPanel.add(okayButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
            bottomPanel.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        }

        add(propertiesScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okayButton);

        pack();

        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        setLocation((toolkit.getScreenSize().width - getSize().width) / 2,
                (toolkit.getScreenSize().height - getSize().height) / 2);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        if (visible) {
            this.committed = false;
        }
        super.setVisible(visible);
    }

    /**
     * Get the configuration location entered in the dialogue, or null if no valid location was entered.
     *
     * @return the location or null if no valid location entered.
     */
    public ConfigurationLocation getConfigurationLocation() {
        configurationLocation.setProperties(propertiesModel.getProperties());
        return configurationLocation;
    }

    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        this.configurationLocation = configurationLocation;

        // get latest properties from file
        try {
            configurationLocation.resolve();
        } catch (IOException e) {
            LOG.error("Couldn't resolve properties file", e);

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            final String message = resources.getString("config.file.resolve-failed");
            final String formattedMessage = new MessageFormat(message).format(new Object[]{e.getMessage()});
            JOptionPane.showMessageDialog(this, formattedMessage,
                    resources.getString("config.file.error.title"), JOptionPane.ERROR);
        }

        propertiesModel.setProperties(configurationLocation.getProperties());
    }

    /**
     * Was okay clicked?
     *
     * @return true if okay clicked, false if cancelled.
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Respond to an okay action.
     */
    private class OkayAction extends AbstractAction {
        private static final long serialVersionUID = 3800521701284308642L;

        /**
         * Create a new action.
         */
        public OkayAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.file.okay.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.okay.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.okay.tooltip"));
        }

        public void actionPerformed(final ActionEvent event) {
            committed = true;
            setVisible(false);
        }
    }

    /**
     * Respond to a cancel action.
     */
    private class CancelAction extends AbstractAction {
        private static final long serialVersionUID = -994620715558602656L;

        /**
         * Create a new action.
         */
        public CancelAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.file.cancel.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.cancel.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.cancel.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            committed = false;

            setVisible(false);
        }
    }
}
