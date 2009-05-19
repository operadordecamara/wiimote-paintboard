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

package org.uweschmidt.wiimote.whiteboard.mouse;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;

import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;

public enum Mouse {
	LEFT_BUTTON(InputEvent.BUTTON1_MASK), RIGHT_BUTTON(InputEvent.BUTTON3_MASK);
	
	private static final WWPreferences prefs = WWPreferences.getPreferences();
	private static Point position = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
	private static Robot r;
	private static Rectangle bounds; 
	
	private final int id;
	private long lastReleased = -1, lastPressed = -1;
	private boolean pressed;
	
	static {
		// TODO when screen changes, http://java.sun.com/j2se/1.5.0/docs/api/java/awt/Robot.html#Robot(java.awt.GraphicsDevice)
		final GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		try {
			r = new Robot(screen);
			setScreen(screen);
		} catch (AWTException e) {
			e.printStackTrace();
		} 
	}
	
	private Mouse(int id) {
		this.id = id;
	}
	
	public static void setScreen(GraphicsDevice screen) {
//		try {
//			r = new Robot(screen);
			bounds = screen.getDefaultConfiguration().getBounds();
//			System.out.printf("Mouse's new screen bounds: %s\n", bounds);
//		} catch (AWTException e) {
//			e.printStackTrace();
//		} 
	}
	
	public static void sleep(int ms) {
		r.delay(ms);
	}
	
	public static void move(Point p) {
		// negated "don't move"-condition
		if (!(prefs.assistDoubleClicks() && position.distance(p) < WWPreferences.PIXEL_MOVE_TOLERANCE && (
			(Mouse.LEFT_BUTTON.isPressed() && System.currentTimeMillis() - Mouse.LEFT_BUTTON.getLastPressed() < WWPreferences.SHORT_DELAY) ||
			(!Mouse.LEFT_BUTTON.isPressed() && System.currentTimeMillis() - Mouse.LEFT_BUTTON.getLastReleased() < WWPreferences.SHORT_DELAY)
		))) {
			r.mouseMove(Math.min(Math.max(bounds.x, p.x), bounds.x+bounds.width-1), Math.min(Math.max(bounds.y, p.y), bounds.y+bounds.height-1));
			position = p;
		}
	}
	
	public static Point getPosition() {
		return position;
	}
	
	public boolean isPressed() {
		return pressed;
	}
	
	public void setPressed(boolean pressRequest) {
		if (pressRequest && !pressed) {
			r.mousePress(id);
			lastPressed = System.currentTimeMillis();
//			WiimoteWhiteboard.getLogger().info(this + " pressed");
		} else if (!pressRequest && pressed) {
			r.mouseRelease(id);
			lastReleased = System.currentTimeMillis();
//			WiimoteWhiteboard.getLogger().info(this + " released");
		}
		this.pressed = pressRequest;
	}
	
	public long getLastPressed() {
		return lastPressed;
	}
	
	public long getLastReleased() {
		return lastReleased;
	}
}
