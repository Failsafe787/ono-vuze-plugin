/**
 * Ono Project
 *
 * File:         ITimerEventPerformer.java
 * RCS:          $Id: ITimerEventPerformer.java,v 1.2 2007/09/18 02:18:21 drc915 Exp $
 * Description:  ITimerEventPerformer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 27, 2007 at 2:22:15 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.util
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2007, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package edu.northwestern.ono.timer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The ITimerEventPerformer class ...
 */
public interface ITimerEventPerformer {
	public void
	perform(
		ITimerEvent		event );

}
