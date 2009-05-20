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

package org.uweschmidt.wiimote.whiteboard;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.uweschmidt.wiimote.whiteboard.calibration.CalibrationPersistence;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration.CalibrationEvent;
import org.uweschmidt.wiimote.whiteboard.gui.AboutWindow;
import org.uweschmidt.wiimote.whiteboard.gui.HelpHandler;
import org.uweschmidt.wiimote.whiteboard.gui.LogWindow;
import org.uweschmidt.wiimote.whiteboard.gui.MainPanel;
import org.uweschmidt.wiimote.whiteboard.gui.MenuBar;
import org.uweschmidt.wiimote.whiteboard.gui.PreferencesWindow;
import org.uweschmidt.wiimote.whiteboard.mouse.Mouse;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.tuio.TuioTransmitter;
import org.uweschmidt.wiimote.whiteboard.util.BareBonesBrowserLaunch;
import org.uweschmidt.wiimote.whiteboard.util.UpdateNotifier;
import org.uweschmidt.wiimote.whiteboard.util.Util;
import org.uweschmidt.wiimote.whiteboard.util.WiiRemoteJErrorHandler;

import wiiremotej.WiiRemoteJ;
import apple.dts.samplecode.osxadapter.OSXAdapter;

public class WiimoteWhiteboard extends SingleFrameApplication {
	
	public static void main(String args[]) {
		if (Util.MAC_OS_X && !Util.INSIDE_APP_BUNDLE) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", getProperty("id"));
		}

		String lang = WWPreferences.getPreferences().getLanguage();
		if (lang.length() > 0) {
			String[] langParts = lang.split("_");
			switch (langParts.length) {
				case 1:
					Locale.setDefault(new Locale(langParts[0]));
					break;
				case 2:
					Locale.setDefault(new Locale(langParts[0], langParts[1]));
					break;
			}
		}
		Application.launch(WiimoteWhiteboard.class, args);
	}
		
	@Override
	protected void startup() {
		try {
			new Thread(new Runnable() {
				public void run() {
					// blocking
					if (WWPreferences.getPreferences().checkForUpdates())
						UpdateNotifier.checkForUpdate(getProperty("version"));
				}
			}).start();
			
			Logger.getLogger("wiimotewhiteboard").setUseParentHandlers(false);

			final JFrame f = getMainFrame();
			LogWindow lw = new LogWindow();
			
			final WiimoteCalibration calibration = new WiimoteCalibration();
			WiimoteDataHandler dh = new WiimoteDataHandler(calibration);
			
//			new IRDotLogger(dh);
			
			MainPanel mp = new MainPanel(dh, calibration);
			AboutWindow af = new AboutWindow();
			HelpHandler hh = new HelpHandler();
			PreferencesWindow pf = new PreferencesWindow(mp, hh);
			f.setJMenuBar(new MenuBar(pf, af, hh, lw));
			registerForMacOSXEvents(pf, af);
			
			// update Mouse's screen
			calibration.addCalibrationEventListener(new WiimoteCalibration.CalibrationEventListener() {
				public void calibrationEvent(CalibrationEvent e) {
					if (e == CalibrationEvent.SCREEN_CHANGED)
					Mouse.setScreen(calibration.getScreen());
				}
			});

			// add persistence as last listener because it indirectly generates new calibration events
			new CalibrationPersistence(calibration);
			// TODO save last used screen?
			calibration.setScreen(WiimoteCalibration.DEFAULT_SCREEN);
			
			new TuioTransmitter(dh, calibration);
			
			WiiRemoteJ.setConsoleLoggingErrors();
			Logger.getLogger("wiiremotej").setLevel(Level.ALL);
			Logger.getLogger("wiiremotej").addHandler(new WiiRemoteJErrorHandler(dh));
			
			try {
				// restore session manually before 'show' on the main panel is
				// called. this is a fix to the problem that the session is not
				// restored; apparently because mainFrame is not resizable
				getContext().getSessionStorage().restore(f, "mainFrame.session.xml");
			} catch (Exception e) {}
			
			show(mp);
			// f.pack() is called in 'show' above if f.isValid() == false
			f.pack();
			
		} catch (Exception e) {
			e.printStackTrace();
			getLogger().log(Level.SEVERE, "Error on startup", e);
			JOptionPane.showMessageDialog(null, e.getMessage(), getProperty("id"), JOptionPane.ERROR_MESSAGE);
			exit();
		}
	}
	
	/*
	 * MAC OS X HOOKS
	 */	
	private void registerForMacOSXEvents(PreferencesWindow pf, AboutWindow af) {
		if (Util.MAC_OS_X) {
	        try {
				OSXAdapter.setQuitHandler(this, WiimoteWhiteboard.class.getDeclaredMethod("quitApp", (Class[])null));				
				if (!Util.INSIDE_APP_BUNDLE) {
					OSXAdapter.setAboutHandler(af, AboutWindow.class.getDeclaredMethod("about", (Class[])null));
				}
				OSXAdapter.setPreferencesHandler(pf, PreferencesWindow.class.getDeclaredMethod("preferences", (Class[])null));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Action
	public boolean quitApp() {
		exit();
		return false;
	}
	
	@Action
	public void donate() {
		BareBonesBrowserLaunch.openURL(getProperty("donateURL"));
	}
	
	public static String getProperty(String key) {
		return Util.getResourceMap(WiimoteWhiteboard.class).getString("Application."+key);
	}
	
	public static Logger getLogger() {
		return Logger.getLogger("wiimotewhiteboard");
	}
	
}
