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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JOptionPane;

import org.jdesktop.application.Application;
import org.uweschmidt.wiimote.whiteboard.WiimoteDataHandler;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;

import com.intel.bluetooth.NotSupportedIOException;

public class WiiRemoteJErrorHandler extends Handler {
	
	public WiiRemoteJErrorHandler(WiimoteDataHandler dh) {
		setLevel(Level.SEVERE);
	}
	
	@Override
	public void close() throws SecurityException {
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		final Throwable thrown = record.getThrown();
		if (thrown != null) {
			thrown.printStackTrace();
			if (thrown.getCause() instanceof NotSupportedIOException) {
				// TODO say that BT stack and BlueCove are probably not compatible
				String msg = thrown.getMessage() + "\n" + thrown.getCause().getMessage();
				JOptionPane.showMessageDialog(null, msg, WiimoteWhiteboard.getProperty("id"), JOptionPane.ERROR_MESSAGE);
				Application.getInstance().exit();
			}
		}
	}

}
