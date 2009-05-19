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
import java.awt.Font;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;
import org.uweschmidt.wiimote.whiteboard.util.BareBonesBrowserLaunch;
import org.uweschmidt.wiimote.whiteboard.util.Util;

@SuppressWarnings("serial")
public class AboutWindow extends JDialog implements HyperlinkListener {
	
	public AboutWindow() {
		super(Application.getInstance(WiimoteWhiteboard.class).getMainFrame());
		if (!Util.INSIDE_APP_BUNDLE) {
			setName("aboutWindow");
			setLayout(new BorderLayout());
			final JPanel appPane = Util.newComponent(JPanel.class, "appPane");
			add(appPane, BorderLayout.NORTH);
			appPane.setLayout(new MigLayout("insets i", "[center|center]"));
			
			JLabel appLabel = new JLabel(String.format("%s %s", WiimoteWhiteboard.getProperty("id"), WiimoteWhiteboard.getProperty("version")));
			appLabel.setFont(appLabel.getFont().deriveFont(Font.BOLD, 14f));		
			JLabel crLabel = new JLabel(Util.getResourceMap(AboutWindow.class).getString("copyRight", WiimoteWhiteboard.getProperty("year"), WiimoteWhiteboard.getProperty("author")));
			crLabel.setFont(appLabel.getFont().deriveFont(10f));
					
			appPane.add(appLabel, "flowy, split 2");
			appPane.add(crLabel, "");		
			appPane.add(new JLabel(Util.getResourceMap(WiimoteWhiteboard.class).getImageIcon("icon")), "flowx, wrap");
			
			try {
				// TODO replace HTML with custom layout, HTML in JEditorPane is apparently too buggy
				URL url = AboutWindow.class.getResource("resources/Credits.html");
				JEditorPane tp = Util.newComponent(JEditorPane.class, "infoPane");
				tp.addHyperlinkListener(this);
				tp.setPage(url);
				add(new JScrollPane(tp), BorderLayout.CENTER);			
				
				Util.getResourceMap(AboutWindow.class).injectComponents(this);
				
				setResizable(false);
				Util.placeDialogWindow(this, 295, 348);
			} catch (Exception e) {
				// temp. catch all exceptions locally because of buggy JEditorPane
				e.printStackTrace();
			}
		}
	}
	
    public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			BareBonesBrowserLaunch.openURL(e.getURL().toString());
		}
	}
	
	@Action
	public void about() {
		setVisible(true);
	}

}
