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

package org.uweschmidt.wiimote.whiteboard.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.lang.reflect.Constructor;

import javax.swing.Action;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

public final class Util {

	private static final String OS_VERSION = System.getProperty("os.version");
	public static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
	public static final boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
	public static final boolean MAC_OS_X_INTEL = MAC_OS_X && System.getProperty("os.arch").equals("i386");
	public static final boolean MAC_OS_X_PPC = MAC_OS_X && System.getProperty("os.arch").equals("ppc");	
	public static final boolean MAC_OS_X_TIGER = MAC_OS_X && OS_VERSION.startsWith("10.4");
	public static final boolean MAC_OS_X_LEOPARD = MAC_OS_X && OS_VERSION.startsWith("10.5");
	public static final boolean MAC_OS_X_LEOPARD_OR_HIGHER = MAC_OS_X && Float.parseFloat(OS_VERSION.substring(0, OS_VERSION.lastIndexOf('.'))) >= 10.5f;
	public static final boolean INSIDE_APP_BUNDLE = MAC_OS_X && System.getProperty("org.uweschmidt.wiimote.whiteboard.insideBundle") != null && System.getProperty("org.uweschmidt.wiimote.whiteboard.insideBundle").equals("true");

	private Util() {}

	public static ResourceMap getResourceMap(final Class<?> c) {
		return Application.getInstance().getContext().getResourceMap(c);
	}

	public static Action getAction(final Object o, final Object key) {
		return Application.getInstance().getContext().getActionMap(o).get(key);
	}
	
	public static <T extends Component> T newComponent(Class<T> classT, String name) {
		return newComponent(classT, name, null);
	}
	
	public static <T extends Component> T newComponent(Class<T> classT, String name, Constructor<T> con, Object... args) {
		try {
			T component = null;
			if (con != null) {
				component = con.newInstance(args);
			} else {
				component = classT.newInstance();
			}
			component.setName(name);
			return component;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void placeDialogWindow(Window window, int width, int height) {

		Dimension windowSize = new Dimension(width, height);
		window.setSize(windowSize);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		Point windowLocation = new Point(0, 0);
		windowLocation.x = (screenSize.width - windowSize.width) / 2;
		windowLocation.y = (screenSize.height / 3) - (windowSize.height / 2);

		window.setLocation(windowLocation);
	}
}
