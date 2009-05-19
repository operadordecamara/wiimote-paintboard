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

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.jdesktop.application.Application;
import org.jdesktop.application.Application.ExitListener;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration;
import org.uweschmidt.wiimote.whiteboard.ds.IRDot;
import org.uweschmidt.wiimote.whiteboard.ds.Wiimote;
import org.uweschmidt.wiimote.whiteboard.mouse.CursorControlStrategy;
import org.uweschmidt.wiimote.whiteboard.mouse.Mouse;
import org.uweschmidt.wiimote.whiteboard.mouse.smoothing.MouseSmoothingStrategy;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences.PreferencesListener;

import wiiremotej.WiiRemote;
import wiiremotej.WiiRemoteJ;
import wiiremotej.event.WRButtonEvent;
import wiiremotej.event.WRIREvent;
import wiiremotej.event.WRStatusEvent;
import wiiremotej.event.WiiDeviceDiscoveredEvent;
import wiiremotej.event.WiiDeviceDiscoveryListener;
import wiiremotej.event.WiiRemoteAdapter;

public class WiimoteDataHandler extends WiiRemoteAdapter implements ExitListener, WiiDeviceDiscoveryListener, PreferencesListener {
	
	public static interface WiimoteDataListener {
		public void wiimoteConnected(Wiimote wiimote);
		public void wiimoteDisconnected(Wiimote wiimote);
		public void irLights(Wiimote wiimote, IRDot[] lights);
		public void irWarped(Map<Wiimote, IRDot[]> data, Point[] warped);
		public void batteryLevel(Wiimote wiimote, double level);
	}
	
	private Map<WiiRemote, Wiimote> remotes = new LinkedHashMap<WiiRemote, Wiimote>(WWPreferences.WIIMOTES, 1f);
	private Map<WiiRemote, WRIREvent> events = new LinkedHashMap<WiiRemote, WRIREvent>(WWPreferences.WIIMOTES, 1f);
	private final WiimoteCalibration calibration;
	private static final WWPreferences prefs = WWPreferences.getPreferences();
	private final Set<WiimoteDataListener> listener = Collections.synchronizedSet(new HashSet<WiimoteDataListener>());

	private boolean cursorControl = true;
	
	private MouseSmoothingStrategy mss[] = new MouseSmoothingStrategy[4];
	private CursorControlStrategy cursorControlStrategy;
	
	public WiimoteDataHandler(WiimoteCalibration calibration) {
		this.calibration = calibration;		
		Application.getInstance().addExitListener(this);
		prefs.addPreferencesListener(this);
		preferencesChanged();
		new WiimoteConnector(this).connect();
//		WiiRemoteJ.findRemotes(this, WWPreferences.WIIMOTES);
	}
	
	public void enableIR(Wiimote wiimote) throws Exception {
		if (wiimote != null && wiimote.getWiiRemote().isConnected()) {
			wiimote.getWiiRemote().setIRSensorEnabled(true, WRIREvent.BASIC, WWPreferences.SENSITIVITY_SETTINGS);
		}
		WiimoteWhiteboard.getLogger().info(String.format("(Re-)Setting IR sensor of Wiimote %d: %s", wiimote.getId(), (wiimote != null && wiimote.getWiiRemote().isConnected() ? "done" : "not connected")));
	}
	
