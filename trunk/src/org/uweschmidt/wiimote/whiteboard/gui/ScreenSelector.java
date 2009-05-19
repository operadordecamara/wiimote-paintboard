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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.jai.PerspectiveTransform;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler.WiimoteDataListener;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration;
import org.uweschmidt.wiimote.whiteboard.ds.IRDot;
import org.uweschmidt.wiimote.whiteboard.ds.Wiimote;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class ScreenSelector extends JPanel implements WiimoteDataListener {
	
	private static final GraphicsDevice DEFAULT_SCREEN = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	private static final int MAX_H = 130;
	private static final int MAX_W = 185;
	
	private static final long REPAINT_FREQ = 1000 / 20;
	
	private WiimoteCalibration calibration;
//	private WiimoteDataHandler dh;
	private List<ScreenBox> screenBoxes = new LinkedList<ScreenBox>();
	private Point lastCursor = null, cursor = null;
	
	public ScreenSelector(WiimoteCalibration calibration, WiimoteDataHandler dh) {
		super(null);
		this.calibration = calibration;
//		this.dh = dh;
		dh.addWiimoteDataListener(this);
		
		final GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		
		// calculate maximum screen bounds (virtual device)
		Rectangle r = new Rectangle();
		for (GraphicsDevice s : screens) {
			r = r.union(s.getDefaultConfiguration().getBounds());
		}
		
		final int maxH = MAX_H - (screens.length == 1 ? 30 : 0);
		// proportional bounds of panel
		int w = MAX_W;
		int h = (int) Math.round((w * r.getHeight()) / r.getWidth());
		// if too high
		if (h > maxH) {
			h = maxH;
			w = (int) Math.round((h * r.getWidth()) / r.getHeight());
		}
		setPreferredSize(new Dimension(w, h));
		
		// map screen bounds to panel bounds
		PerspectiveTransform t = PerspectiveTransform.getQuadToQuad(
				r.getMinX(), r.getMinY(), r.getMaxX(), r.getMinY(),
				r.getMaxX(), r.getMaxY(), r.getMinX(), r.getMaxY(),
				0, 0, w, 0, w, h, 0, h);
		
		// TODO temporary until multiple screens are allowed
		ButtonGroup bg = new ButtonGroup();
		for (int i = 0; i < screens.length; i++) {
			Rectangle b = screens[i].getDefaultConfiguration().getBounds();
			final ScreenBox sb = new ScreenBox(screens[i], i, transformBounds(t, b), screens[i].equals(DEFAULT_SCREEN));
			bg.add(sb);
			screenBoxes.add(sb);
			add(sb);
		}
		
		new Timer(true).schedule(new UpdateTask(), 0, REPAINT_FREQ);
	}
	
	private class UpdateTask extends TimerTask {
		@Override
		public void run() {
			if (cursor != lastCursor) {
				for (ScreenBox sb : screenBoxes) {
					if (sb.isSelected() && sb.isEnabled()) {
						lastCursor = cursor;
						sb.repaint();
					}
				}
			}
		}
	}
	
	private Rectangle transformBounds(PerspectiveTransform t, Rectangle b) {
		Point2D ul = t.transform(new Point2D.Double(b.getMinX(), b.getMinY()), null);
		Point2D ur = t.transform(new Point2D.Double(b.getMaxX(), b.getMinY()), null);
		Point2D ll = t.transform(new Point2D.Double(b.getMinX(), b.getMaxY()), null);		
		return new Rectangle2D.Double(ul.getX(), ul.getY(), ul.distance(ur), ul.distance(ll)).getBounds();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		enableScreenBoxes(enabled);
	}
	
	// TODO use JToggleButton instead of JRadioButton or JCheckBox
	private class ScreenBox extends JRadioButton {
		private final BasicStroke STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
		private static final int DIAMETER = 4;
		private static final int MAX_TRACE = 1000 / (int)REPAINT_FREQ;
		private LinkedList<Point> trace = new LinkedList<Point>();		
		private GraphicsDevice screen;
		private final Rectangle bounds;
//		private final int screenWidth, screenHeight; 
		public ScreenBox(GraphicsDevice screen, int i, Rectangle b, boolean selected) {
			super(Util.getResourceMap(ScreenSelector.class).getString("screenText", i+1, (int)screen.getDefaultConfiguration().getBounds().getWidth(), (int)screen.getDefaultConfiguration().getBounds().getHeight()));
			this.screen = screen;
			bounds = screen.getDefaultConfiguration().getBounds();
//			screenWidth = (int)screen.getDefaultConfiguration().getBounds().getWidth();
//			screenHeight = (int)screen.getDefaultConfiguration().getBounds().getHeight();
			
			setBounds(b);
			setSelected(selected);
			
			setFocusable(false);
			setHorizontalAlignment(SwingConstants.CENTER);
			
			if (!Util.MAC_OS_X) setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(true);
			
			addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						calibration.setScreen(ScreenBox.this.screen);
					}
//					enableScreenBoxes(true);
				}
			});
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (isSelected() && cursor != null) {
				Graphics2D g2d = (Graphics2D)g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				try {
//					g.setColor(new Color(255,255,255,75));
//					g.fillRect(0, 0, getWidth(), getHeight());
					
					int x = Math.max(0, Math.min(getWidth(), (int)Math.round(((cursor.x - bounds.x) / bounds.getWidth()) * getWidth())));
					int y = Math.max(0, Math.min(getHeight(), (int)Math.round(((cursor.y - bounds.y) / bounds.getHeight()) * getHeight()))); 

					if (trace.size() > MAX_TRACE)
						trace.removeFirst();
					
					if (!trace.isEmpty()) {
						g2d.setStroke(STROKE);
						int inc = 255/MAX_TRACE, alpha = (MAX_TRACE - trace.size()) * inc;
						Iterator<Point> it = trace.iterator();
						Point lp = it.next();
						while (it.hasNext()) {
							Point p = it.next();
							g2d.setColor(new Color(0,0,255,alpha));
							g2d.drawLine(lp.x, lp.y, p.x, p.y);
							lp = p;
							alpha += inc;
						}
						g2d.setColor(Color.blue);
						g2d.drawLine(lp.x, lp.y, x, y);
					}
				
					g2d.setColor(Color.blue);
					g2d.fillOval(x-DIAMETER/2, y-DIAMETER/2, DIAMETER, DIAMETER);
					
					trace.addLast(new Point(x,y));
				} catch (NullPointerException e) {
					// cursor can be null because of UpdateTask, no problem
					trace.clear();
				}
			} else {
				trace.clear();
			}
		}
	}
	
	private void enableScreenBoxes(boolean enabled) {
		
		for (ScreenBox b : screenBoxes)			
			b.setEnabled(enabled);
		
//		if (!enabled) {
//			for (ScreenBox b : screenBoxes)			
//				b.setEnabled(false);
//		} else {
//			LinkedList<ScreenBox> list = new LinkedList<ScreenBox>();
//
//			// enable all screens
//			for (ScreenBox sb : screenBoxes) {
//				sb.setEnabled(true);
//				if (sb.isSelected()) list.add(sb);
//			}
//
//			// disallow deselection if only screen left
//			if (list.size() == 1)
//				list.getFirst().setEnabled(false);
//		}
	}
	
	public void irWarped(Map<Wiimote, IRDot[]> data, Point[] warped) {
		cursor = warped[0];
	}
	public void batteryLevel(Wiimote wiimote, double level) {}
	public void irLights(Wiimote wiimote, IRDot[] lights) {}
	public void wiimoteConnected(Wiimote wiimote) {}
	public void wiimoteDisconnected(Wiimote wiimote) {}	
	

