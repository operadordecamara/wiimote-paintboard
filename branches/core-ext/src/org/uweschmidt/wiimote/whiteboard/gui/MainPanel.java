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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler.WiimoteDataListener;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration.CalibrationEvent;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration.CalibrationEventListener;
import org.uweschmidt.wiimote.whiteboard.ds.IRDot;
import org.uweschmidt.wiimote.whiteboard.ds.Wiimote;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class MainPanel extends JPanel implements WiimoteDataListener, CalibrationEventListener {
	
	private static final ImageIcon CALIBRATED = new ImageIcon(MainPanel.class.getResource("resources/icons/ok.png"));
	private static final ImageIcon NOT_CALIBRATED = new ImageIcon(MainPanel.class.getResource("resources/icons/warning.png"));
	
	private static final WWPreferences prefs = WWPreferences.getPreferences();

	private DotLabel[] dotLabel = new DotLabel[4];
	private JProgressBar[] batteryLevel = new JProgressBar[WWPreferences.WIIMOTES];
	private JButton[] resetButton = new JButton[WWPreferences.WIIMOTES];
	private WiimoteIcon[] wiimoteIcon = new WiimoteIcon[WWPreferences.WIIMOTES];
	private JLabel[] statusLabel = new JLabel[WWPreferences.WIIMOTES];
	private JCheckBox cursorControl;
	private JRadioButton moveMouse, leftClick;
	private JButton calibrationButton, cameraButton /*,warpedButton, calibrationInfoButton*/;
	private ScreenSelector screenSelector;
	
	private boolean notifiedLowBattery = false;
//	private boolean donePack = false;
	
	private Wiimote[] wiimotes = new Wiimote[WWPreferences.WIIMOTES];
	
	private WiimoteDataHandler dh;
	private WiimoteCalibration calibration;
	private ResourceMap r = Util.getResourceMap(MainPanel.class);
	
	public MainPanel(WiimoteDataHandler dh, WiimoteCalibration calibration) {
		this.dh = dh;
		this.calibration = calibration;
		
		dh.addWiimoteDataListener(this);
		calibration.addCalibrationEventListener(this);
		
		createComponents();

		getValues();
		update();
	}
	
	public void getValues() {
		cursorControl.setSelected(dh.isCursorControl());
//		rightClick.setSelected(prefs.isRightClick());
		leftClick.setSelected(prefs.isLeftClick());
		moveMouse.setSelected(!prefs.isLeftClick());
	}
	
	public void calibrationEvent(CalibrationEvent e) {
		switch (e) {
			case FINISHED:
			case LOADED:
				cursorControl.setSelected(true);
			case STARTED:
			case SCREEN_CHANGED:
			case ABORTED:
				update();
				break;
		}	
	}
	
	public void batteryLevel(Wiimote wiimote, double level) {
		batteryLevel[wiimote.getId()-1].setValue((int) Math.round(level * 100));
		batteryLevel[wiimote.getId()-1].setString(r.getString("batteryLevel", level * 100));
		
		if (!notifiedLowBattery && prefs.isLowBatteryWarning() && dh.isConnected(wiimote) && level <= .05) {
			notifiedLowBattery = true;
			WiimoteWhiteboard.getLogger().log(Level.WARNING, r.getString("lowBattery"));
			new Thread(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, r.getString("lowBattery"), WiimoteWhiteboard.getProperty("id"), JOptionPane.WARNING_MESSAGE);
				}
			}).start();
		}
	}
	
	public void irLights(Wiimote wiimote, IRDot[] lights) {
		for (int i = 0; i < lights.length; i++) {
			dotLabel[i].update(wiimote, lights[i] != null);
		}
	}
	
	public void irWarped(Map<Wiimote, IRDot[]> data, Point[] warped) {
	}	
	
	public void wiimoteConnected(Wiimote wiimote) {
		wiimotes[wiimote.getId()-1] = wiimote;
		
		if (wiimote.getId() > 1) {
			for (int i = 0; i < wiimote.getId(); i++) {
				wiimoteIcon[i].setVisible(true);
			}
		}
		statusLabel[wiimote.getId()-1].setVisible(true);

//		Application.getInstance(WiimoteWhiteboard.class).getMainFrame().pack();

		batteryLevel[wiimote.getId()-1].setVisible(true);
		resetButton[wiimote.getId()-1].setVisible(true);
		cursorControl.setSelected(true);
		update();
	}
	
	public void wiimoteDisconnected(Wiimote wiimote) {
		if (wiimote.getId() != 1) batteryLevel[wiimote.getId()-1].setVisible(false);
		if (wiimote.getId() != 1) wiimoteIcon[wiimote.getId()-1].setVisible(false);
		if (wiimote.getId() != 1) resetButton[wiimote.getId()-1].setVisible(false);
		if (wiimote.getId() != 1) statusLabel[wiimote.getId()-1].setVisible(false);
		update();
//		Application.getInstance(WiimoteWhiteboard.class).getMainFrame().pack();
//		WiimoteWhiteboard.getLogger().log(Level.SEVERE, r.getString("disconnected", id));
		JOptionPane.showMessageDialog(null, r.getString("disconnected", wiimote.getId()), WiimoteWhiteboard.getProperty("id"), JOptionPane.ERROR_MESSAGE);
		Application.getInstance(WiimoteWhiteboard.class).exit();
	}
	
	
	/*
	 * UPDATE UI WIDGETS
	 */
	
	@Action
	public void update() {
		calibrationButton.setEnabled(dh.isConnected());
		for (int i = 0; i < WWPreferences.WIIMOTES; i++) {
			resetButton[i].setEnabled(dh.isConnected(wiimotes[i]));
			wiimoteIcon[i].displayConnected(dh.isConnected(wiimotes[i]));
			if (dh.isConnected(wiimotes[i])) {				
				boolean calibrated = calibration.isCalibrated(wiimotes[i]);
				statusLabel[i].setText(r.getString(calibrated ? "calibrated" : "notCalibrated"));
				statusLabel[i].setIcon(calibrated ? CALIBRATED : NOT_CALIBRATED);
			}
		}
		cameraButton.setEnabled(dh.isConnected());
//		warpedButton.setEnabled(dh.isConnected() && calibration.isDone() && calibration.isAnyCalibrated(dh.getConnectedWiimotes()));
		screenSelector.setEnabled(dh.isConnected() && !calibration.inProgress());		
		
		if (!dh.isConnected()) {
			batteryLevel[0].setString(r.getString("searching"));
			batteryLevel[0].setValue(0);
			batteryLevel[0].setToolTipText(Util.getResourceMap(MainPanel.class).getString("batteryLevelBar.toolTipText"));
		} else {
			batteryLevel[0].setToolTipText(null);
		}
		
		cursorControl.setEnabled(dh.isConnected() && calibration.isDone() && calibration.isAnyCalibrated(dh.getConnectedWiimotes()));
		if (!cursorControl.isEnabled())
			cursorControl.setSelected(false);
		
		moveMouse.setEnabled(cursorControl.isSelected());
		leftClick.setEnabled(cursorControl.isSelected());
//		rightClick.setEnabled(cursorControl.isSelected() && leftClick.isSelected());

		dh.setCursorControl(cursorControl.isSelected());
		prefs.setLeftClick(leftClick.isSelected());
//		prefs.setRightClick(/*rightClick.isEnabled() && */rightClick.isSelected());
	}
	
	/*
	 * MAIN PANEL
	 */
	
	private void createComponents() {
		setLayout(new MigLayout("nocache, hidemode 3, gap 0", "[fill]"));
		
		addHeadline(r.getString("wiimoteHeadline"), false);
		
		for (int i = 0; i < WWPreferences.WIIMOTES; i++) {
			add(wiimoteIcon[i] = new WiimoteIcon(i+1), "split");
			if (i > 0) wiimoteIcon[i].setVisible(false);
		}
		
		for (int i = 0; i < WWPreferences.WIIMOTES; i++) {
			add(batteryLevel[i] = Util.newComponent(JProgressBar.class, "batteryLevelBar"), (i == 0 ? "newline related/2, " : "") + "split");
			if (i > 0) batteryLevel[i].setVisible(false);
		}

		for (int i = 0; i < WWPreferences.WIIMOTES; i++) {
			add(statusLabel[i] = Util.newComponent(JLabel.class, "statusLabel"), (i == 0 ? "newline related/2, split, " : "") + "sg, h 16!, center");
			statusLabel[i].setFont(statusLabel[i].getFont().deriveFont(10f));
			if (i > 0) statusLabel[i].setVisible(false);
		}

		String gap = Util.MAC_OS_X_LEOPARD_OR_HIGHER ? "0" : "related/2";
		for (int i = 0; i < WWPreferences.WIIMOTES; i++) {
			final int j = i;
			add(resetButton[i] = Util.newComponent(JButton.class, "resetCameraButton"), (i == 0 ? "newline "+gap+", split, " : "") + "");
			resetButton[i].putClientProperty("JButton.buttonType", "textured");
			resetButton[i].putClientProperty("JComponent.sizeVariant", "small");
			resetButton[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						dh.enableIR(wiimotes[j]);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			});
			if (i > 0) resetButton[i].setVisible(false);
		}
		
		add(new JSeparator(), String.format("newline %s, span, growx, wrap related", Util.MAC_OS_X_LEOPARD_OR_HIGHER ? "related/3" : "related"));
		
		add(Util.newComponent(JLabel.class, "visibleDotsLabel"), "split" + (Util.MAC_OS_X ? ", gapleft 4" : ""));		
		add(dotLabel[0] = new DotLabel("1"), "w 18, h 18");
		add(dotLabel[1] = new DotLabel("2"), "w 18, h 18");
		add(dotLabel[2] = new DotLabel("3"), "w 18, h 18");
		add(dotLabel[3] = new DotLabel("4"), "w 18, h 18, wrap related" + (Util.MAC_OS_X ? ", gapright 4" : ""));

		add(cameraButton = new JButton(Util.getAction(new CameraMonitor(dh), "monitor")), "");
		cameraButton.putClientProperty("JButton.buttonType", "textured");
		
		// CALIBRATION
		
		addHeadline(r.getString("calibrationHeadline"));
		
