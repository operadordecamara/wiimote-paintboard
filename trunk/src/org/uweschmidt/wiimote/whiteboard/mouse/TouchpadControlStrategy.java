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

import java.awt.Point;

import org.uweschmidt.wiimote.whiteboard.mouse.rightclick.RightClickStrategy;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;

public class TouchpadControlStrategy implements CursorControlStrategy {
	
	private static final WWPreferences prefs = WWPreferences.getPreferences();
	private RightClickStrategy rcs = WWPreferences.RIGHT_CLICK_STRATEGY;
	
	private long lightOnTime = 0, lightOffTime = System.currentTimeMillis();
	
	public void process(Point p) {
		
		final long now = System.currentTimeMillis();
		final boolean lightOnNow = p != null;
		final boolean lightOnBefore = lightOnTime > lightOffTime;
		
		rcs.process(p);
		
		// update time stamps
		if (!lightOnBefore && lightOnNow) {
			lightOnTime = now;
		} else if (lightOnBefore && !lightOnNow) {
			lightOffTime = now;
		}
		
		// release mouse button
		if (!lightOnNow && Mouse.LEFT_BUTTON.isPressed() && now - lightOffTime > WWPreferences.SHORT_DELAY) {
			Mouse.LEFT_BUTTON.setPressed(false);
		}
		
		if (lightOnNow) {
			Mouse.move(p);
			if (!prefs.isLeftClick() || (prefs.isRightClick() && rcs.trigger())) {
				return;
			}
		}

		if (lightOnBefore && !lightOnNow) {
			// short flash => left click
			if (lightOffTime - lightOnTime < WWPreferences.SHORT_DELAY) {
				// double-click (end current click, start a new one)
				if (Mouse.LEFT_BUTTON.isPressed()) {
					Mouse.LEFT_BUTTON.setPressed(false);
				}
				Mouse.LEFT_BUTTON.setPressed(true);
			} else {
				Mouse.LEFT_BUTTON.setPressed(false);
			}
			
		}
	}

}
