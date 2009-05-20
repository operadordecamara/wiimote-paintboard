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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.uweschmidt.wiimote.whiteboard.ds.IRDot;
import org.uweschmidt.wiimote.whiteboard.ds.Wiimote;
import org.uweschmidt.wiimote.whiteboard.preferences.WWPreferences;

public class PointClusterer {

	static {
		if (WWPreferences.WIIMOTES > 2)
			throw new RuntimeException("Clustering for more than 2 Wiimotes not implemented yet.");
	}

	/**
	 * @return array of clusters (first index = cluster number, second index = dots in cluster)
	 */
	public static IRDot[][] cluster(Map<Wiimote, IRDot[]> data) {
		IRDot[][] cluster = new IRDot[4][];

		// trivial case
		if (data.size() == 1) {
			IRDot[] values = data.values().iterator().next();
			int j = 0;
			for (int i = 0; i < 4; i++)
				if (values[i] != null)
					cluster[j++] = new IRDot[] { values[i] };
			return cluster;
		}

		Wiimote[] wiimotes = new Wiimote[data.keySet().size()];
		data.keySet().toArray(wiimotes);

		Set<IRDot> used = new HashSet<IRDot>();
		int c = 0;
		
		for (int a = 0; a < 2; a++) {
			
			for (int i = 0; i < 4; i++) {				
				IRDot dot1 = data.get(wiimotes[a])[i];
				if (dot1 == null || used.contains(dot1))
					continue;
				
				IRDot minDot = null;
				double minDist = Double.POSITIVE_INFINITY;
				
				for (int j = 0; j < 4; j++) {
					IRDot dot2 = data.get(wiimotes[(a+1)%2])[j];
					if (dot2 == null || used.contains(dot2))
						continue;
					
					double dist = dot1.distance(dot2);
					if (dist < minDist) {
						minDot = dot2;
						minDist = dist;
					}
				}
				
				used.add(dot1);
				if (minDot == null) {
					cluster[c++] = new IRDot[] { dot1 };
				} else {
					cluster[c++] = new IRDot[] { dot1, minDot };
					used.add(minDot);
				}
				
			}
		}		

		return cluster;
	}

}
