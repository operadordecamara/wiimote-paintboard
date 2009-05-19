/*
 * Copyright (C) 2008-2009, Uwe Schmidt
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE. 
 * 
 * The Software uses a third-party library (WiiRemoteJ) which is not part of
 * the Software and is subject to its own license.
 */

package org.uweschmidt.wiimote.whiteboard.gui;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.mouse.DefaultControlStrategy;
import org.uweschmidt.wiimote.whiteboard.mouse.TouchpadControlStrategy;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.AdaptiveExponentialSmoothing;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.NoSmoothing;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.SimpleMovingAverage;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences.LocaleWrapper;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class PreferencesWindow extends JDialog {
	
	private final static WWPreferences prefs = WWPreferences.getPreferences();
	private final static ResourceMap r = Util.getResourceMap(PreferencesWindow.class);
	
	private JSlider delaySlider;
	private JTextField tuioHost;
	private JButton defaultsButton;
	private JCheckBox batteryWarning, checkForUpdates, tuioEnable, mouseSmoothing, touchpadMode, rightClicks, assistDoubleClicks;
	private JRadioButton staticSmoothing, adaptiveSmoothing;
//	private JRadioButton[] wiimoteNumberButtons = new JRadioButton[WWPreferences.WIIMOTES];
	private JComboBox languages;
	private JTabbedPane tabbedPane;
	private boolean donePack = false;
	
	public PreferencesWindow(final MainPanel mp, final HelpHandler hh) {
		super(Application.getInstance(WiimoteWhiteboard.class).getMainFrame(), r.getString("preferences.Action.text"));

		// use NSBox-like TitledBorder if available (Leopard)
		Border aquaBorder = UIManager.getBorder("TitledBorder.aquaVariant");
		if (aquaBorder != null) UIManager.put("TitledBorder.border", aquaBorder);
		
		// create panels
		tabbedPane = new JTabbedPane();
		JPanel generalPanel = new JPanel(new MigLayout(Util.MAC_OS_X ? "insets para-10 para para para" : ""));
		// FIXME bottom insets too large, but only in French on Leopard?!
		JPanel mousePanel = new JPanel(new MigLayout(Util.MAC_OS_X ? String.format("insets para-10 para %s para", Util.MAC_OS_X_LEOPARD_OR_HIGHER ? "para-10" : "para") : ""));
		JPanel tuioPanel = new JPanel(new MigLayout(Util.MAC_OS_X ? "insets para-10 para para para" : ""));
		generalPanel.setOpaque(false);
		mousePanel.setOpaque(false);
		tuioPanel.setOpaque(false);		
		tabbedPane.addTab(r.getString("generalTab"), generalPanel);
		tabbedPane.addTab(r.getString("mouseTab"), mousePanel);
		tabbedPane.addTab(r.getString("tuioTab"), tuioPanel);
		
		((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
		add(tabbedPane, BorderLayout.CENTER);
		
		/*
		 * GENERAL PANEL
		 */
		
		// check for updates
		generalPanel.add(checkForUpdates = Util.newComponent(JCheckBox.class, "checkForUpdates"), "wrap");
		checkForUpdates.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				prefs.setCheckForUpdates(checkForUpdates.isSelected());
			}
		});

		// low battery warning
		generalPanel.add(batteryWarning = Util.newComponent(JCheckBox.class, "batteryWarning"), "wrap");
		batteryWarning.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				prefs.setLowBatteryWarning(batteryWarning.isSelected());
			}
		});
		
		generalPanel.add(new JSeparator(), "split, span, pushx, growx, wrap");

		generalPanel.add(Util.newComponent(JLabel.class, "language"), "split, gapbottom 3, gapleft 6");
		generalPanel.add(languages = Util.newComponent(JComboBox.class, "languageBox"), "wrap");
//		languages.putClientProperty("JComboBox.isSquare", Boolean.TRUE);
//		languages.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
		languages.setModel(new DefaultComboBoxModel(WWPreferences.LANGUAGES));
		languages.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					prefs.setLanguage(((LocaleWrapper)languages.getSelectedItem()).getLocaleString());
				}
			}
		});
		generalPanel.add(Util.newComponent(JLabel.class, "languageRestartLabel"), "split, span, w 225, gapleft 6");
		
