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

package org.uweschmidt.wiimote.whiteboard.preferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.gui.PreferencesWindow;
import org.uweschmidt.wiimote.whiteboard.mouse.DefaultControlStrategy;
import org.uweschmidt.wiimote.whiteboard.mouse.rightclick.DefaultRightClick;
import org.uweschmidt.wiimote.whiteboard.mouse.rightclick.DragRightClick;
import org.uweschmidt.wiimote.whiteboard.mouse.rightclick.RightClickStrategy;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.AdaptiveExponentialSmoothing;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.MouseSmoothingStrategy;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.NoSmoothing;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.SimpleMovingAverage;
import org.uweschmidt.wiimote.whiteboard.util.Util;

import wiiremotej.IRSensitivitySettings;

public class WWPreferences {
	
	public static class LocaleWrapper {
		private final Locale locale;
		private final String name;
		public LocaleWrapper(Locale locale, String name) {
			this.locale = locale;
			this.name = name;
		}
//		public Locale getLocale() {
//			return locale;
//		}
		public String getLocaleString() {
			return locale != null ? locale.toString() : "";
		}
		@Override
		public String toString() {
			return name != null ? name : Util.getResourceMap(PreferencesWindow.class).getString("default");
		}
	}
	
	// TODO don't forget to update the ant build.xml script when a new language is added
	public static final LocaleWrapper[] LANGUAGES = {
		new LocaleWrapper(null, null),
		new LocaleWrapper(Locale.GERMAN, "Deutsch"),
		new LocaleWrapper(Locale.ENGLISH, "English"),
		new LocaleWrapper(new Locale("es"), "Espa\u00f1ol"),		
		new LocaleWrapper(Locale.FRENCH, "Fran\u00e7ais"),
		new LocaleWrapper(new Locale("pt"), "Portugu\u00eas"),
	};
	
	public static final int WIIMOTES;// = 2;	
	public static final int PIXEL_MOVE_TOLERANCE;
	public static final int SHORT_DELAY;
	public static final int MOUSE_PRESS_DELAY;
	
	// max sensitivity according to http://wiibrew.org/index.php?title=Wiimote#Sensitivity_Settings 
	private static final byte[] MAX_SENSITIVITY_BLOCK1 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x90, 0x00, 0x41 };
	private static final byte[] MAX_SENSITIVITY_BLOCK2 = new byte[] { 0x40, 0x00 };
	public static final IRSensitivitySettings SENSITIVITY_SETTINGS;// = new IRSensitivitySettings(MAX_SENSITIVITY_BLOCK1, MAX_SENSITIVITY_BLOCK2);
	
	public static final RightClickStrategy RIGHT_CLICK_STRATEGY;
	public static final List<String> WIIMOTE_BT_ADDRESSES;
	
	static {		

		int tolerance = -1;
		try {
			tolerance = Integer.parseInt(System.getProperty("org.uweschmidt.wiimote.whiteboard.pixelMoveTolerance"));
		} catch (Exception e) {}
		PIXEL_MOVE_TOLERANCE = tolerance < 0 ? 10 : tolerance;

		int delay = -1;
		try {
			delay = Integer.parseInt(System.getProperty("org.uweschmidt.wiimote.whiteboard.shortDelay"));
		} catch (Exception e) {}
		SHORT_DELAY = delay < 0 ? 200 : delay;

		int mouseDelay = -1;
		try {
			mouseDelay = Integer.parseInt(System.getProperty("org.uweschmidt.wiimote.whiteboard.mousePressDelay"));
		} catch (Exception e) {}
		MOUSE_PRESS_DELAY = mouseDelay < 0 ? 0 : mouseDelay;
		
		IRSensitivitySettings sensitivity = new IRSensitivitySettings(MAX_SENSITIVITY_BLOCK1, MAX_SENSITIVITY_BLOCK2);
		try {
			String str = System.getProperty("org.uweschmidt.wiimote.whiteboard.sensitivity");
			if (str != null) {
				if (str.equals("wii1"))
					sensitivity = IRSensitivitySettings.WII_LEVEL_1;
				else if (str.equals("wii2"))
					sensitivity = IRSensitivitySettings.WII_LEVEL_2;
				else if (str.equals("wii3"))
					sensitivity = IRSensitivitySettings.WII_LEVEL_3;
				else if (str.equals("wii4"))
					sensitivity = IRSensitivitySettings.WII_LEVEL_4;
				else if (str.equals("wii5"))
					sensitivity = IRSensitivitySettings.WII_LEVEL_5;
//				else if (str.equals("max"))
//					;
			}
		} catch (Exception e) {}
		SENSITIVITY_SETTINGS = sensitivity;
		
		int wiimotes = 0;
		try {
			wiimotes = Integer.parseInt(System.getProperty("org.uweschmidt.wiimote.whiteboard.wiimotes"));
		} catch (Exception e) {}
		WIIMOTES = wiimotes == 1 || wiimotes == 2 ? wiimotes : 2;
		
		List<String> addresses = Collections.emptyList();
		try {
			String str = System.getProperty("org.uweschmidt.wiimote.whiteboard.wiimoteAddresses");
			if (str != null) {
				addresses = Collections.unmodifiableList(Arrays.asList(str.split(":")));
			}
		} catch (Exception e) {}
		WIIMOTE_BT_ADDRESSES = addresses;
		
		RightClickStrategy rcs = new DefaultRightClick();
		try {
			String str = System.getProperty("org.uweschmidt.wiimote.whiteboard.rightClick");
			if (str != null && str.equals("drag")) {
				rcs = new DragRightClick();
			}
		} catch (Exception e) {}
		RIGHT_CLICK_STRATEGY = rcs;
	}
	
	private static final String RIGHT_CLICK_DELAY = "rightClickDelay";
	private static final String RIGHT_CLICK = "rightClick";
	private static final String LEFT_CLICK = "leftClick";
	private static final String LOW_BATTERY_WARNING = "lowBatteryWarning";
	private static final String CHECK_FOR_UPDATES = "checkForUpdates";
