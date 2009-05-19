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

import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler.WiimoteDataListener;
import org.uweschmidt.wiimote.whiteboard.ds.IRDot;
import org.uweschmidt.wiimote.whiteboard.ds.Wiimote;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class CameraMonitor extends JDialog implements WiimoteDataListener {

	private static final long REPAINT_FREQ = 1000 / 25;

	private JPanel canvas;
	private IRDot[][] lights = new IRDot[WWPreferences.WIIMOTES+1][4];
	private LightLabel[][] labels = new LightLabel[WWPreferences.WIIMOTES+1][4];

	public CameraMonitor(WiimoteDataHandler dh) {
		super(Application.getInstance(WiimoteWhiteboard.class).getMainFrame(), Util.getResourceMap(CameraMonitor.class).getString("monitor.Action.text"));
		getRootPane().putClientProperty("Window.style", "small");
		setLayout(new MigLayout());
		
		dh.addWiimoteDataListener(this);

		canvas = new JPanel(null, true);
		canvas.setOpaque(true);
		canvas.setBorder(BorderFactory.createLineBorder(SystemColor.inactiveCaptionBorder));
		add(canvas, "w 50sp, h 50sp, grow, push");
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					setVisible(false);
				}
			}
		});

		pack();
		setLocationRelativeTo(null);

		new Timer(true).schedule(new UpdateTask(), 0, REPAINT_FREQ);
	}

	@Action
	public void monitor() {
		setVisible(true);
	}

	private class UpdateTask extends TimerTask {
		@Override
		public void run() {
			if (isVisible()) {
				for (int i = 1; i <= WWPreferences.WIIMOTES; i++)
					for (int j = 0; j < 4; j++) {
						if (labels[i][j] != null)
							labels[i][j].update();
					}
			}
		}
	}

	public void irLights(Wiimote wiimote, IRDot[] lights) {
		this.lights[wiimote.getId()] = lights;
	}
	
	public void irWarped(Map<Wiimote, IRDot[]> data, Point[] warped) {
	}

	public void batteryLevel(Wiimote wiimote, double level) {
	}

	public void wiimoteConnected(Wiimote wiimote) {
		for (int i = 0; i < 4; i++)
			canvas.add(labels[wiimote.getId()][i] = new LightLabel(canvas, lights, wiimote.getId(), i + 1));
	}

	public void wiimoteDisconnected(Wiimote wiimote) {
		for (int i = 0; i < 4; i++) {
			canvas.remove(labels[wiimote.getId()][i]);
			labels[wiimote.getId()][i] = null;
		}
	}

}