	void addRemote(final WiiRemote remote) {
		try {
			int id = remotes.size()+1;
			final Wiimote wiimote = new Wiimote(remote, remote.getBluetoothAddress(), id);
			remotes.put(remote, wiimote);
			remote.setAccelerometerEnabled(false);
//			remote.setIRSensorEnabled(true, WRIREvent.BASIC, SENSITIVITY_BLOCK1, SENSITIVITY_BLOCK2);
			enableIR(wiimote);
			remote.setLEDIlluminated(id-1, true);			
			remote.setUseMouse(false);
			
			synchronized (listener) {
				for (WiimoteDataListener l : listener)
					l.wiimoteConnected(wiimote);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			WiimoteWhiteboard.getLogger().log(Level.SEVERE, "Error on configuring Wii Remote", e);
		}
		
		remote.addWiiRemoteListener(this);

		// update battery level every minute
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						// triggers #statusReported(WRStatusEvent)
						if (remote.isConnected())
							remote.requestStatus();
					} catch (Exception e) {
						e.printStackTrace();
						WiimoteWhiteboard.getLogger().log(Level.WARNING, "Error on requesting status from Wii Remote", e);
					}
					try {
						Thread.sleep(60 * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();		
	}
	

	/*
	 * EXIT
	 */
	
	public void willExit(EventObject event) {
		for (WiiRemote remote : remotes.keySet())
			remote.disconnect();
	}
	
	public boolean canExit(EventObject event) {
		return true;
	}

	
	/*
	 * WIIREMOTEJ LISTENER
	 */
	
	@Override
	public synchronized void IRInputReceived(WRIREvent e) {
		events.put(e.getSource(), e);
		// wait till data from all connected wiimotes was received once and only process input on data from first wiimote to reduce the number of times the function gets called
		if (events.size() == getNumberOfConnectedWiimotes() && remotes.get(e.getSource()).getId() == 1)
			IRInputReceived();
	}
	
	private void IRInputReceived() {
		boolean firstDotVisible = false;
		Map<Wiimote, IRDot[]> data = new LinkedHashMap<Wiimote, IRDot[]>();
		for (WiiRemote r : events.keySet()) {
			Wiimote wiimote = remotes.get(r);
			IRDot[] dots = IRDot.getIRDots(events.get(r).getIRLights());
			
			synchronized (listener) {
				for (WiimoteDataListener l : listener)
					l.irLights(wiimote, dots);
			}

			// exclude points from uncalibrated wiimotes during "normal operation"
			if (!calibration.isDone() || calibration.isCalibrated(wiimote)) {
				firstDotVisible = firstDotVisible || dots[0] != null;
				data.put(wiimote, dots);
			}
		}

		if (calibration.isDone()) {
			// should always be true, but just in case...
			if (calibration.isAnyCalibrated(data.keySet())) {
				Point warped[] = calibration.warp(data);
				for (int i = 0; i < 4; i++) {
					if (warped[i] != null) {
						warped[i] = mss[i].translate(warped[i]);
					} else {
						mss[i].reset();
					}
				}
				
				if (isCursorControl()) {
					cursorControlStrategy.process(warped[0]);
				} else {
//					if (Mouse.LEFT_BUTTON.isPressed())
						Mouse.LEFT_BUTTON.setPressed(false);
//					if (Mouse.RIGHT_BUTTON.isPressed())
						Mouse.RIGHT_BUTTON.setPressed(false);
				}
				
//				if (warped[0] != null) {
//					// normal operation after calibration has been done
//					if (isCursorControl()) {
//						Mouse.move(warped[0]);
//						rcs.process(warped[0]);
//						if (prefs.isLeftClick() && !(prefs.isRightClick() && rcs.trigger())) {
//							Mouse.LEFT_BUTTON.setPressed(true);
//						}
//					}
//				} else {
//					rcs.process(null);
//					Mouse.LEFT_BUTTON.setPressed(false);
//				}

				synchronized (listener) {
					for (WiimoteDataListener l : listener)
						l.irWarped(data, warped);
				}

			}
		} else if (calibration.inProgress()) {
			if (firstDotVisible)
				calibration.process(data);
		}

	}
	
	@Override
	public void statusReported(WRStatusEvent e) {
		synchronized (listener) {
			for (WiimoteDataListener l : listener)
				l.batteryLevel(remotes.get(e.getSource()), e.getBatteryLevel());
		}
	}

	@Override
	public void buttonInputReceived(WRButtonEvent e) {
		if (e.isOnlyPressed(WRButtonEvent.A)) {
			calibration.start(getConnectedWiimotes());
		} else if (e.isOnlyPressed(WRButtonEvent.HOME)) {
			Application.getInstance().exit();
		}
	}
	
	public void wiiDeviceDiscovered(WiiDeviceDiscoveredEvent e) {
		if (e.getWiiDevice() instanceof WiiRemote) {
			addRemote((WiiRemote)e.getWiiDevice());
		}
	}
	
	public void findFinished(int numberFound) {
	}
	
	@Override
	public void disconnected() {
		// TODO support dis-/reconnecting?
		WiiRemoteJ.stopFind();
		WiiRemote remove = null;
		for (WiiRemote remote : remotes.keySet()) {
			if (!remote.isConnected()) {
				remove = remote;
				synchronized (listener) {
					for (WiimoteDataListener l : listener)
						l.wiimoteDisconnected(remotes.get(remote));
				}
				break;
			}
		}
		if (remove != null) remotes.remove(remove);
	}	
	
	
	/*
	 * LISTENER
	 */
	
	public void addWiimoteDataListener(WiimoteDataListener l) {
		listener.add(l);
	}
	
	public void removeWiimoteDataListener(WiimoteDataListener l) {
		listener.remove(l);
	}
	
	/*
	 * PREFERENCES STUFF
	 */
	
	public void preferencesChanged() {
		updateMSS();
		updateCursorControlStrategy();
//		final int newNumWiimotes = prefs.getNumberOfWiimotes();
//		if (numWiimotes != newNumWiimotes) {
//			WiiRemoteJ.stopFind();
//			if (newNumWiimotes > getNumberOfConnectedWiimotes()) {
//				WiiRemoteJ.findRemotes(this, newNumWiimotes - getNumberOfConnectedWiimotes());
//			}
//			numWiimotes = newNumWiimotes;
//		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateCursorControlStrategy() {
		if (cursorControlStrategy == null || !cursorControlStrategy.getClass().getName().equals(prefs.getCursorControl())) {
			try {
				Class<?> c = Class.forName(prefs.getCursorControl());
				cursorControlStrategy = ((Class<? extends CursorControlStrategy>)c).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				WiimoteWhiteboard.getLogger().log(Level.SEVERE, "Cursor Control Method error.", e);
			}	
		}
	}

	@SuppressWarnings("unchecked")
	private void updateMSS() {
		if (mss[0] == null || !mss[0].getClass().getName().equals(prefs.getMouseSmoothing())) {
			try {
				Class<?> c = Class.forName(prefs.getMouseSmoothing());
				for (int i = 0; i < 4; i++)
					mss[i] = ((Class<? extends MouseSmoothingStrategy>)c).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				WiimoteWhiteboard.getLogger().log(Level.SEVERE, "Mouse Movement Smoothing error.", e);
			}
		}
	}
	
	/*
	 * GETTER & SETTER
	 */
	
	public boolean isConnected() {
		for (WiiRemote remote : remotes.keySet()) {
			if (remote.isConnected()) return true;
		}
		return false;
	}
	
	public boolean isConnected(Wiimote wiimote) {
		return wiimote != null && wiimote.getWiiRemote().isConnected();
	}
	
	public int getNumberOfConnectedWiimotes() {
		return remotes.size();
	}
	
	public Collection<Wiimote> getConnectedWiimotes() {
		return remotes.values();
	}

	public boolean isCursorControl() {
		return cursorControl;
	}

	public void setCursorControl(boolean cursorControl) {
		this.cursorControl = cursorControl;
	}
	
}
