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

package org.uweschmidt.wiimote.whiteboard.mouse.smoothing;

import java.awt.Point;
import java.util.LinkedList;

public class SimpleMovingAverage implements MouseSmoothingStrategy {
	
	public final static String NAME = "Simple Moving Average";
	
	// TODO allow user to change value
	private static final int N = 7;
	private LinkedList<Point> points = new LinkedList<Point>();	

	public Point translate(Point p) {
		return getAverage(p);
	}
	
	public void reset() {
		if (!points.isEmpty()) points.clear();
	}

	// XXX inefficient, but shouldn't be an issue here
	private Point getAverage(Point p) {
		// add new and remove old
		points.addLast(p);
		if (points.size() > N) points.removeFirst();
		
		// get average of last N points
		int x = 0, y = 0;
		for (Point o : points) {
			x += o.x;
			y += o.y;
		}
		
		return new Point(Math.round((float)x / (float)points.size()), Math.round((float)y / (float)points.size()));
	}	

}