//		add(warpedButton = new JButton(SC.getAction(new WarpedMonitor(dh, calibration), "monitor")), "wrap");
		
		final JButton infoButton = new JButton(Util.getAction(new CalibrationInfoWindow(calibration, dh), "info"));
//		final JToggleButton infoButton = new JToggleButton(Util.getAction(new CalibrationInfoWindow(calibration, dh), "info"));
		infoButton.putClientProperty("JButton.buttonType", "textured");
		infoButton.putClientProperty("JComponent.sizeVariant", "small");
		add(infoButton, "wrap related");
//		add(new JSeparator(), "span, growx, wrap");
	
		
		add(screenSelector = new ScreenSelector(calibration, dh), "align center, grow 0, wrap related");

		add(calibrationButton = Util.newComponent(JButton.class, "calibrationButton"), "");
		calibrationButton.putClientProperty("JButton.buttonType", "textured");
		calibrationButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				calibration.start(dh.getConnectedWiimotes());
			}
		});

		// MOUSE CONTROL
	
		addHeadline(r.getString("mouseControlHeadline"));
		
		add(cursorControl = Util.newComponent(JCheckBox.class, "cursorControl"), "wrap related");
		add(moveMouse = Util.newComponent(JRadioButton.class, "moveMouse"), "pad 0 20 0 0, wmin pref+20, wrap related");
		add(leftClick = Util.newComponent(JRadioButton.class, "leftClick"), "pad 0 20 0 0, wmin pref+20, wrap");