//	private static final String MOUSE_MOVE_THRESHOLD = "mouseMoveThreshold";
	private static final String MOUSE_SMOOTHING = "mouseSmoothing";
	private static final String TUIO_ENABLED = "tuioEnabled";
	private static final String TUIO_PORT = "tuioPort";
	private static final String TUIO_HOST = "tuioHost";
//	private static final String NUMBER_OF_WIIMOTES = "numberOfWiimotes";
//	private static final String BLUETOOTH_ADDRESS = "bluetoothAddress";
	private static final String CURSOR_CONTROL = "cursorControl";
	private static final String ASSIST_DOUBLE_CLICKS = "assistDoubleClicks";
	private static final String LANGUAGE = "language";
	
	public static interface PreferencesListener {
		public void preferencesChanged();
	}	
	
	private final Set<PreferencesListener> listener = new HashSet<PreferencesListener>();
	private final static WWPreferences instance = new WWPreferences();	
	private final Preferences settings;
	
	private WWPreferences() {
		settings = Preferences.userNodeForPackage(WiimoteWhiteboard.class);
		MouseSmoothingStrategy.REGISTERED.put(NoSmoothing.NAME, NoSmoothing.class);
		MouseSmoothingStrategy.REGISTERED.put(SimpleMovingAverage.NAME, SimpleMovingAverage.class);
	}
	
	public static WWPreferences getPreferences() {
		return instance;
	}
	
	public void reset() {
		try {
			settings.clear();
			notifyListener();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * LISTENER STUFF
	 */
	
	public void addPreferencesListener(PreferencesListener l) {
		listener.add(l);
	}
	
	public void removePreferencesListener(PreferencesListener l) {
		listener.remove(l);
	}
	
	private void notifyListener() {
		for (Object l : listener.toArray())
			((PreferencesListener)l).preferencesChanged();
	}
	
	/*
	 * GETTER & SETTER
	 */	
	
	public boolean isLeftClick() {
		return settings.getBoolean(LEFT_CLICK, true);
	}

	public void setLeftClick(boolean leftClick) {
		settings.putBoolean(LEFT_CLICK, leftClick);
		notifyListener();
	}

	public boolean isRightClick() {
		return settings.getBoolean(RIGHT_CLICK, true);
	}

	public void setRightClick(boolean rightClick) {
		settings.putBoolean(RIGHT_CLICK, rightClick);
		notifyListener();
	}

	public long getRightClickDelay() {
		return settings.getLong(RIGHT_CLICK_DELAY, 1000L);
	}

	public void setRightClickDelay(long rightClickDelay) {
		settings.putLong(RIGHT_CLICK_DELAY, rightClickDelay);
		notifyListener();
	}
	
	public boolean isLowBatteryWarning() {
		return settings.getBoolean(LOW_BATTERY_WARNING, true);
	}
	
	public void setLowBatteryWarning(boolean lowBatteryWarning) {
		settings.putBoolean(LOW_BATTERY_WARNING, lowBatteryWarning);
		notifyListener();
	}
	
	public boolean checkForUpdates() {
		return settings.getBoolean(CHECK_FOR_UPDATES, true);
	}
	
	public void setCheckForUpdates(boolean checkForUpdates) {
		settings.putBoolean(CHECK_FOR_UPDATES, checkForUpdates);
		notifyListener();
	}
	
//	public int getMouseMoveThreshold() {
//		return settings.getInt(MOUSE_MOVE_THRESHOLD, 1);
//	}
//	
//	public void setMouseMoveThreshold(int mouseMoveThreshold) {
//		settings.putInt(MOUSE_MOVE_THRESHOLD, mouseMoveThreshold);
//		notifyListener();
//	}
	
	public String getMouseSmoothing() {
		return settings.get(MOUSE_SMOOTHING, AdaptiveExponentialSmoothing.class.getName());
	}

	public void setMouseSmoothing(String mouseSmoothing) {
		if (!getMouseSmoothing().equals(mouseSmoothing)) {
			settings.put(MOUSE_SMOOTHING, mouseSmoothing);
			notifyListener();
		}
	}
	
	public boolean isTuioEnabled() {
		return settings.getBoolean(TUIO_ENABLED, false);
	}
	
	public void setTuioEnabled(boolean tuioEnabled) {
		settings.putBoolean(TUIO_ENABLED, tuioEnabled);
		notifyListener();
	}
	
	public int getTuioPort() {
		return settings.getInt(TUIO_PORT, 3333);
	}
	
	public void setTuioPort(int tuioPort) {
		settings.putInt(TUIO_PORT, tuioPort);
	}
	
	public String getTuioHost() {
		return settings.get(TUIO_HOST, "localhost");
	}
	
	public void setTuioHost(String tuioHost) {
		settings.put(TUIO_HOST, tuioHost);
	}
	
//	public int getNumberOfWiimotes() {
//		return settings.getInt(NUMBER_OF_WIIMOTES, 1);
//	}
//	
//	public void setNumberOfWiimotes(int numberOfWiimotes) {
//		if (getNumberOfWiimotes() != numberOfWiimotes) {
//			settings.putInt(NUMBER_OF_WIIMOTES, numberOfWiimotes);
//			notifyListener();
//		}
//	}
	
	public String getCursorControl() {
		return settings.get(CURSOR_CONTROL, DefaultControlStrategy.class.getName());
	}

	public void setCursorControl(String cursorControl) {
		settings.put(CURSOR_CONTROL, cursorControl);
		notifyListener();
	}
	
	public boolean assistDoubleClicks() {
		return settings.getBoolean(ASSIST_DOUBLE_CLICKS, false);
	}
	
	public void setAssistDoubleClicks(boolean assistDoubleClicks) {
		settings.putBoolean(ASSIST_DOUBLE_CLICKS, assistDoubleClicks);
		notifyListener();
	}
	
	public void setLanguage(String language) {
		if (!getLanguage().equals(language)) {
			settings.put(LANGUAGE, language);
			notifyListener();
		}
	}
	
	public String getLanguage() {
		return settings.get(LANGUAGE, "");
	}

	
//	public void setBluetoothAddress(String address) {
//		settings.put(BLUETOOTH_ADDRESS, address);
//	}
//	
//	public String getBluetoothAddress() {
//		return settings.get(BLUETOOTH_ADDRESS, null);
//	}

}
