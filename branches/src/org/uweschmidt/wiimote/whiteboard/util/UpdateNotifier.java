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

package org.uweschmidt.wiimote.whiteboard.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JOptionPane;

import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;

public class UpdateNotifier {
	
	public static void checkForUpdate(String version) {
		try {
			URL url = new URL(WiimoteWhiteboard.getProperty("updateURL"));
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			final String current = in.readLine();
			if (compare(version, current))
				showUpdateNotification(version, current);
			in.close();
		} catch (Exception e) {
			// ignore
		}
	}
	
	private static void showUpdateNotification(String program, String current) throws Exception {
		String question = Util.getResourceMap(UpdateNotifier.class).getString("updateQuestion", WiimoteWhiteboard.getProperty("id"));
		String title = Util.getResourceMap(UpdateNotifier.class).getString("updateTitle", WiimoteWhiteboard.getProperty("id"), current);
		int response = JOptionPane.showConfirmDialog(null, question, title, JOptionPane.YES_NO_OPTION);
		if (response == JOptionPane.YES_OPTION) {
			BareBonesBrowserLaunch.openURL(WiimoteWhiteboard.getProperty("homepage"));
		}
	}
	
	private static boolean compare(String program, String current) throws Exception {
		String[] psplit = program.split("\\."), csplit = current.split("\\.");
		int[] p = new int[3], c = new int[3];
		for (int i = 0; i < 3; i++) {
			p[i] = Integer.valueOf(psplit[i]);
			c[i] = Integer.valueOf(csplit[i]);
		}
		if (c[0] > p[0]) return true;
		if (c[0] == p[0]) {
			if (c[1] > p[1]) return true;
			if (c[1] == p[1]) {
				if (c[2] > p[2]) return true;
			}
		}
		return false;
	}

}
