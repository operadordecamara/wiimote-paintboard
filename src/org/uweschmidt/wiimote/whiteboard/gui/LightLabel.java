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
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.uweschmidt.wiimote.whiteboard.ds.IRDot;

@SuppressWarnings("serial")
public class LightLabel extends JLabel {
	
	private static final int RADIUS = 10;
	
	private final int id, number;
	private Point2D[][] lights;
	private JPanel canvas;

	public LightLabel(JPanel canvas, Point2D[][] lights, int id, int number) {
		this(canvas, lights, id, number, WiimoteIcon.COLORS[id - 1]);
	}

	public LightLabel(JPanel canvas, Point2D[][] lights, int id, int number, Color bg) {
		super(String.valueOf(number));
		this.id = id;
		this.number = number;
		this.lights = lights;
		this.canvas = canvas;
		// setOpaque(true);
		setHorizontalAlignment(SwingConstants.CENTER);
		// setBorder(BorderFactory.createLineBorder(Color.black));
		setForeground(Color.black);
		setBackground(bg);
	}

	public void update() {
		Point2D l = lights[id][number - 1];
		if (l != null) {
			int x = (int) Math.round(l.getX() * canvas.getWidth());
			int y = canvas.getHeight() - (int) Math.round(l.getY() * canvas.getHeight());
			this.setBounds(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
			setVisible(true);
		} else {
			setVisible(false);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (lights[id][number - 1] != null) {
			g2d.setColor(this.getBackground());
			g2d.fillOval(1, 1, RADIUS * 2 - 2, RADIUS * 2 - 2);
			g2d.setColor(this.getForeground());
			g2d.drawOval(1, 1, RADIUS * 2 - 2, RADIUS * 2 - 2);

			// XXX explain cyan oval for size
			double scale = lights[id][number - 1] instanceof IRDot ? ((IRDot)lights[id][number - 1]).getSize() : -1;
			if (scale != -1) {
				g2d.setColor(Color.cyan);
				final int d = (int)Math.round(RADIUS * scale * 10);
				g2d.fillOval(RADIUS - d/2, RADIUS - d/2, d, d);
			}
		}
		super.paintComponent(g);
	}
}