//	private class ScreenLabel extends JLabel {
//		
//		private boolean selected;
//		private GraphicsDevice screen;
//		
//		public ScreenLabel(GraphicsDevice screen, int i, Rectangle b, boolean selected) {
//			super(String.format("<html><center>Screen %d<br><small>%d x %d</small></center></html>", i+1, (int)screen.getDefaultConfiguration().getBounds().getWidth(), (int)screen.getDefaultConfiguration().getBounds().getHeight()));
//			this.screen = screen;
//			setSelected(selected);
//			setBounds(b);
//			setOpaque(true);
//			setBorder(BorderFactory.createLineBorder(SystemColor.windowBorder));
//			setHorizontalAlignment(SwingConstants.CENTER);
//
//			final ScreenLabel thisLabel = this;
//			addMouseListener(new MouseAdapter() {
//				@Override
//				public void mousePressed(MouseEvent e) {
//					if (thisLabel.isEnabled() && !thisLabel.isSelected()) {
//						calibration.setScreen(thisLabel.screen);
//						thisLabel.setSelected(true);
//						repaint();
//						for (ScreenLabel l : screenLabels) {
//							if (thisLabel != l) {
//								l.setSelected(false);
//								l.repaint();
//							}
//						}						
//					}
//				}
//			});
//		}
//		
//		@Override
//		public void setEnabled(boolean enabled) {
//			super.setEnabled(enabled);
//			setForeground(enabled ? SystemColor.textText : SystemColor.textInactiveText);
//		}
//		
//		public void setSelected(boolean selected) {
//			this.selected = selected;
//			setBackground(selected ? SystemColor.textHighlight : SystemColor.window);
//		}
//		
//		public boolean isSelected() {
//			return selected;
//		}
//		
//	}

}
