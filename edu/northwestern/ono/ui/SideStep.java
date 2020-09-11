/**
 * Ono Project
 *
 * File:         SideStep.java
 * RCS:          $Id: SideStep.java,v 1.17 2010/03/29 16:48:04 drc915 Exp $
 * Description:  SideStep class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Sep 26, 2007 at 3:40:50 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.ui
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
package edu.northwestern.ono.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.Pair;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The SideStep class is the main visualizer class for SideStep.
 */
public class SideStep implements ITimerEventPerformer {

	private static final double MAX_RANGE = 200;
	private static final double MAX_Y = -1;
	private static SideStep self;
	JFrame frame;
	private JPanel contentPane;
	private JTabbedPane tabbedPane;
	private HashMap<String, XYSeries> dlSeries;
	private HashMap<String, XYSeries> ulSeries;
	private HashMap<String, LinkedList<Pair<Long, Integer>>> dlDataPoints;
	private HashMap<String, LinkedList<Pair<Long, Integer>>> ulDataPoints;
	private HashMap<String, Long> lastDlTimes;
	private HashMap<String, Long> lastUlTimes;	
	private long startTime=-1;
	JFreeChart ulChart;
	JFreeChart dlChart;
	XYSeriesCollection dlDataset;
	XYSeriesCollection ulDataset;
	private JPanel graphDlTab;
	private HashMap<String, String> ipToNames;
	private JPanel graphUlTab;
	private JTable nearbyPeersTable;
	private DefaultTableModel nearbyPeersModel;
	private boolean ulFixedRange;
	private boolean dlFixedRange;
	LinkedList<Double> ulMax = new LinkedList<Double>();
	LinkedList<Double> dlMax = new LinkedList<Double>();
	LinkedList<String> raceText = new LinkedList<String>();
	JTextPane textPane;
	
	public SideStep(){
		dlDataPoints = new HashMap<String, LinkedList<Pair<Long, Integer>>>();
		ulDataPoints = new HashMap<String, LinkedList<Pair<Long, Integer>>>();
		lastDlTimes = new HashMap<String, Long>();
		lastUlTimes = new HashMap<String, Long>();
		dlSeries = new HashMap<String, XYSeries>();
		ulSeries = new HashMap<String, XYSeries>();
		ipToNames = new HashMap<String, String>();
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable()
				{
			public void run()
			{
				createAndShowGUI();
			}
				});

