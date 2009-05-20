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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
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
public class CalibrationInfoWindow extends JDialog implements CalibrationEventListener, WiimoteDataListener {
	
	private static class WiimoteWrapper {
		@SuppressWarnings("unused")
		private int idx;
		private JPanel panel;
		private WiimoteIcon icon;
		private JProgressBar trackingUtility;
		private Double d[];
		private IRDot[] lights = new IRDot[4];
		private void setVisible(boolean visible) {
			panel.setVisible(visible);
			icon.setVisible(visible);
			trackingUtility.setVisible(visible);
		}
	}
	
	private static final long REPAINT_FREQ = 1000 / 25;
	
	private static final Color CALIBRATED_COLOR = SystemColor.textHighlight;
	private static final Color TRACKING_COLOR = SystemColor.text;
	
	private static final Color COLORS[] = {Color.blue, Color.red, Color.green, Color.orange, Color.black, Color.cyan, Color.magenta, Color.pink};
	
	private WiimoteWrapper ww[] = new WiimoteWrapper[WWPreferences.WIIMOTES];
	private WiimoteCalibration calibration;
	private WiimoteDataHandler dh;
	private boolean calibrated = false;

//	private Matrix F;
	
	
	public CalibrationInfoWindow(WiimoteCalibration calibration, WiimoteDataHandler dh) {
		super(Application.getInstance(WiimoteWhiteboard.class).getMainFrame(), Util.getResourceMap(CalibrationInfoWindow.class).getString("info.Action.text"));
		getRootPane().putClientProperty("Window.style", "small");
		this.calibration = calibration;
		this.dh = dh;
		calibration.addCalibrationEventListener(this);
		dh.addWiimoteDataListener(this);
		setLayout(new MigLayout("hidemode 3"));
		// info labels
		final JLabel taLabel = Util.newComponent(JLabel.class, "trackingAreaLabel");
		final JLabel csLabel = Util.newComponent(JLabel.class, "calibratedScreenLabel");
		taLabel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
		csLabel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
		taLabel.setBackground(TRACKING_COLOR);
		csLabel.setBackground(CALIBRATED_COLOR);
		taLabel.setForeground(SystemColor.textText);
		csLabel.setForeground(SystemColor.textHighlightText);
		add(taLabel, "sg 1, split, span, growx, pushx");
		add(csLabel, "sg 1, gapbefore 0, growx, pushx, wrap");
		
		for (int i = 0; i < ww.length; i++) {
			ww[i] = new WiimoteWrapper();
			ww[i].idx = i;
			ww[i].icon = new WiimoteIcon(i+1);
			ww[i].icon.displayConnected(true);
			add(ww[i].panel = new InfoPanel(ww[i]), "flowy, w 400, h 300, grow, push");
			add(ww[i].icon, "cell "+i+" 1, growx, pushx");
			add(ww[i].trackingUtility = Util.newComponent(JProgressBar.class, "trackingUtility"), "cell "+i+" 1, growx, pushx");
			ww[i].setVisible(i == 0);
		}
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					setVisible(false);
				}
			}
		});
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				componentResized(null);
			}
			@Override
			public void componentResized(ComponentEvent e) {				
				repaintPanels();
			}
		});
		
		
		Util.getResourceMap(CalibrationInfoWindow.class).injectComponents(this);
		pack();
		Util.placeDialogWindow(this, getWidth(), getHeight());
