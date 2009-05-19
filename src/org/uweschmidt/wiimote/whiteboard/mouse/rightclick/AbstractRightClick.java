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

package org.uweschmidt.wiimote.whiteboard.mouse.rightclick;

import java.awt.Point;

import org.uweschmidt.wiimote.whiteboard.mouse.Mouse;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;

public abstract class AbstractRightClick implements RightClickStrategy {

	private long lightStartTime = 0;
	private Point lightStartPoint = null;
	private boolean active = false;

	protected abstract void activate();
	protected abstract void deactivate();

	public void process(Point p) {
		if (p != null) {
			// reset start point for potential right click
			if (!active && (lightStartPoint == null || lightStartPoint.distance(p) > WWPreferences.PIXEL_MOVE_TOLERANCE)) {
				lightStartPoint = p;
				lightStartTime = System.currentTimeMillis();
			}
		} else {
			lightStartPoint = null;
			if (active) {
				active = false;
				deactivate();
			}
		}
	}

	public boolean trigger() {
		// light hasn't moved in the same area for long enough to trigger right click
		if (lightStartPoint != null && System.currentTimeMillis() - lightStartTime > WWPreferences.getPreferences().getRightClickDelay()) {
			if (!active) {
				active = true;
				Mouse.LEFT_BUTTON.setPressed(false);
				activate();
			}
			return true;
		}
		return false;
	}

}