		synchronized(this)
		{
			while(frame==null)
			{
				try
				{
					wait();
				}
				catch(InterruptedException e)
				{
				}
			}
		}
		frame.invalidate();
		ITimer timer = MainGeneric.createTimer("VisualizerUpdater");
		timer.addPeriodicEvent(1000, this);
		
	}
	
	public void resetGraph(){
		dlSeries.clear();
		ulSeries.clear();
		dlDataset = null;
		ulDataset = null;
		graphDlTab.removeAll();
		graphUlTab.removeAll();
		ulChart = null;
		dlChart=null;
		dlFixedRange = false;
		ulFixedRange = false;
		startTime = -1;
		dlDataPoints.clear();
		ulDataPoints.clear();
		lastDlTimes.clear();
		lastUlTimes.clear();
		textPane.setText("");
		dlMax.clear();
		ulMax.clear();
		
	}
	
	public void addDlDataPoint(String ipAddress, long currentTime, int bytesSent){
//		if (startTime==-1) startTime = currentTime;
		if (dlDataPoints.get(ipAddress)==null){
//			if (dlSeries.get(ipAddress)==null){
//				addSeriesToGraph(dlSeries, dlDataset, ipAddress);
//			}
//			dlSeries.get(ipAddress).add((currentTime-startTime)/1000.0, 0);
			dlDataPoints.put(ipAddress, new LinkedList<Pair<Long, Integer>>());
		}
		LinkedList<Pair<Long, Integer>> list = dlDataPoints.get(ipAddress);
		synchronized (list){
//			if (list.size()>0 && list.get(list.size()-1).getValue()==0){
//				list.add(new Pair<Long, Integer>( list.get(list.size()-1).getKey()-1 ,0));
//			}

//			if (bytesSent==0){
//				if (dlSeries.get(ipAddress)==null){
//					addSeriesToGraph(dlSeries, dlDataset, ipAddress);
//				}
//				dlSeries.get(ipAddress).add((currentTime-startTime)/1000.0, 0);
//				list.clear();
//				dlDataPoints.remove(ipAddress);
//			}
//			else 
				list.add(new Pair<Long, Integer>(currentTime, bytesSent));
		}
	}
	
	public void addUlDataPoint(String ipAddress, long currentTime, int bytesSent){
		//if (bytesSent < 2000) return;
		if (ulDataPoints.get(ipAddress)==null) ulDataPoints.put(ipAddress, new LinkedList<Pair<Long, Integer>>());
		synchronized (ulDataPoints.get(ipAddress)){
			ulDataPoints.get(ipAddress).add(new Pair<Long, Integer>(currentTime, bytesSent));
		}
	}
	
	private synchronized void createAndShowGUI(){
		JFrame frame = new JFrame("DraFTP");
        Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

//        if (d.width < fieldX || d.height < fieldY)
//        {
//            zoom = Math.max((int)Math.ceil((double)fieldX/d.width), fieldY/(d.height-BOTTOM_OFFSET-totalTopHeight-20));
//        }
        
        //fieldX = Math.max((int)(4*infoPaneWidth)+30, fieldX);
//        d.setSize(Math.min(d.width, fieldX+10), 
//                Math.min(d.height-BOTTOM_OFFSET, fieldY/(zoom)+totalTopHeight+60));

        
		// ensure proper width for info panes

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setBounds(0,0, this.fieldX, this.fieldY+infoPaneHeight+10);
		frame.setSize(600, 400);
        //frame.setPreferredSize(d);
		//frame.setMaximizedBounds(new Rectangle(d.width, d.height));
		
		contentPane = new JPanel();
        contentPane.setOpaque(true);
//		contentPane.setBounds(0,0, this.fieldX, this.fieldY+infoPaneHeight+10);
		//contentPane.setSize(d);
		//contentPane.setPreferredSize(d);
		
		addElementsToContentPane();
		
		frame.setContentPane(contentPane);
		
		// show frame
		frame.pack();
		frame.setVisible(true);
		
		this.frame = frame;
//        frame.setAlwaysOnTop(alwaysOnTop);
		notifyAll();
	}
	private void addElementsToContentPane() {
		tabbedPane = new JTabbedPane();
		tabbedPane.setSize(600, 400);
		tabbedPane.setPreferredSize(new Dimension(600, 400));

		graphDlTab = new JPanel();
		tabbedPane.addTab("Download performance", null, graphDlTab,
		                  "Shows current download performance");
		
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		
		graphUlTab = new JPanel();
		tabbedPane.addTab("Upload performance", null, graphUlTab,
		                  "Shows current upload performance");
		
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		//addChart(graphTab);

		JPanel ssInfoTab = new JPanel(new GridBagLayout());
		tabbedPane.addTab("SS Info", null, ssInfoTab,
		                  "Information used to locate SideStep detour points");
		tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
		addContentToInfoTab(ssInfoTab);

		JPanel resultsTab = new JPanel();
		tabbedPane.addTab("Race Results", null, resultsTab,
		"Displays results from evaluating SideStep detour paths");
		tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);
		textPane = new JTextPane();
		JScrollPane jsp = new JScrollPane(textPane);
		resultsTab.add(jsp);
		jsp.setSize(400, 400);
		jsp.setPreferredSize(new Dimension(400, 400));
		textPane.setAutoscrolls(true);
		jsp.setAutoscrolls(true);
		
		contentPane.add(tabbedPane);
		
	}
	
    
    private void addContentToInfoTab(JPanel ssInfoTab) {
    	
   nearbyPeersModel = new DefaultTableModel();
    
    
        String[] columnNames = {"IP",
                "Closeness",
                "Names",
                "Download",
                "DL Impr", "UL Improvement"};

		
		nearbyPeersTable = new JTable(nearbyPeersModel);
		nearbyPeersTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
		nearbyPeersModel.setColumnIdentifiers(columnNames);
		//nearbyPeersTable..setFillsViewportHeight(true);
		
		
		
		//Create the scroll pane and add the table to it.
		JScrollPane scrollPane = new JScrollPane(nearbyPeersTable);
		
		//Add the scroll pane to this panel.
		ssInfoTab.add(scrollPane);
		
	}
    
    public void updateNearbyPeers(OnoPeer op){
    	int columnNum = nearbyPeersModel.findColumn("IP");
    	Vector v = nearbyPeersModel.getDataVector();
    	Vector column = (Vector)v.elementAt(columnNum);
    	int row = -1;
    	for (int i = 0; i < column.size(); i++){
    		Object o = column.elementAt(i);
    		if (o.equals(op.getIp())){
    			row = i;
    			break;
    		}
    	}
    	if (row==-1){
    		nearbyPeersModel.addRow(new Object[]{op.getIp(), 
    				OnoPeerManager.getInstance().getOnoPeer(op.getIp()).printCosSim(true),
    				op.printCustomers(true), op.getKeyNames(), 0, 0} 
    				);
    	}
    	else {
    		
    		nearbyPeersModel.setValueAt(op.getIp(), row, columnNum);
    		nearbyPeersModel.setValueAt(OnoPeerManager.getInstance().getOnoPeer(op.getIp()).printCosSim(true), row, nearbyPeersModel.findColumn("Closeness"));
    		nearbyPeersModel.setValueAt(op.printCustomers(true), row, nearbyPeersModel.findColumn("Names"));
    		nearbyPeersModel.setValueAt(op.getKeyNames(), row, nearbyPeersModel.findColumn("Download"));
    		nearbyPeersModel.setValueAt(0, row, nearbyPeersModel.findColumn("DL Impr"));
    		nearbyPeersModel.setValueAt(0, row, nearbyPeersModel.findColumn("UL Improvement"));
    	}
    }

	/**
     * Creates a chart.
     * 
     * @param dataset  the data for the chart.
     * 
     * @return a chart.
     */
    private JFreeChart createChart(final XYDataset dataset, String title) {
        
        // create the chart...
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,      // chart title
            "Time",                      // x axis label
            "Rate (Bytes/sec)",                      // y axis label
            dataset,                  // data
            PlotOrientation.VERTICAL,
            true,                     // include legend
            true,                     // tooltips
            false                     // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);

//        final StandardLegend legend = (StandardLegend) chart.getLegend();
  //      legend.setDisplaySeriesShapes(true);
        
        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(new Color(220, 220, 220));
    //    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
//        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
//        renderer.setSeriesFillPaint(0, Color.black);//.setSeriesLinesVisible(0, false);
//        renderer.setSeriesShapesVisible(1, false);
//        renderer.setSeriesStroke(
//                0, new BasicStroke(
//                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
//                    1.0f, new float[] {10.0f, 6.0f}, 0.0f
//                )
//            );
//        plot.setRenderer(renderer);

        plot.setDrawingSupplier(getSupplier());
    
        
        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        //plot.getDomainAxis().setFixedAutoRange(MAX_RANGE);
        // OPTIONAL CUSTOMISATION COMPLETED.
                
        return chart;
        
    }

	public void perform(ITimerEvent event) {
		int WINDOW_SIZE = 20;
		// check for ip-to-name conversions
		checkForNames();
		
		// process new data points		
		if (dlDataset==null){
			 dlDataset = new XYSeriesCollection();
		}
		int numAdded = processDataPoints(50, dlDataPoints, dlSeries, 
				dlDataset, lastDlTimes, dlMax);
			
		
		if (dlChart==null && numAdded > 0){
			dlChart = createChart(dlDataset, "SideStep Download Performance");
			final ChartPanel chartPanel = new ChartPanel(dlChart);
			
			tabbedPane.setPreferredSize(null);
	        //chartPanel.setPreferredSize(new Dimension(500, 270));
			graphDlTab.add(chartPanel);
			frame.pack();
		}
		XYPlot plot;
		if (dlChart!=null){
			plot = dlChart.getXYPlot();
			if (plot !=null && !dlFixedRange && plot.getDomainAxis().getRange().getUpperBound()>MAX_RANGE){
				dlFixedRange = true;
				plot.getDomainAxis().setFixedAutoRange(MAX_RANGE);
				if (MAX_Y!=-1) plot.getRangeAxis().setUpperBound(MAX_Y);
			}
//			if (plot!=null && dlMax.size()>0){
//				plot.getRangeAxis().setFixedAutoRange(dlMax.getLast());
//				plot.getRangeAxis().setRange(new Range(0, dlMax.getLast()*1.1));
//
//			}

		}
		
		if (ulDataset==null){
			 ulDataset = new XYSeriesCollection();
		}
		numAdded = processDataPoints(WINDOW_SIZE, ulDataPoints, ulSeries, 
				ulDataset, lastUlTimes, ulMax);
			
		
		if (ulChart==null && numAdded > 0){
			ulChart = createChart(ulDataset, "SideStep Upload Performance");
			final ChartPanel chartPanel = new ChartPanel(ulChart);
			
			tabbedPane.setPreferredSize(null);
	        //chartPanel.setPreferredSize(new Dimension(500, 270));
			graphUlTab.add(chartPanel);
			frame.pack();
		}
		
		if (ulChart!=null){
			plot = ulChart.getXYPlot();
			if (plot!=null && !ulFixedRange && plot.getDomainAxis().getRange().getUpperBound()>MAX_RANGE){
				ulFixedRange = true;
				plot.getDomainAxis().setFixedAutoRange(MAX_RANGE);
			}
			if (plot!=null && ulMax.size()>0){
				plot.getRangeAxis().setFixedAutoRange(ulMax.getLast());
				plot.getRangeAxis().setRange(new Range(0, ulMax.getLast()*1.1));

			}
		}
		
		if (dlChart!=null){			
			dlChart.fireChartChanged();
		}
		if (ulChart!=null){
			ulChart.fireChartChanged();
		}
		
		LinkedList<String> temp = new LinkedList<String>();
		
		synchronized (raceText){
			temp.addAll(raceText);
		}
		for (String s : temp) textPane.setText(textPane.getText()+"\n"+s);
		synchronized (raceText){raceText.clear();	}			
	}

	/**
	 * @param windowSize
	 * @param dataset 
	 * @return
	 */
	private int processDataPoints(int windowSize, HashMap<String, LinkedList<Pair<Long, Integer>>> data, 
			HashMap<String, XYSeries> series, XYSeriesCollection dataset, HashMap<String, Long> lastTimes, 
			LinkedList<Double> history) {
		int numAdded = 0;
		if (data==null) return numAdded;		
		HashSet<String> set = new HashSet<String>();
		set.addAll(data.keySet());
		long gapStart = -1;
		long gapEnd = -1;
		for (String s : set){
			LinkedList<Pair<Long, Integer>> temp = new LinkedList<Pair<Long, Integer>>();
			LinkedList<Pair<Long, Integer>> toRemove = new LinkedList<Pair<Long, Integer>>();
			if (data!=null && data.get(s)!=null){
			synchronized (data.get(s)){temp.addAll(data.get(s));}
			}
			if (temp.size()>windowSize){
				if (lastTimes.get(s)==null) {
					lastTimes.put(s, temp.getFirst().getKey());
					if (startTime==-1) startTime = temp.getFirst().getKey();
					synchronized(data.get(s)) {data.get(s).removeFirst();}
					continue;
				}
				Iterator<Pair<Long, Integer>> it = temp.iterator();
				Pair<Long, Integer> vals[] = new Pair[windowSize];
				for (int i = 1; i < windowSize; i++){
					vals[i] = it.next();
				}
				boolean gap = false;
				while (it.hasNext()){
					for (int i = 0; i < windowSize-1; i++){
						vals[i] = vals[i+1];
						if ( vals[i].getKey()-vals[i+1].getKey()>5000){
							gapStart = vals[i].getKey();
							gapEnd = vals[i].getKey();
							gap = true;
							break;
						}
					}
					vals[windowSize-1] = it.next();
					
					if (series.get(s)==null){
						addSeriesToGraph(series, dataset, s);
					}
					long timeSum=0;
					int dataSum=0;
					for (int i = 0; i < vals.length; i++){
						timeSum+= vals[i].getKey();
						dataSum+=vals[i].getValue();
					}
					long timeDiff = (vals[windowSize-1].getKey()-lastTimes.get(s));					
					Double x = ((timeSum/windowSize)-startTime)/1000.0;
					Double y = (dataSum*1000.0)/timeDiff;
					if (x.isInfinite() || x.isNaN() || y.isNaN() || y.isInfinite()) continue;
					numAdded++;

					series.get(s).add(x, y, false);					
					lastTimes.put(s, vals[0].getKey());
					toRemove.add(vals[0]);
					
					if (history.size()==0){
						history.add(y);
						history.add(y);
					} else {
						if (history.size()==100){
							Double out = history.removeFirst();
							Double max = history.removeLast();
							if (out.doubleValue()==max.doubleValue()){
								max = y;
								for (Double d : history) if (d>max) max = d;								
							}		
							history.addLast(y);
							history.addLast(max);
						} else {
							Double max = history.removeLast();
							history.addLast(y);
							if (y>max) max = y;
							history.addLast(max);
						}
					}
					
				}
				
				if (gap){
					if (series.get(s).getItemCount()>0){
						series.get(s).add((startTime-gapStart+100)/1000.0, 0, false);
						series.get(s).add((startTime-gapEnd-100)/1000.0, 0, false);
						synchronized (data.get(s)){data.get(s).clear();}
					}
				}
				else {
					if (data!=null && data.get(s)!=null){
						synchronized (data.get(s)){
							data.get(s).removeAll(toRemove);
						}
					}
				}
				series.get(s).fireSeriesChanged();
			}
		}
		
		return numAdded;
	}

	/**
	 * @param series
	 * @param dataset
	 * @param s
	 */
	private void addSeriesToGraph(HashMap<String, XYSeries> series,
			XYSeriesCollection dataset, String s) {
		String name = s;
		if (ipToNames.get(s)!=null) name=ipToNames.get(s);
		series.put(s, new XYSeries(name));
		
		dataset.addSeries(series.get(s));
	}

	private void checkForNames() {
		
		HashSet<String> set = new HashSet<String>();
		set.addAll(dlDataPoints.keySet());
		
		lookupNames(set, dlSeries);
		
		set.clear();
		set.addAll(ulDataPoints.keySet());
		
		lookupNames(set, ulSeries);
		
	}

	private void lookupNames(HashSet<String> set, HashMap<String, XYSeries> series) {
		for (String n : set){
			if (ipToNames.get(n)==null){
				String name;
				try {
					 name = InetAddress.getByName(n).getHostName();
				} catch (UnknownHostException e) {
					name = n;
				}
				ipToNames.put(n, name);
				if (series.get(n)!=null) series.get(n).setDescription(name);
			}
		}
	}

	public static SideStep getInstance() {
		if (self == null){
			self = new SideStep();
		}
		return self;
		
	}
	
	 public static DefaultDrawingSupplier getSupplier(){
	        return new DefaultDrawingSupplier(
	                new Paint[] {
	                	Color.black,
	                    Color.green,
	                    Color.red,
	                    Color.orange,
	                    Color.magenta,
	                    Color.cyan,
	                    Color.pink,
	                    Color.gray,
	                    Color.yellow,
	                    ChartColor.DARK_RED,
	                    ChartColor.DARK_GREEN,
	                    ChartColor.LIGHT_RED,
	                    ChartColor.DARK_YELLOW,
	                    ChartColor.DARK_MAGENTA,
	                    ChartColor.DARK_CYAN,
	                    Color.lightGray,
	                    ChartColor.LIGHT_RED,
	                    ChartColor.LIGHT_BLUE,
	                    ChartColor.LIGHT_GREEN,
	                    ChartColor.LIGHT_YELLOW,
	                    ChartColor.LIGHT_MAGENTA,
	                    ChartColor.LIGHT_CYAN},
	                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
	                            DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
	                            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
	                            DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE
	                            );
	 }
	
	public void addRaceResult(String s){
		
		//synchronized (raceText){
		raceText.add(s);
		//}
	}
}
