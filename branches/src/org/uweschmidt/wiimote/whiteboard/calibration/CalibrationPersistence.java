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

package org.uweschmidt.wiimote.whiteboard.calibration;

import java.awt.DisplayMode;
import java.io.IOException;

import org.jdesktop.application.Application;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration.CalibrationEvent;
import org.uweschmidt.wiimote.whiteboard.calibration.WiimoteCalibration.CalibrationEventListener;

public class CalibrationPersistence implements CalibrationEventListener {
	
	WiimoteCalibration calibration;
	
	public CalibrationPersistence(WiimoteCalibration calibration) {
		this.calibration = calibration;
		calibration.addCalibrationEventListener(this);
	}
	
	public void calibrationEvent(CalibrationEvent e) {
		switch (e) {
			case SCREEN_CHANGED:
				loadCalibrationData();
				break;
			case FINISHED:
				saveCalibrationData();
				break;
		}
	}

//	private String safeFilename(String rawName) {
//		return rawName.replaceAll("[\\W]", "").toLowerCase();		
//	}
	
	private String calibrationFileName() {
		DisplayMode dm = calibration.getScreen().getDisplayMode();
		return String.format("calibration_%d_%dx%d.txt", calibration.getScreenNumber(), dm.getWidth(), dm.getHeight());
	}
	
	private void loadCalibrationData() {
		try {
			String fileName = calibrationFileName();
			calibration.load(Application.getInstance().getContext().getLocalStorage().openInputFile(fileName));
		} catch (IOException e) {
			// ignore
		}
	}
	
	private void saveCalibrationData() {
		try {
			String fileName = calibrationFileName();
			calibration.save(Application.getInstance().getContext().getLocalStorage().openOutputFile(fileName));
		} catch (IOException e) {
			// ignore
		}
	}

}
