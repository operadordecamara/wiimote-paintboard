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

package org.uweschmidt.wiimote.whiteboard.tuio;

import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler.WiimoteDataListener;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration.CalibrationEvent;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration.CalibrationEventListener;
import org.uweschmidt.wiimote.whiteboard.ds.IRDot;
import org.uweschmidt.wiimote.whiteboard.ds.Wiimote;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences.PreferencesListener;

import de.sciss.net.OSCBundle;
import de.sciss.net.OSCClient;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCServer;

public class TuioTransmitter implements PreferencesListener, WiimoteDataListener, CalibrationEventListener {

	private static final OSCMessage SOURCE_PACKET = new OSCMessage("/tuio/2Dcur", new Object[] { "source", WiimoteWhiteboard.getProperty("id") });
	
	private int fseq;
	private int sseq;
	private int[] sessions;
	private Point[] last;
	private float[] lastSpeed = {0f,0f,0f,0f};
	private OSCClient trans = null;
	private int port = -1;
	private String host = null;
	
	private Rectangle bounds;
	private WiimoteDataHandler dh;
	private WiimoteCalibration calibration;
	private static final WWPreferences prefs = WWPreferences.getPreferences();

	public TuioTransmitter(WiimoteDataHandler dh, WiimoteCalibration calibration) {
		this.dh = dh;
		this.calibration = calibration;
		calibration.addCalibrationEventListener(this);
		prefs.addPreferencesListener(this);
		update();
	}
	
	private void update() {
		try {
			if (prefs.isTuioEnabled()) {
				if (trans == null || prefs.getTuioPort() != port || !prefs.getTuioHost().equals(host)) {
					if (trans != null)
						trans.stop();
					
					fseq = 0;
					sseq = 0;
					sessions = new int[4];
					last = new Point[4];
					port = prefs.getTuioPort();
					host = prefs.getTuioHost();
					getScreenSize();

					trans = OSCClient.newUsing(OSCServer.UDP);
					trans.setTarget(new InetSocketAddress(host, port));
					trans.start();
					dh.addWiimoteDataListener(this);
				}
			} else {
				dh.removeWiimoteDataListener(this);
				if (trans != null)
					trans.stop();
				trans = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			WiimoteWhiteboard.getLogger().log(Level.WARNING, "TUIO transmission error.", e);
		}
	}
	
	public void preferencesChanged() {
		update();
	}
	
	public void calibrationEvent(CalibrationEvent e) {
		if (e == CalibrationEvent.SCREEN_CHANGED) {
			getScreenSize();
		}
	}

	private void getScreenSize() {
		GraphicsDevice screen = calibration.getScreen();
		if (screen != null)
			bounds = screen.getDefaultConfiguration().getBounds();
	}
	
	// implementation rationale:
	// - http://www.tuio.org/specs.html
	// - http://www.adrienm.net/emotion/forum/viewtopic.php?f=3&t=20&st=0&sk=t&sd=a&start=10
	public void irWarped(Map<Wiimote, IRDot[]> data, Point[] points) {
		if (trans == null) return;
		
		// return if all lights are off now and were previously
		if ((points[0] == null && points[1] == null && points[2] == null && points[3] == null) &&
			(last[0] == null && last[1] == null && last[2] == null && last[3] == null))
			return;
		
		OSCBundle b = new OSCBundle();
		
		// (1) source name
		b.addPacket(SOURCE_PACKET);

		// (2) alive sessions
		List<Object> alive = new ArrayList<Object>();
		alive.add("alive");
		for (int i = 0; i < 4; i++) {
			if (points[i] != null) {
				if (last[i] == null)
					sessions[i] = ++sseq;
				alive.add(sessions[i]);
			}
		}
		b.addPacket(new OSCMessage("/tuio/2Dcur", alive.toArray()));

		// (3) cursor data
		for (int i = 0; i < 4; i++) {
			Point p = points[i];
			Point l = last[i] != null ? last[i] : p;
			if (p != null) {
				p.translate(-bounds.x, -bounds.y);
				// "l" is the last point and already translated, or p itself if not available
				float px = (float)Math.max(0, Math.min(p.x, bounds.width)) / bounds.width;
				float py = (float)Math.max(0, Math.min(p.y, bounds.height)) / bounds.height;
				float lx = (float)Math.max(0, Math.min(l.x, bounds.width)) / bounds.width;
				float ly = (float)Math.max(0, Math.min(l.y, bounds.height)) / bounds.height;
				float speed = (float)Math.sqrt(Math.pow(px - lx, 2) + Math.pow(py - ly, 2));
				b.addPacket(new OSCMessage("/tuio/2Dcur", new Object[] {
						"set",
						// s: session id
						sessions[i],
						// x, y: normalized position, range 0..1
						px, py,
						// X, Y: normalized motion speed (dt = 1)
						px - lx, py - ly,
						// m: normalized motion acceleration (dt = 1)
						speed - lastSpeed[i]
				}));
				lastSpeed[i] = speed;
			} else {
				lastSpeed[i] = 0f;
			}
			last[i] = p;
		}
		
		// (4) frame sequence number
		b.addPacket(new OSCMessage("/tuio/2Dcur", new Object[] { "fseq", fseq++ }));
		
		try {
//			OSCPacket.printTextOn(System.out, b);
			trans.send(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void batteryLevel(Wiimote wiimote, double level) {
	}
	
	public void irLights(Wiimote wiimote, IRDot[] lights) {
	}
	
	public void wiimoteConnected(Wiimote wiimote) {
	}
	
	public void wiimoteDisconnected(Wiimote wiimote) {
	}

}
