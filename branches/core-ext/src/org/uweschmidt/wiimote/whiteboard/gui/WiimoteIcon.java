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
import java.awt.SystemColor;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class WiimoteIcon extends JPanel {

	public static final Color[] COLORS = { Color.white, Color.pink, Color.orange, Color.green };
	private JLabel idLabel;

	public WiimoteIcon(int id) {
		setBackground(COLORS[id - 1]);
		setBorder(BorderFactory.createLineBorder(Color.lightGray));
		setLayout(new MigLayout("insets 3, center"));
		for (int i = 1; i <= 4; i++) {
			JLabel l = new JLabel();
			l.setOpaque(true);
			l.setBackground(SystemColor.textInactiveText);
			add(l, "w 6!, h 6!");
			if (i == id) idLabel = l;
		}
	}
	
	public void displayConnected(boolean connected) {
		idLabel.setBackground(connected ? Color.blue : SystemColor.textInactiveText);
	}

}
