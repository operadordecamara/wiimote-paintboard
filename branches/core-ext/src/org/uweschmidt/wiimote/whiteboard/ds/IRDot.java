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

package org.uweschmidt.wiimote.whiteboard.ds;

import java.awt.geom.Point2D;

import wiiremotej.IRLight;

public class IRDot extends Point2D.Double {

	private final int id;
	private double size;
	
	public IRDot(int id, double x, double y, double size) {
		super(x, y);
		this.id = id;
		this.size = size;
	}
	
	public IRDot(IRDot dot) {
		this(dot.getId(), dot.getX(), dot.getY(), dot.getSize());
	}

	public IRDot(int id, IRLight light) {
		this(id, light.getX(), light.getY(), light.getSize());
	}

	@Override
	public String toString() {
		return String.format("[Dot%d: x = %.2f, y = %.2f, s = %.2f]", id, x, y, size);
	}
	
	public double getSize() {
		return size;
	}
	
	public int getId() {
		return id;
	}

	public static IRDot[] getIRDots(IRLight[] lights) {
		IRDot[] dots = new IRDot[lights.length];
		for (int i = 0; i < lights.length; i++) {
			dots[i] = lights[i] == null ? null : new IRDot(i, lights[i]);
		}
		return dots;
	}

}