//		setResizable(false);
//		setVisible(true);
		new Timer(true).schedule(new UpdateTask(), 0, REPAINT_FREQ);
	}
	
	private class UpdateTask extends TimerTask {
		@Override
		public void run() {
			if (isVisible()) {
				repaintPanels();
			}
		}
	}
	
	private void repaintPanels() {
		for (WiimoteWrapper wrapper : ww) {
			if (wrapper.panel.isVisible())
				wrapper.panel.repaint();
		}
	}
	
	@Action(enabledProperty="calibrated")
	public void info() {
		setVisible(true);
	}
	
	public boolean isCalibrated() {
		return calibrated;
	}
	
	private void updateCalibrated() {
		final boolean old = calibrated;
		calibrated = dh.isConnected() && calibration.isDone() && calibration.isAnyCalibrated(dh.getConnectedWiimotes());
		firePropertyChange("calibrated", old, calibrated);
	}
	
	public void calibrationEvent(CalibrationEvent e) {
//		System.out.println(e);
		switch (e) {
			case LOADED:
				update();
			case FINISHED:
				update();
				break;
			case SCREEN_CHANGED:
			case STARTED:
			case ABORTED:
			case SAVED:
		}
		updateCalibrated();
		if (!isCalibrated())
			setVisible(false);
	}
	
	public void batteryLevel(Wiimote wiimote, double level) {}
	public void irLights(Wiimote wiimote, IRDot[] lights) {
		ww[wiimote.getId()-1].lights = lights;
//		ww[wiimote.getId()-1].panel.repaint();
	}
	public void irWarped(Map<Wiimote, IRDot[]> data, Point[] warped) {}
	public void wiimoteConnected(Wiimote wiimote) {
		update();
		updateCalibrated();
	}
	public void wiimoteDisconnected(Wiimote wiimote) {}	
	
	private int r(double d) {
		return (int)Math.round(d);
	}
	
	private void update() {
		Map<String, Double[]> finals = calibration.getFinals();
		
//		// XXX 3D
//		if (dh.getNumberOfConnectedWiimotes() == 2) {
//			boolean continue3d = true;
//			for (Wiimote wiimote : dh.getConnectedWiimotes()) {			
//				if (!calibration.isCalibrated(wiimote)) continue3d = false;
//			}
//			
//			if (continue3d) {
//				Rectangle bounds = calibration.getScreen().getDefaultConfiguration().getBounds();
//				Map<String, PerspectiveTransform> transformer = calibration.getTransformer();
//				int innerpoints = 1;
//				Point2D warped[][] = new Point2D[2][(int)Math.pow(innerpoints+2,2)];
//				for (Wiimote wiimote : dh.getConnectedWiimotes()) {
//					try {
//						PerspectiveTransform ti = transformer.get(wiimote.getAddress()).createInverse();
//						int c = 0;
//						for (double i = 0; i <= 1; i += (1d / (innerpoints + 1d))) {
//							for (double j = 0; j <= 1; j += (1d / (innerpoints + 1d))) {
////								System.out.printf("(%.2f,%.2f) - ",i,j);
//								// reverse transform to "sample" matching points in wiimote coordinates (from 0 ... 1)
//								final Point2D p = ti.transform(new Point2D.Double(i*bounds.width,j*bounds.height), null);
//								// get size for virtual wiimote camera view
//								final Dimension panelSize = ww[0].panel.getSize();
//								// scale wiimote coordinates by virtual camera size
//								p.setLocation(p.getX()*panelSize.width, p.getY()*panelSize.height);
//								warped[wiimote.getId()-1][c++] = p;
//							}
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
////				System.out.println("warped[0] - " + Arrays.asList(warped[0]));
////				System.out.println("warped[1] - " + Arrays.asList(warped[1]));
//				F = NormalizedEightPointAlgorithm.computeFundamentalMatrix(warped[0], warped[1]);
////				F.print(18, 15);
//			} else {
//				F = null;
//			}
//		} else {
//			F = null;
//		}
//		// XXX END 3D
		
		for (Wiimote wiimote : dh.getConnectedWiimotes()) {
			WiimoteWrapper w = ww[wiimote.getId()-1];
			if (!calibration.isCalibrated(wiimote)) continue;
			
			w.d = finals.get(wiimote.getAddress());
			
			double x0 = w.d[0]*1024, y0 = w.d[1]*768;
			double x1 = w.d[2]*1024, y1 = w.d[3]*768;
			double x3 = w.d[4]*1024, y3 = w.d[5]*768;
			double x2 = w.d[6]*1024, y2 = w.d[7]*768;
			double idealArea = 0.8 * 1024 * 0.8 * 768;
			double calibratedArea = 0.5 * (Math.abs(
					(x1 - x2) * (y0 - y3) - (x0 - x3) * (y1 - y2)
			));
			
			w.trackingUtility.setValue(r(calibratedArea));
			w.trackingUtility.setString(Util.getResourceMap(CalibrationInfoWindow.class).getString("trackingUtilString", 100 * (calibratedArea / idealArea)));
			
			if (!w.panel.isVisible()) {
				w.setVisible(true);
				pack();
				Util.placeDialogWindow(this, getWidth(), getHeight());
			}
		}
		repaintPanels();
	}
	
	private class InfoPanel extends JPanel {
		private static final int DIAMETER = 6;
		private WiimoteWrapper wrapper;
		public InfoPanel(WiimoteWrapper wrapper) {
			this.wrapper = wrapper;
			setLayout(null);
			setOpaque(true);
			setBackground(TRACKING_COLOR);
			setBorder(BorderFactory.createLineBorder(SystemColor.lightGray));
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (wrapper.d != null) {
				Graphics2D g2d = (Graphics2D)g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				final int w = getWidth();
				final int h = getHeight();
				int x[] = new int[5], y[] = new int[5];
				for (int i = 0; i < 4; i++) {
					x[i] = r(wrapper.d[2*i]*w);
					y[i] = r(h-wrapper.d[2*i+1]*h);
				}
				x[4] = x[0]; y[4] = y[0];
				
//				g2d.setColor(Color.black);
//				g2d.setStroke(new BasicStroke(2f));
//				g2d.drawPolygon(x, y, 5);
				g2d.setColor(CALIBRATED_COLOR);
				g2d.fillPolygon(x, y, 5);
				
//				// XXX 3D				
//				int[] matchingDotNumber = new int[]{-1,-1,-1,-1};
//				double[] minDistances = new double[]{Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
//
//				// do only for second wiimote
//				if (dh.getNumberOfConnectedWiimotes() == 2 && F != null && wrapper.idx == 1) {
//					WiimoteWrapper other = ww[wrapper.idx == 0 ? 1 : 0];
//					for (int i = 0; i < 4; i++) {
//						IRDot dot = other.lights[i];
//						if (dot != null) {
//							g2d.setColor(COLORS[i]);
//							Matrix p = new Matrix(new double[]{dot.x*w,dot.y*h,1},3);
//							Point2D line[] = NormalizedEightPointAlgorithm.hom2Line(
//									(wrapper.idx == 0 ? F.transpose() : F).times(p),
//									w, h
//							);
//							if (line[0] != null && line[1] != null) {
//								
//								g2d.drawLine(r(line[0].getX()), r(h-line[0].getY()), r(line[1].getX()), r(h-line[1].getY()));
//								
//								double minDist = Double.POSITIVE_INFINITY;
//								int minDotNr = -1; Point2D minIntersection = null;
//
//								double m = (line[1].getY() - line[0].getY()) / (line[1].getX() - line[0].getX());
//								double m_dash = -1/m;
//								double b = line[0].getY() - line[0].getX() * m;										
//								
//								for (int j = 0; j < 4; j++) {
//									IRDot dot_ = wrapper.lights[j];
//									if (dot_ != null) {
////										g2d.setColor(COLORS[j]);
//
//										double b_dash = h*dot_.y - m_dash * w*dot_.x;
//										// intersection point
//										double xi = (b_dash - b) / (m - m_dash);
//										double yi = m * xi + b;
//										
//										double dist = Point2D.distance(w*dot_.x, h*dot_.y, xi, yi);
//										if (dist < minDist) {
//											minDist = dist;
//											minDotNr = j;
//											minIntersection = new Point2D.Double(xi,yi);
//										}
////										System.out.printf("Distance for dot %d: %.3f\n", i, dist);
////										g2d.setColor(Color.black);
////										g2d.drawLine(r(xi), r(h-yi), r(w*dot_.x), r(h-h*dot_.y));
//									}
//
//								}
//								if (minDotNr != -1 && minDist < 25) {
//									if (minDist < minDistances[minDotNr]) {
//										matchingDotNumber[minDotNr] = i;
//										minDistances[minDotNr] = minDist;
//									}
//									IRDot minDot = wrapper.lights[minDotNr];
////									System.out.printf("Distance for dot %d: %.3f\n", i, minDist);
//									g2d.setColor(Color.black);
//									g2d.drawLine(r(minIntersection.getX()), r(h-minIntersection.getY()), r(w*minDot.x), r(h-h*minDot.y));
//								}
//							}
//						}
//					}
//				}
//				// XXX END 3D
				
				for (int i = 0; i < 4; i++) {
					IRDot dot = wrapper.lights[i];
					if (dot != null) {
//						// no 3D stuff
//						if (F == null)
							g2d.setColor(COLORS[i]);
//						// XXX 3D
//						else {
//							// use matching color for second wiimote
//							if (wrapper.idx == 1)
//								// unmatched points of second wiimote are distinct points and have another color 
//								g2d.setColor(COLORS[matchingDotNumber[i] != -1 ? matchingDotNumber[i] : 4+i]);
//							else
//								// use base colors for first wiimote
//								g2d.setColor(COLORS[i]);
//						}
//						// XXX END 3D
						g2d.fillOval(r(w*dot.x)-DIAMETER/2, r(h-h*dot.y)-DIAMETER/2, DIAMETER, DIAMETER);
//						// display internal number assigned by wiimote
//						g2d.setColor(Color.black);
//						g2d.drawString(""+i, r(w*dot.x), r(h-h*dot.y));
					}
				}
			}
		}		
	}


}
