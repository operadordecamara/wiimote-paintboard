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

package org.uweschmidt.wiimote.whiteboard.gui;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class LogWindow extends JDialog {
	
	private JTextArea log;
	private JDialog failedDialog;	
	
	public LogWindow() {
		super(Application.getInstance(WiimoteWhiteboard.class).getMainFrame(), WiimoteWhiteboard.getProperty("id") + " " + Util.getResourceMap(LogWindow.class).getString("log.Action.text"));
		getRootPane().putClientProperty("Window.style", "small");
//		setLayout(new BorderLayout());
		setLayout(new MigLayout());
//		if (getContentPane() instanceof JPanel) {
//			((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//		}
		
		log = Util.newComponent(JTextArea.class, "log");
//		add(new JScrollPane(log), BorderLayout.CENTER);
		add(new JScrollPane(log), "w 66sp, h 33sp, grow, push");
		
		Logger.getLogger("wiiremotej").addHandler(new LogHandler("WiiRemoteJ: "));
		WiimoteWhiteboard.getLogger().addHandler(new LogHandler(""));
		
		log.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					setVisible(false);
				}
			}
		});
		
		Util.getResourceMap(LogWindow.class).injectComponents(this);
		pack();
//		Util.placeDialogWindow(this, 500, 300);
		Util.placeDialogWindow(this, getWidth(), getHeight());
		
		failedDialog = Util.newComponent(JDialog.class, "failedDialog");
		failedDialog.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					failedDialog.setVisible(false);
				}
			}
		});		
		failedDialog.setAlwaysOnTop(true);
		failedDialog.getRootPane().putClientProperty("Window.style", "small");
		failedDialog.getRootPane().putClientProperty("Window.alpha", new Float(0.90));
		failedDialog.setLayout(new BorderLayout());
		failedDialog.add(Util.newComponent(JLabel.class, "failedLabel"), BorderLayout.CENTER);
		Util.getResourceMap(LogWindow.class).injectComponents(failedDialog);
		failedDialog.pack();
//		failedDialog.setBounds(getX(), getY()+getHeight()+10, getWidth(), failedDialog.getHeight()+20);
		failedDialog.setBounds(getX()+getWidth()/2-(failedDialog.getWidth()+20)/2, getY()+getHeight()+10, failedDialog.getWidth()+20, failedDialog.getHeight()+20);
//		failedDialog.setVisible(true);
	}
	
	@Action
	public void log() {
		setVisible(!isVisible());
	}
	
	private class LogHandler extends Handler {
		private static final String DIRECT_CONNECTION_TIMEOUT = "Failed to open baseband connection";
		private static final String CONNECTION_FAILED = "WiiRemote failed to connect!";
		private static final String CONNECTION_SUCCEEDED = "Initial connection complete.";
		private final String name;
		public LogHandler(String name) {
			this.name = name;
			setLevel(Level.ALL);
		}
		@Override
		public void close() throws SecurityException {}
		@Override
		public void flush() {}
		
		@Override
		public void publish(LogRecord record) {
			Level level = record.getLevel();
			String msg = record.getMessage();
			Throwable e = record.getThrown();
			boolean failed = false;
			
			// skip that error (apparently timeout on direct connection attempts)
			if (!WWPreferences.WIIMOTE_BT_ADDRESSES.isEmpty() && e != null && e.getCause() != null && e.getCause().getMessage().startsWith(DIRECT_CONNECTION_TIMEOUT))
				return;
			
			log.append(String.format("%7s     %tT  %s%s\n", level.getName(), Calendar.getInstance(), name, msg));
			if (e != null) {
				log.append(getStackTrace(e));
				failed = CONNECTION_FAILED.equals(e.getMessage());
			}
			log.setCaretPosition(log.getText().length()-1);
			
			if (!failed && level == Level.SEVERE) {
				if (!LogWindow.this.isVisible()) {
//					LogWindow.this.pack();
//					Util.placeDialogWindow(LogWindow.this, LogWindow.this.getWidth(), LogWindow.this.getHeight());
					LogWindow.this.setVisible(true);
				}
			}
			
			if (CONNECTION_SUCCEEDED.equals(msg)) {
//				LogWindow.this.setVisible(false);
				failedDialog.setVisible(false);
			} else if (failed) {
				failedDialog.setVisible(true);
			}
		}
	}

	private static String getStackTrace(Throwable e) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		return result.toString();
	}

}
