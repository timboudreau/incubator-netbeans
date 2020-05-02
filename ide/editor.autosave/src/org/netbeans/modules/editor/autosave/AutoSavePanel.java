/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.editor.autosave;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.WeakReference;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SpinnerNumberModel;
import org.netbeans.modules.editor.autosave.command.AutoSaveController;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

final class AutoSavePanel extends javax.swing.JPanel {

    AutoSavePanel(final AutoSaveOptionsPanelController controller) {
        spnModel = new SpinnerNumberModel(10, 0, 999, 1);

        initComponents();

        AutoSaveController.prefs().
                addPreferenceChangeListener(new WeakReference<PreferenceChangeListener>(evt -> {
                    controller.update();
                    controller.changed();
                }).get());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        chkUseFeature = new JCheckBox();
        chkSaveOnFocusLost = new JCheckBox();
        jLabel1 = new JLabel();
        spnMinutes = new JSpinner();
        jLabel2 = new JLabel();

        Mnemonics.setLocalizedText(chkUseFeature, NbBundle.getMessage(AutoSavePanel.class, "AutoSavePanel.chkUseFeature.text")); // NOI18N
        chkUseFeature.setActionCommand(NbBundle.getMessage(AutoSavePanel.class, "AutoSavePanel.chkUseFeature.actionCommand")); // NOI18N
        chkUseFeature.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        chkUseFeature.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                chkUseFeatureItemStateChanged(evt);
            }
        });

        Mnemonics.setLocalizedText(chkSaveOnFocusLost, NbBundle.getMessage(AutoSavePanel.class,"AutoSavePanel.chkSaveOnFocusLost.text")); // NOI18N

        Mnemonics.setLocalizedText(jLabel1, NbBundle.getMessage(AutoSavePanel.class, "AutoSavePanel.jLabel1.text")); // NOI18N

        spnMinutes.setModel(this.spnModel);
        spnMinutes.setToolTipText(NbBundle.getMessage(AutoSavePanel.class, "AutoSavePanel.spnMinutes.toolTipText")); // NOI18N

        Mnemonics.setLocalizedText(jLabel2, NbBundle.getMessage(AutoSavePanel.class, "AutoSavePanel.jLabel2.text")); // NOI18N

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(spnMinutes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jLabel2))
                            .addComponent(chkSaveOnFocusLost)))
                    .addComponent(chkUseFeature))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkUseFeature)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(chkSaveOnFocusLost)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(spnMinutes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chkUseFeatureItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkUseFeatureItemStateChanged
        this.chkSaveOnFocusLost.setEnabled(this.chkUseFeature.isSelected());
        this.spnMinutes.setEnabled(this.chkUseFeature.isSelected());
    }//GEN-LAST:event_chkUseFeatureItemStateChanged

    void load() {
        chkUseFeature.setSelected(AutoSaveController.prefs().getBoolean(AutoSaveController.KEY_ACTIVE,
                AutoSaveController.KEY_ACTIVE_DEFAULT));
        chkSaveOnFocusLost.setSelected(AutoSaveController.prefs().getBoolean(AutoSaveController.KEY_SAVE_ON_FOCUS_LOST,
                false));
        spnModel.setValue(AutoSaveController.prefs().getInt(AutoSaveController.KEY_INTERVAL, 10));
    }

    void store() {
        AutoSaveController.prefs().putBoolean(AutoSaveController.KEY_ACTIVE, chkUseFeature.isSelected());
        AutoSaveController.prefs().
                putBoolean(AutoSaveController.KEY_SAVE_ON_FOCUS_LOST, chkSaveOnFocusLost.isSelected());
        AutoSaveController.prefs().putInt(AutoSaveController.KEY_INTERVAL, spnModel.getNumber().intValue());
    }

    boolean valid() {
        return true;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JCheckBox chkSaveOnFocusLost;
    private JCheckBox chkUseFeature;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JSpinner spnMinutes;
    // End of variables declaration//GEN-END:variables
   private SpinnerNumberModel spnModel;
}
