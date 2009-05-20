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

import javax.swing.JOptionPane;

import org.jdesktop.application.Action;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.util.BareBonesBrowserLaunch;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class HelpHandler {
	
	@Action
	public void help() {
		if (Util.INSIDE_APP_BUNDLE) {
			HelpBook.launchHelpViewer();
		} else {
			if (JOptionPane.showConfirmDialog(null, Util.getResourceMap(HelpHandler.class).getString("helpQuestion"), Util.getResourceMap(HelpHandler.class).getString("help.Action.text"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				BareBonesBrowserLaunch.openURL(WiimoteWhiteboard.getProperty("onlineHelp"));
			}
		}
	}

}