//		add(rightClick = SC.newComponent(JCheckBox.class, "rightClick"), "pad 0 20 0 0, wrap");

//		add(new JSeparator(), "wrap");
//		final JButton exitButton = new JButton(SC.getAction(Application.getInstance(), "quitApp"));
//		add(exitButton, "wrap");

		ButtonGroup group = new ButtonGroup();
		group.add(moveMouse);
		group.add(leftClick);

		cursorControl.addActionListener(Util.getAction(this, "update"));
		moveMouse.addActionListener(Util.getAction(this, "update"));
		leftClick.addActionListener(Util.getAction(this, "update"));
//		rightClick.addActionListener(SC.getAction(this, "update"));
		
		// fixes issue that components stay grayed out although they're enabled
		// (mac os x), only happens when mainFrame doesn't have focus on connect
		// workaround for windows: pack() once frame gains focus, due to
		// incorrect height apparently caused by non-resizability
		Application.getInstance(WiimoteWhiteboard.class).getMainFrame().addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
				updateUI();
//				if (!Util.MAC_OS_X && !donePack) {
//					donePack = true;
//					Application.getInstance(WiimoteWhiteboard.class).getMainFrame().pack();
//				}
			}
			public void windowLostFocus(WindowEvent e) {}
		});
		
		Util.getResourceMap(MainPanel.class).injectComponents(this);
	}
	
	private JLabel addHeadline(String name) {
		return addHeadline(name, true);
	}
		
	private JLabel addHeadline(String name, boolean newline) {
		final JLabel label = new JLabel(name);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
		add(label, (newline ? "newline unrelated, " : "") + "split, span");
		add(new JSeparator(), "growx, wrap related");
		return label;
	}
	
	private class DotLabel extends JLabel {
		
		private boolean[] state = new boolean[WWPreferences.WIIMOTES];
		
		public DotLabel(String name) {
			setText(name);
			setHorizontalAlignment(SwingConstants.CENTER);
			setBorder(BorderFactory.createLineBorder(Color.lightGray));
		}
		
		public void update(Wiimote wiimote, boolean state) {
			if (this.state[wiimote.getId() - 1] != state) {
				this.state[wiimote.getId() - 1] = state;
				repaint();
			}
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			final int n = Math.max(1, dh.getNumberOfConnectedWiimotes());
			for (int i = 1; i <= n; i++) {
				g.setColor(state[i-1] ? WiimoteIcon.COLORS[i-1] : SystemColor.textInactiveText);
				g.fillRect(0, (i-1)*this.getHeight()/n, this.getWidth(), this.getHeight()/n);
//				g.fillRect((i-1)*this.getWidth()/n, 0, this.getWidth()/n, this.getHeight());
			}
			super.paintComponent(g);
		}
		
	}
}
