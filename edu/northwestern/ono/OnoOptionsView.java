/**
 * Ono Project
 *
 * File:         OnoOptionsView.java
 * RCS:          $Id: OnoOptionsView.java,v 1.4 2010/03/29 16:48:04 drc915 Exp $
 * Description:  OnoOptionsView class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Sep 5, 2008 at 11:22:04 AM
 * Language:     Java
 * Package:      edu.northwestern.ono
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2008, Northwestern University, all rights reserved.
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
package edu.northwestern.ono;

import java.util.ArrayList;
import java.util.Properties;

import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.FileParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.StringListParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;

import edu.northwestern.ono.position.CIDRMatcher;
import edu.northwestern.ono.util.PluginInterface;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoOptionsView class ...
 */
public class OnoOptionsView {

	private static OnoOptionsView self;
	private BasicPluginConfigModel config_model;
	private BooleanParameter enableStatsParam;
	private Properties props;
	private ArrayList<String> propertiesLocations;
	private PluginInterface pi_az;
	private IntParameter maxPingsParam;
	private IntParameter reportingTimeoutParam;
	private StringListParameter testList;
	private FileParameter testFile;
	private FileParameter cidrFileParam;
	
	public static OnoOptionsView getInstance() {
		if (self==null){
			self = new OnoOptionsView();
		}
		return self;
	}



	public void setConfigModel(BasicPluginConfigModel config_model) {
		this.config_model = config_model;
		
	}
	