//		// number of wiimotes
//		generalPanel.add(Util.newComponent(JLabel.class, "numberOfWiimotesLabel"));
//		ButtonGroup bg = new ButtonGroup();
//		for (int i = 1; i <= WWPreferences.WIIMOTES; i++) {
//			final int j = i-1;
//			bg.add(wiimoteNumberButtons[j] = new JRadioButton(String.valueOf(i)));
//			wiimoteNumberButtons[j].setOpaque(false);
//			wiimoteNumberButtons[j].addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					prefs.setNumberOfWiimotes(j+1);
//				}
//			});
//			generalPanel.add(wiimoteNumberButtons[j], i == 1 ? "split" : "");
//		}	
//		generalPanel.add(Util.newComponent(JLabel.class, "numberOfWiimotesExplLabel"), "newline, split, span, center, w 290");
		
		/*
		 * MOUSE CONTROL PANEL
		 */
		
		// touchpad mode
		mousePanel.add(touchpadMode = Util.newComponent(JCheckBox.class, "touchpadMode"), "wrap");
		touchpadMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				prefs.setCursorControl(touchpadMode.isSelected() ? TouchpadControlStrategy.class.getName() : DefaultControlStrategy.class.getName());
			}
		});

		// assist double clicks
		mousePanel.add(assistDoubleClicks = Util.newComponent(JCheckBox.class, "assistDoubleClicks"), "wrap");
		assistDoubleClicks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				prefs.setAssistDoubleClicks(assistDoubleClicks.isSelected());
			}
		});

		// mouse smoothing
		final ButtonGroup smoothingGroup = new ButtonGroup();
		mousePanel.add(mouseSmoothing = Util.newComponent(JCheckBox.class, "mouseSmoothing"), "split");
		mouseSmoothing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				staticSmoothing.setEnabled(mouseSmoothing.isSelected());
				adaptiveSmoothing.setEnabled(mouseSmoothing.isSelected());
				if (mouseSmoothing.isSelected()) {					
					if (smoothingGroup.getSelection() == null)
						adaptiveSmoothing.doClick();
					else {
						// trigger actionlistener
						if (adaptiveSmoothing.isSelected()) adaptiveSmoothing.doClick();
						if (staticSmoothing.isSelected()) staticSmoothing.doClick();
					}
				} else {
					prefs.setMouseSmoothing(NoSmoothing.class.getName());
				}
			}
		});
		
		mousePanel.add(staticSmoothing = Util.newComponent(JRadioButton.class, "staticSmoothing"));
		staticSmoothing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prefs.setMouseSmoothing(SimpleMovingAverage.class.getName());
			}
		});
		smoothingGroup.add(staticSmoothing);
		
		mousePanel.add(adaptiveSmoothing = Util.newComponent(JRadioButton.class, "adaptiveSmoothing"));
		adaptiveSmoothing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prefs.setMouseSmoothing(AdaptiveExponentialSmoothing.class.getName());
			}
		});
		smoothingGroup.add(adaptiveSmoothing);
		
		
		
		mousePanel.add(new JSeparator(), "newline, split, span, pushx, growx, wrap");	
		
		// right clicks
		mousePanel.add(rightClicks = Util.newComponent(JCheckBox.class, "rightClicks"), "wrap");
		rightClicks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				prefs.setRightClick(rightClicks.isSelected());
			}
		});
		
		// right click activation delay
		JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		wrapper.setOpaque(false);
		final TitledBorder tb = BorderFactory.createTitledBorder(null, r.getString("rightClickDelay"), TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, new JLabel().getFont().deriveFont(11f));
		wrapper.setBorder(tb);
		delaySlider = Util.newComponent(JSlider.class, "delaySlider");
		Dictionary<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put(500, new JLabel("\u00bd"));
		labelTable.put(1000, new JLabel("1"));
		labelTable.put(2000, new JLabel("2"));
		labelTable.put(3000, new JLabel("3"));
		delaySlider.setLabelTable(labelTable);		
		delaySlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!delaySlider.getValueIsAdjusting() && isVisible()) {
					prefs.setRightClickDelay(delaySlider.getValue());
				}
			}
		});
		Insets tbInsets = tb.getBorderInsets(wrapper);
		int tbWidth = tb.getMinimumSize(wrapper).width-tbInsets.left-tbInsets.right+(Util.MAC_OS_X_LEOPARD_OR_HIGHER ? 0 : 20);
		wrapper.add(delaySlider);
		mousePanel.add(wrapper, "center, growx, w " +tbWidth);
		
		/*
		 * TUIO/OSC PANEL
		 */
		
		// enable tuio
		tuioPanel.add(tuioEnable = Util.newComponent(JCheckBox.class, "tuioEnable"), "wrap");
		tuioEnable.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (tuioEnable.isSelected()) {
					String[] s = tuioHost.getText().split(":");
					String errorTitle = null;
					if ((s.length == 1 && s[0].length() == 0) || s.length > 2) {
						errorTitle = r.getString("tuioInvalidFormat");
					} else {
						try {
							int port = 3333;
							if (s.length == 2) {
								port = Integer.valueOf(s[1]);
								if (port < 0 || port > 0xFFFF) {
									throw new NumberFormatException();
								}
							}
							InetAddress.getByName(s[0]);
							
							prefs.setTuioPort(port);
							prefs.setTuioHost(s[0]);
							prefs.setTuioEnabled(true);
						} catch (NumberFormatException e) {
							errorTitle = r.getString("tuioInvalidPort", s[1]);
						} catch (UnknownHostException e) {
							errorTitle = r.getString("tuioInvalidHost", s[0]);
						}
					}
					
					if (errorTitle != null) {
						JOptionPane.showMessageDialog(PreferencesWindow.this, r.getString("tuioErrorMessage"), errorTitle, JOptionPane.ERROR_MESSAGE);
					}
					
				} else {
					prefs.setTuioEnabled(false);
				}
				update();
			}
		});
		
		// tuio host
		tuioPanel.add(tuioHost = Util.newComponent(JTextField.class, "tuioHost"), "growx, pushx, wrap");
		

		JPanel bottomPanel = new JPanel(new MigLayout(Util.MAC_OS_X ? "insets 0 2 0 2, gap 0" : "insets i 0 0 0, gap 0"));
		add(bottomPanel, BorderLayout.SOUTH);
						
		bottomPanel.add(defaultsButton = Util.newComponent(JButton.class, "resetButton"));
