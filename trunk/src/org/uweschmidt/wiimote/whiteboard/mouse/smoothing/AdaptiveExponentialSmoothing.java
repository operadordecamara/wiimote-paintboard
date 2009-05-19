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

public class AdaptiveExponentialSmoothing implements MouseSmoothingStrategy {
	
	public final static String NAME = "Adaptive Exponential Smoothing";
	
	private static final double alpha = .20d;
	private static final double dAlpha = .1d;
	
	private double x, y, dx, dy;
	private Point last;
	
	public AdaptiveExponentialSmoothing() {
		reset();
	}

	public void reset() {
		x = y = dx = dy = Double.NaN;
		last = null;
	}

	public Point translate(Point p) {
		
		double alpha = AdaptiveExponentialSmoothing.alpha;
		
		// smoothed "velocities" of x and y movements
		if (last == null) {
			dx = dy = 0;
		} else {
			dx += dAlpha*((p.getX() - last.getX()) - dx);
			dy += dAlpha*((p.getY() - last.getY()) - dy);
			// slow movements => alpha is closer to 0, newer values are less important
			// faster movements => alpha is closer to ExponentialSmoothing.alpha, newer values are more important 
			alpha = alpha+.015 - (alpha+.015)*(1/(Math.max(Math.abs(dx), Math.abs(dy))+1));
		}
		last = p;
		
//		System.out.printf("%d\t%d\t%d\n", System.currentTimeMillis(), p.x, p.y);
//		System.out.printf("dx = %7.3f, dy = %7.3f, alpha = %5.3f\n", dx, dy, alpha);

		// smoothed cursor position, alpha based on dx and dy
		if (Double.isNaN(x)) {
			x = p.getX();
			y = p.getY();
		} else {
			x += alpha*(p.getX() - x);
			y += alpha*(p.getY() - y);
		}
		return new Point((int)Math.round(x), (int)Math.round(y));
	}

}
