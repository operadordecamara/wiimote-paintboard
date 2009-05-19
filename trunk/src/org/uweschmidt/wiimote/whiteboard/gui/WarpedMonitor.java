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
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler.WiimoteDataListener;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration;
import org.uweschmidt.wiimote.whiteboard.ds.IRDot;
import org.uweschmidt.wiimote.whiteboard.ds.Wiimote;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class WarpedMonitor extends JDialog implements WiimoteDataListener {

	private static final int REPAINT_FREQ = 1000 / 50;
	static final int RADIUS = 10;

	private JPanel canvas;
	private Point2D[][] lights = new Point2D[WWPreferences.WIIMOTES+1][4];
	private LightLabel[][] labels = new LightLabel[WWPreferences.WIIMOTES+1][4];
	private WiimoteCalibration calibration;
	private WiimoteDataHandler dh;

	public WarpedMonitor(WiimoteDataHandler dh, final WiimoteCalibration calibration) {
		super(Application.getInstance(WiimoteWhiteboard.class).getMainFrame(), "Warped Point Monitor");
		setLayout(new BorderLayout());
		dh.addWiimoteDataListener(this);
		this.calibration = calibration;
		this.dh = dh;
		((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		canvas = new JPanel(null, true);
		canvas.setBorder(BorderFactory.createLineBorder(Color.black));
		add(canvas, BorderLayout.CENTER);
		
		setUndecorated(true);
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_F || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					GraphicsDevice screen = calibration.getScreen();
					if (screen != null) {
						if (screen.getFullScreenWindow() == WarpedMonitor.this) {
							((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
							screen.setFullScreenWindow(null);
							Util.placeDialogWindow(WarpedMonitor.this, 640, 480);
						} else {
							if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
								WarpedMonitor.this.setVisible(false);
							else {
								((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder());
								screen.setFullScreenWindow(WarpedMonitor.this);
							}
						}
					}
				}
			}
		});

		Util.placeDialogWindow(this, 640, 480);
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
				for (int i = WWPreferences.WIIMOTES; i >= 0; i--)
					for (int j = 0; j < 4; j++) {
						if (labels[i][j] != null)
							labels[i][j].update();
					}
			}
		}
	}

	public void irLights(Wiimote wiimote, IRDot[] lights) {
	}
	
	public void irWarped(Map<Wiimote, IRDot[]> data, Point[] warped) {
		if (isVisible()) {
			Rectangle bounds = calibration.getScreen().getDefaultConfiguration().getBounds();
			for (int i = 0; i < 4; i++) {
				final Point w = warped[i];
				this.lights[0][i] = w == null ? null : new Point2D.Double(w.getX() / bounds.getWidth(), 1 - w.getY() / bounds.getHeight());
			}
//			Map<String, Point[]> warpedData = new LinkedHashMap<String, Point[]>();
			for (Wiimote wiimote : dh.getConnectedWiimotes()) {
//				Point[] pArr = new Point[4];
				for (int i = 0; i < 4; i++) {
					final Point2D w = calibration.warp(i, wiimote, data);
//					pArr[i] = w;
					this.lights[wiimote.getId()][i] = w == null ? null : new Point2D.Double(w.getX() / bounds.getWidth(), 1 - w.getY() / bounds.getHeight());
				}
//				warpedData.put(address, pArr);
			}
//			
//			Point[][] cluster = PointClusterer.cluster(warpedData);
//			for (int i = 0; i < cluster.length; i++) {
//				Point[] c = cluster[i];
//				double x = 0, y = 0;
//				for (Point d : c) {
//					x += d.getX();
//					y += d.getY();
//				}
//				final Point2D w = new Point2D.Double(x/c.length, y/c.length);
//				this.lights[0][i] = new Point2D.Double(w.getX() / bounds.getWidth(), 1 - w.getY() / bounds.getHeight());
////				System.out.println(this.lights[0][i]);
//			}
//			for (int i = cluster.length; i < 4; i++)
//				this.lights[0][i] = null;
			
			
		}
	}

	public void batteryLevel(Wiimote wiimote, double level) {
	}

	public void wiimoteConnected(Wiimote wiimote) {
		if (wiimote.getId() == 1)
			for (int i = 0; i < 4; i++)
				canvas.add(labels[0][i] = new LightLabel(canvas, lights, 0, i + 1, Color.red));
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
