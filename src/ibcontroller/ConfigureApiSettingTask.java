package ibcontroller;

import java.awt.Component;
import java.awt.Container;
import javax.swing.*;

class ConfigureApiSettingTask implements Runnable {
    private final boolean readOnlyApi;
    private final boolean bypassOrderPrecautions;
    private final int apiPort;
    private final boolean isGateway;

    ConfigureApiSettingTask(boolean isGateway, int apiPort, boolean readOnlyApi, boolean bypassOrderPrecautions) {
        this.isGateway = isGateway;
        this.apiPort = apiPort;
        this.readOnlyApi = readOnlyApi;
        this.bypassOrderPrecautions = bypassOrderPrecautions;
    }

    @Override
    public void run() {
        try {
            // blocks the thread until the config dialog is available
            final JDialog configDialog = ConfigDialogManager.configDialogManager().getConfigDialog();

            GuiExecutor.instance().execute(new Runnable(){
                @Override
                public void run() {
                    configure(configDialog, apiPort, readOnlyApi, bypassOrderPrecautions);
                }
            });

        } catch (Exception e) {
            Utils.logError("" + e.getMessage());
        }
    }

    private void configure(final JDialog configDialog, final int apiPort, final boolean readOnlyApi, final boolean bypassOrderPrecautions) {
        try {
            Utils.logToConsole("Performing Api setting configuration");

            if (!Utils.selectConfigSection(configDialog, new String[] {"API","Settings"}))
                // older versions of TWS don't have the Settings node below the API node
                Utils.selectConfigSection(configDialog, new String[] {"API"});

            // set API port
            if (apiPort != 0) {
                Component comp = SwingUtils.findComponent(configDialog, "Socket port");
                if (comp == null)
                    throw new IBControllerException("could not find socket port component");

                JTextField tf = SwingUtils.findTextField((Container)comp, 0);
                if (tf == null) throw new IBControllerException("could not find socket port field");

                int currentPort = Integer.parseInt(tf.getText());
                if (currentPort == apiPort) {
                    Utils.logToConsole("TWS API socket port is already set to " + tf.getText());
                } else {
                    if (!this.isGateway) {
                        JCheckBox cb = SwingUtils.findCheckBox(configDialog,
                                "Enable ActiveX and Socket Clients");
                        if (cb == null) {
                            throw new IBControllerException("could not find Enable ActiveX checkbox");
                        }
                        if (cb.isSelected()) {
                            ConfigDialogManager.configDialogManager().setApiConfigChangeConfirmationExpected();
                        }
                    }
                    Utils.logToConsole("TWS API socket port was set to " + tf.getText());
                    tf.setText(new Integer(apiPort).toString());
                    Utils.logToConsole("TWS API socket port now set to " + tf.getText());
                }
            }

            // disable ReadOnly API
            String text = "Read-Only API";
            JCheckBox cb = SwingUtils.findCheckBox(configDialog, text);
            if (cb == null) throw new IBControllerException("could not find " + text + " checkbox");

            if(cb.isSelected() && readOnlyApi) {
                cb.setSelected(true);
                Utils.logToConsole("Select and enable " + text);
            } else {
                cb.setSelected(false);
                Utils.logToConsole("Unselect and disable " + text);
            }

            // disable Order Precautions
            if (!Utils.selectConfigSection(configDialog, new String[]{"API", "Precautions"}))
                // older versions of TWS don't have the Settings node below the API node
                Utils.selectConfigSection(configDialog, new String[] {"API"});

            text = "Bypass Order Precautions for API Orders";
            cb = SwingUtils.findCheckBox(configDialog, text);
            if (cb == null) throw new IBControllerException("could not find " + text + " checkbox");

            if (cb.isSelected() && !bypassOrderPrecautions) {
                cb.setSelected(false);
                Utils.logToConsole("Unselect and disable " + text);
            } else {
                cb.setSelected(false);
                Utils.logToConsole("Select and enable " + text);
            }

            // apply settings and close dialog
            SwingUtils.clickButton(configDialog, "OK");

            configDialog.setVisible(false);
        } catch (IBControllerException e) {
            Utils.logError("" + e.getMessage());
        }
    }
}