	public void initialize(){
	
        
        //peers       = config_model.addStringParameter2(
        //        "ono.peers", "ono.peers", "" );
        
        //final ActionParameter   inject      = config_model.addActionParameter2(
        //        "ono.inject.info", "ono.inject.button" );
        final OnoConfiguration oc = OnoConfiguration.getInstance();
        enableStatsParam     = config_model.addBooleanParameter2(
                "ono.enablestats", "ono.enablestats", oc.isRecordStats() );
        
        enableStatsParam.addConfigParameterListener(new ConfigParameterListener(){

			public void configParameterChanged(ConfigParameter param) {
				oc.setRecordStats(enableStatsParam.getValue());
				oc.writeProperties(props, propertiesLocations);
				
				
			}});
        String timeoutS = props.getProperty(
		"ono.reportingTimeout");
        int timeout = (timeoutS == null ? 60 : Integer.valueOf(timeoutS));
        reportingTimeoutParam = config_model.addIntParameter2(
        		"ono.reportingTimeout", "ono.reportingTimeout", 
        		timeout);
        reportingTimeoutParam.addConfigParameterListener(new ConfigParameterListener(){

			public void configParameterChanged(ConfigParameter param) {
				oc.setReportingTimeout(reportingTimeoutParam.getValue());
				oc.writeProperties(props, propertiesLocations);
				
			}});
        
        String maxPingsS = props.getProperty(
		"ono.maxPings");
        int maxPings = maxPingsS == null ? 5 : Integer.valueOf(maxPingsS);
        if (maxPings==60) {
        	maxPings = 10;
        	oc.setMaxPings(10);
        	oc.writeProperties(props, propertiesLocations);
        }
        maxPingsParam = config_model.addIntParameter2(
        		"ono.maxPings", "ono.maxPings", 
        		maxPings);
        maxPingsParam.addConfigParameterListener(new ConfigParameterListener(){

			public void configParameterChanged(ConfigParameter param) {
				oc.setMaxPings(maxPingsParam.getValue());
				oc.writeProperties(props, propertiesLocations);
				
			}});
        
        cidrFileParam = config_model.addFileParameter2("ono.cidrFile", "ono.cidrFile", 
        	oc.getCidrFile()==null?"":oc.getCidrFile());
        cidrFileParam.addConfigParameterListener(new ConfigParameterListener(){

			public void configParameterChanged(ConfigParameter param) {
				oc.setCidrFile(cidrFileParam.getValue());
				CIDRMatcher.getInstance().loadFromFile(oc.getCidrFile());
				oc.writeProperties(props, propertiesLocations);
				
			}});
        
        config_model.addLabelParameter2("ono.respect");
       
        try {
	        if (pi_az.getUtilities().compareVersions("2.5.0.2", 
	        		pi_az.getAzureusVersion())<=0) {
	        	config_model.addHyperlinkParameter2("ono.teamSig", "http://aqualab.cs.northwestern.edu");
	        } else {
	        	config_model.addLabelParameter2("ono.teamSig");
	        } 
        } catch (Exception e){
        	Main.log("Error adding hyperlink, continuing...");
        } catch (Error e){
        	Main.log("Error adding hyperlink, continuing...");
        }

//        Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
//        String sCurConfigID;
//
//        GridData gridData;
//
//        int userMode = COConfigurationManager.getIntParameter("User Mode");
//
//      
//        
//        // start controls
//
//        	// row: enable filter + allow/deny
//        
//    	gridData = new GridData();
//
//        BooleanParameter enabled = new BooleanParameter(gFilter, "Ip Filter Enabled");
//    	enabled.setLayoutData( gridData ); 
//        Messages.setLanguageText(enabled.getControl(), "ConfigView.section.ipfilter.enable");
//
//    	gridData = new GridData();
//
//        BooleanParameter deny = new BooleanParameter(gFilter, "Ip Filter Allow");
//    	deny.setLayoutData( gridData ); 
//        Messages.setLanguageText(deny.getControl(), "ConfigView.section.ipfilter.allow");
//      
//        deny.addChangeListener(
//        	new ParameterChangeAdapter()
//    		{
//        		public void
//        		parameterChanged(
//        			Parameter	p,
//        			boolean		caused_internally )
//    			{
//        			setPercentageBlocked();
//    			}
//    		});
//        
//        	// row persist banning
//        
//    	gridData = new GridData();
//
//      BooleanParameter persist_bad_data_banning = new BooleanParameter(gFilter, "Ip Filter Banning Persistent");
//      persist_bad_data_banning.setLayoutData( gridData );
//      Messages.setLanguageText(persist_bad_data_banning.getControl(), "ConfigView.section.ipfilter.persistblocking");
//
//      
//        Group gAutoLoad = new Group(gFilter, SWT.NONE);
//        Messages.setLanguageText(gAutoLoad, "ConfigView.section.ipfilter.autoload.group");
//        layout = new GridLayout();
//        layout.numColumns = 4;
//        gAutoLoad.setLayout(layout);
//        gAutoLoad.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
//        
//        // Load from file
//        sCurConfigID = "Ip Filter Autoload File";
//        //allConfigIDs.add(sCurConfigID);
//        Label lblDefaultDir = new Label(gAutoLoad, SWT.NONE);
//        Messages.setLanguageText(lblDefaultDir, "ConfigView.section.ipfilter.autoload.file");
//        lblDefaultDir.setLayoutData(new GridData());
//        
//        gridData = new GridData(GridData.FILL_HORIZONTAL);
//        gridData.minimumWidth = 50;
//        final StringParameter pathParameter = new StringParameter(gAutoLoad, sCurConfigID);
//        pathParameter.setLayoutData(gridData);
//
//        Button browse = new Button(gAutoLoad, SWT.PUSH);
//        browse.setImage(imgOpenFolder);
//        imgOpenFolder.setBackground(browse.getBackground());
//
//        browse.addListener(SWT.Selection, new Listener() {
//          public void handleEvent(Event event) {
//            FileDialog dialog = new FileDialog(parent.getShell(), SWT.APPLICATION_MODAL);
//            dialog.setFilterPath(pathParameter.getValue());
//            dialog.setText(MessageText.getString("ConfigView.section.ipfilter.autoload.file"));
//            dialog.setFilterExtensions(new String[] {
//    					"*.dat" + File.pathSeparator + "*.p2p" + File.pathSeparator + "*.p2b"
//    							+ File.pathSeparator + "*.txt",
//    					"*.*"
//    				});
//            dialog.setFileName("ipfilter.dat");
//            String file = dialog.open();
//            if (file != null) {
//              pathParameter.setValue(file);
//            }
//          }
//        });
//        browse.setLayoutData(new GridData());
//        
//        final Button btnLoadNow = new Button(gAutoLoad, SWT.PUSH);
//        Messages.setLanguageText(btnLoadNow, "ConfigView.section.ipfilter.autoload.loadnow");
//        btnLoadNow.addListener(SWT.Selection, new Listener() {
//    			public void handleEvent(Event event) {
//    				try {
//    					btnLoadNow.getShell().setCursor(btnLoadNow.getDisplay().getSystemCursor(
//    							SWT.CURSOR_WAIT));
//    					COConfigurationManager.setParameter(IpFilterAutoLoaderImpl.CFG_AUTOLOAD_LAST, 0);
//    					filter.reload();
//    					btnLoadNow.getShell().setCursor(btnLoadNow.getDisplay().getSystemCursor(
//    							SWT.CURSOR_ARROW));
//    				} catch (Exception e) {
//    					e.printStackTrace();
//    				}
//    			}
//    		});
//        btnLoadNow.setLayoutData(new GridData());
//        
//        Label lblAutoLoadInfo = new Label(gAutoLoad, SWT.WRAP);
//        Messages.setLanguageText(lblAutoLoadInfo, "ConfigView.section.ipfilter.autoload.info");
//        lblAutoLoadInfo.setLayoutData(Utils.getWrappableLabelGridData(4, 0));
//
//        
//        
//        // description scratch file
//        if (userMode > 0) {
//        	gridData = new GridData();
//        	BooleanParameter enableDesc = new BooleanParameter(gFilter,
//        	"Ip Filter Enable Description Cache");
//        	enableDesc.setLayoutData(gridData);
//        	Messages.setLanguageText(enableDesc.getControl(),
//        	"ConfigView.section.ipfilter.enable.descriptionCache");
//        }
//          
//     
//    		// table
//    	
//        table = new Table(gFilter, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
//        String[] headers = { "ConfigView.section.ipfilter.description", "ConfigView.section.ipfilter.start", "ConfigView.section.ipfilter.end" };
//        int[] sizes = { 110, 110, 110 };
//        int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER };
//        for (int i = 0; i < headers.length; i++) {
//          TableColumn tc = new TableColumn(table, aligns[i]);
//          tc.setText(headers[i]);
//          tc.setWidth(sizes[i]);
//          Messages.setLanguageText(tc, headers[i]); //$NON-NLS-1$
//        }
//        
//        
//        
//        TableColumn[] columns = table.getColumns();
//        columns[0].setData(new Integer(FilterComparator.FIELD_NAME));
//        columns[1].setData(new Integer(FilterComparator.FIELD_START_IP));
//        columns[2].setData(new Integer(FilterComparator.FIELD_END_IP));
//        
//        Listener listener = new Listener() {
//          public void handleEvent(Event e) {
//            TableColumn tc = (TableColumn) e.widget;
//            int field = ((Integer) tc.getData()).intValue();
//            comparator.setField(field);
//            
//            if (field == FilterComparator.FIELD_NAME && !bIsCachingDescriptions) {
//            	ipFilterManager.cacheAllDescriptions();
//            	bIsCachingDescriptions = true;
//            }
//            ipRanges = getSortedRanges(filter.getRanges());
//            table.setItemCount(ipRanges.length);
//            table.clearAll();
//        	// bug 69398 on Windows
//        	table.redraw();
//          }
//        };
//        
//        columns[0].addListener(SWT.Selection,listener);
//        columns[1].addListener(SWT.Selection,listener);
//        columns[2].addListener(SWT.Selection,listener);
//
//        table.setHeaderVisible(true);
//
//        gridData = new GridData(GridData.FILL_BOTH);
//        gridData.heightHint = table.getHeaderHeight() * 3;
//    		gridData.widthHint = 200;
//        table.setLayoutData(gridData);
//
//        Composite cArea = new Composite(gFilter, SWT.NULL);
//        layout = new GridLayout();
//        layout.marginHeight = 0;
//        layout.marginWidth = 0;
//        layout.numColumns = 4;
//        cArea.setLayout(layout);
//      	gridData = new GridData(GridData.FILL_HORIZONTAL);
//        cArea.setLayoutData(gridData);
//
//        Button add = new Button(cArea, SWT.PUSH);
//        gridData = new GridData(GridData.CENTER);
//        gridData.widthHint = 100;
//        add.setLayoutData(gridData);
//        Messages.setLanguageText(add, "ConfigView.section.ipfilter.add");
//        add.addListener(SWT.Selection, new Listener() {
//          public void handleEvent(Event arg0) {
//            addRange();
//          }
//        });
//
//        Button remove = new Button(cArea, SWT.PUSH);
//        gridData = new GridData(GridData.CENTER);
//        gridData.widthHint = 100;
//        remove.setLayoutData(gridData);
//        Messages.setLanguageText(remove, "ConfigView.section.ipfilter.remove");
//        remove.addListener(SWT.Selection, new Listener() {
//          public void handleEvent(Event arg0) {
//            TableItem[] selection = table.getSelection();
//            if (selection.length == 0)
//              return;
//            removeRange((IpRange) selection[0].getData());
//            ipRanges = getSortedRanges(filter.getRanges());
//            table.setItemCount(ipRanges.length);
//            table.clearAll();
//            table.redraw();
//          }
//        });
//
//        Button edit = new Button(cArea, SWT.PUSH);
//        gridData = new GridData(GridData.CENTER);
//        gridData.widthHint = 100;
//        edit.setLayoutData(gridData);
//        Messages.setLanguageText(edit, "ConfigView.section.ipfilter.edit");
//        edit.addListener(SWT.Selection, new Listener() {
//          public void handleEvent(Event arg0) {
//            TableItem[] selection = table.getSelection();
//            if (selection.length == 0)
//              return;
//            editRange((IpRange) selection[0].getData());
//          }
//        });
//
//        percentage_blocked  = new Label(cArea, SWT.WRAP | SWT.RIGHT);
//        percentage_blocked.setLayoutData(Utils.getWrappableLabelGridData(1, 0));
//        setPercentageBlocked();
//        
//
//        
//        table.addMouseListener(new MouseAdapter() {
//          public void mouseDoubleClick(MouseEvent arg0) {
//            TableItem[] selection = table.getSelection();
//            if (selection.length == 0)
//              return;
//            editRange((IpRange) selection[0].getData());
//          }
//        });
//
//        Control[] controls = new Control[3];
//        controls[0] = add;
//        controls[1] = remove;
//        controls[2] = edit;
//        IAdditionalActionPerformer enabler = new ChangeSelectionActionPerformer(controls);
//        enabled.setAdditionalActionPerformer(enabler);
//
//        ipRanges = getSortedRanges(filter.getRanges());
//
//        table.addListener(SWT.SetData, new Listener() {
//    			public void handleEvent(Event event) {
//    				TableItem item = (TableItem) event.item;
//    				int index = table.indexOf(item);
//
//    				// seems we can get -1 here (see bug 1219314 )
//
//    				if (index < 0 || index >= ipRanges.length) {
//    					return;
//    				}
//    				IpRange range = ipRanges[index];
//    				item.setText(0, range.getDescription());
//    				item.setText(1, range.getStartIp());
//    				item.setText(2, range.getEndIp());
//    				item.setData(range);
//    			}
//    		});
//        
//        table.setItemCount(ipRanges.length);
//        table.clearAll();
//    	// bug 69398 on Windows
//    	table.redraw();
//        
//    		table.addListener(SWT.Resize, new Listener() {
//    			public void handleEvent(Event e) {
//            resizeTable();
//    			}
//    		});
//
//    		gFilter.addListener(SWT.Resize, new Listener() {
//    			public void handleEvent(Event e) {
//            resizeTable();
//    			}
//    		});
//        
//        
//    		filterListener = new IPFilterListener() {
//    			public boolean canIPBeBanned(String ip) {
//    				return true;
//    			}
//    		
//    			public void IPBanned(BannedIp ip) {
//    			}
//
//    			public void IPBlockedListChanged(final IpFilter filter) {
//    				Utils.execSWTThread(new AERunnable() {
//    					public void runSupport() {
//    						if (table.isDisposed()) {
//    					  	filter.removeListener(filterListener);
//    					  	return;
//    						}
//    		        ipRanges = getSortedRanges(filter.getRanges());
//    		        table.setItemCount(ipRanges.length);
//    		        table.clearAll();
//    		        table.redraw();
//    					}
//    				});
//    				
//    			}
//    			public boolean
//    			canIPBeBlocked(
//    				String	ip,
//    				byte[]	torrent_hash )
//    			{
//    				return true;
//    			}
//    		};
//        filter.addListener(filterListener);
        
        
	}



	public void setProperties(Properties props) {
		this.props = props;
		
	}



	public void setPropsLocations(ArrayList<String> propertiesLocations) {
		this.propertiesLocations=propertiesLocations;
		
	}



	public void setPluginInterface(PluginInterface pi_az) {
		this.pi_az=pi_az;
		
	}

}