//		defaultsButton.putClientProperty("JButton.buttonType", "textured");
		defaultsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prefs.reset();
				// re-set currently chosen mouse control settings
				// (probably not perceived as persistent preferences by user)
				mp.update();
				update();
			}
		});		
		
		JButton helpButton = new JButton(Util.getAction(hh, "help"));
		bottomPanel.add(helpButton, "push, right");
		if (Util.MAC_OS_X_LEOPARD_OR_HIGHER) {
			helpButton.setText("");
			helpButton.putClientProperty("JButton.buttonType", "help");
		} else {
			helpButton.setText(Util.getResourceMap(HelpHandler.class).getString("help"));
		}
		
		Util.getResourceMap(PreferencesWindow.class).injectComponents(this);
		
		pack();
		setResizable(false);
		
		if (Util.WINDOWS) {
			addWindowFocusListener(new WindowFocusListener() {
				public void windowGainedFocus(WindowEvent e) {
					if (!donePack) {
						donePack = true;
						pack();
						Util.placeDialogWindow(PreferencesWindow.this, getWidth(), getHeight());
					}
				}
				public void windowLostFocus(WindowEvent e) {}
			});
		}
	}
	
	private void update() {
		checkForUpdates.setSelected(prefs.checkForUpdates());
		batteryWarning.setSelected(prefs.isLowBatteryWarning());
		rightClicks.setSelected(prefs.isRightClick());
		assistDoubleClicks.setSelected(prefs.assistDoubleClicks());
		delaySlider.setValue((int)prefs.getRightClickDelay());
		tuioEnable.setSelected(prefs.isTuioEnabled());
		tuioHost.setText(String.format("%s:%d", prefs.getTuioHost(), prefs.getTuioPort()));
		tuioHost.setEnabled(!tuioEnable.isSelected());
		tuioHost.setToolTipText(tuioHost.isEnabled() ? Util.getResourceMap(PreferencesWindow.class).getString("tuioHost.toolTipText") : r.getString("tuioDisableToEdit"));
//		wiimoteNumberButtons[prefs.getNumberOfWiimotes()-1].setSelected(true);

		touchpadMode.setSelected(TouchpadControlStrategy.class.getName().equals(prefs.getCursorControl()));
		
		mouseSmoothing.setSelected(!NoSmoothing.class.getName().equals(prefs.getMouseSmoothing()));
		adaptiveSmoothing.setSelected(true);
		staticSmoothing.setSelected(SimpleMovingAverage.class.getName().equals(prefs.getMouseSmoothing()));
		adaptiveSmoothing.setEnabled(mouseSmoothing.isSelected());
		staticSmoothing.setEnabled(mouseSmoothing.isSelected());

		String lang = prefs.getLanguage();
		for (int i = 0; i < languages.getItemCount(); i++) {
			LocaleWrapper lw = (LocaleWrapper)languages.getItemAt(i);
			if (lang.equals(lw.getLocaleString())) {
				languages.setSelectedIndex(i);
				break;
			}
		}
	}
		
	@Action
	public void preferences() {
		if (!isVisible()) {
			update();
			pack();
//			if (Util.WINDOWS && !donePack) {
//				setSize(getWidth(), 300);
//			}
			Util.placeDialogWindow(this, getWidth(), getHeight());
		}
		setVisible(true);
	}

}
