/*
 * Copyright 2005 Philipp Reichart <philipp.reichart@vxart.de>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.vxart.zipupdate.ui;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import de.vxart.zipupdate.MultiProgressListener;
import de.vxart.zipupdate.ProgressListener;

/**
 * 
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class MultiProgressDialog implements MultiProgressListener
{
	private ProgressPanel overallProgress;
	private ProgressPanel panel;
	private JOptionPane pane;
	JDialog dialog;
	
	public MultiProgressDialog()
	{
		super();
		
		panel = new ProgressPanel();
		
		overallProgress = new ProgressPanel()
		{
            @Override
			public void finish()
			{
				super.finish();
				dialog.setVisible(false);
				dialog.dispose();
			}
		};
		overallProgress.setSpeedShown(false);
		overallProgress.setTimeShown(false);
		
		Box multiPanel = new Box(BoxLayout.Y_AXIS); 
		multiPanel.add(overallProgress);
		multiPanel.add(panel);
		
		pane = new JOptionPane(
			multiPanel, // message
			JOptionPane.INFORMATION_MESSAGE, // msg type
			JOptionPane.DEFAULT_OPTION, // options type
			null, // icon
			new Object[0]); // options
		
		overallProgress.init("Initializing...");
		
		dialog = pane.createDialog(null, "Updating");
		Dimension size = dialog.getSize();
		dialog.setSize(size.width*3/2, size.height);
		dialog.setModal(false);
	}
	

	public void init(String message)
	{
		panel.init(message);
		dialog.setVisible(true);
	}
	
	public void init(String message, int min, int max)
	{
		panel.init(message, min, max);
		dialog.setVisible(true);
	}
	
	public void update(int value)
	{
		panel.update(value);
	}

	public void label(String message)
	{
		panel.label(message);
	}

	public int getProgress()
	{
		return panel.getProgress();
	}
	
	public void finish()
	{
		panel.finish();
	}
	
	public ProgressListener getOverallProgressListener()
	{
		return overallProgress;
	}
}
