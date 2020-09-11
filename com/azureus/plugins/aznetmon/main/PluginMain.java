/**
 * Created on Dec 17, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.azureus.plugins.aznetmon.main;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;


//public class PluginMain implements org.gudy.azureus2.plugins.Plugin
public class PluginMain implements UnloadablePlugin
{

	private PluginInterface pluginInterface;

    UTTimer utTimer=null;

    RSTUnloadableView view = null;
    UISWTInstance swtInst = null;

    private static final String VIEWID = "aznetmon_View";

    private UIManagerListener uiManagerListener=null;

    /**
     * Register the plug-in.
	 */
	public void initialize(PluginInterface _pluginInterface) {
		pluginInterface = _pluginInterface;

        //attach the view.
        uiManagerListener = new UIManagerListener(){
            public void UIAttached(UIInstance uiInstance) {
                if(uiInstance instanceof UISWTInstance){
                    swtInst = (UISWTInstance) uiInstance;

                    view = new RSTUnloadableView(pluginInterface);
                    if( view!=null ){
                        //add to menu
                        swtInst.addView(UISWTInstance.VIEW_MAIN, VIEWID, view);
                        //open immediately
                        swtInst.openMainView(VIEWID,view,null);
                    }

                }
            }

            public void UIDetached(UIInstance uiInstance) {
                if(uiInstance instanceof UISWTInstance){
                    swtInst = null;
                }
            }
        };

        pluginInterface.getUIManager().addUIListener(uiManagerListener);

        
        //Start the thread to detect RST packets.
        pluginInterface.getUtilities();
        Utilities util = pluginInterface.getUtilities();

        //For now we are getting data for only Windows //ToDo: make it run on multiple OSs
        if( util.isWindows() ){

            utTimer = util.createTimer("RST Packet Monitor",false);
            UTTimerEventPerformer perf = new RSTPacketCountPerformer(pluginInterface);

            //start on event within one second.
            utTimer.addEvent(1000,perf);
            //follow-up every 10 minutes.
            utTimer.addPeriodicEvent(RSTPacketStats.INTERVAL,perf);

            //Stats are uploaded through the version server.

        }else{
            //Currently only works with windows
            RSTPacketStats stats = RSTPacketStats.getInstance();
            stats.setStatus("Plug-in currently only works with Windows machines.");
        }

    }//initialize

    public void unload()
        throws PluginException
    {
        //
        if(utTimer!=null){
            utTimer.destroy();
        }

        if( swtInst!=null ){
            swtInst.removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
        }

        if( uiManagerListener!=null ){
            pluginInterface.getUIManager().removeUIListener( uiManagerListener );
        }

    }//unload

}//class
