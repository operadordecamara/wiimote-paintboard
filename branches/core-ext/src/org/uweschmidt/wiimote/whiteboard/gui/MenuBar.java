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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.jdesktop.application.Application;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class MenuBar extends JMenuBar {
	
	public MenuBar(PreferencesWindow pf, AboutWindow af, HelpHandler hh, LogWindow lw) {
		JMenu menu;
		JMenuItem item;
		
		if (!Util.MAC_OS_X) {
			menu = new JMenu(Util.getResourceMap(MenuBar.class).getString("editMenu"));
			menu.add(new JMenuItem(Util.getAction(pf, "preferences")));
			add(menu);
		}
		
		menu = new JMenu(Util.getResourceMap(HelpHandler.class).getString("help"));
		
		menu.add(item = new JMenuItem(Util.getAction(hh, "help")));
		if (Util.MAC_OS_X)
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, KeyEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		menu.addSeparator();

		if (!Util.MAC_OS_X)
			menu.add(new JMenuItem(Util.getAction(af, "about")));
		
		menu.add(new JMenuItem(Util.getAction(lw, "log")));
		
		menu.addSeparator();
		
		menu.add(new JMenuItem(Util.getAction(Application.getInstance(WiimoteWhiteboard.class), "donate")));

		add(menu);
	}

}
