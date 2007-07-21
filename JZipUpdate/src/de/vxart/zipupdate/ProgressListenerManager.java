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
package de.vxart.zipupdate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides a convenient way to handle multiple ProgressListeners at once.  
 * 
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class ProgressListenerManager implements ProgressListener, Iterable<ProgressListener>
{
	private List<ProgressListener> listeners;
	
	
	public ProgressListenerManager()
	{
		this.listeners = new LinkedList<ProgressListener>();
	}
	
	public void add(ProgressListener listener)
	{
		listeners.add(listener);
	}
	
	public void remove(ProgressListener listener)
	{
		listeners.remove(listener);
	}
	
	public void init(String message)
	{
		for(ProgressListener listener : listeners)
		{
			listener.init(message);
		}
	}

	public void init(String message, int min, int max)
	{
		for(ProgressListener listener : listeners)
		{
			listener.init(message, min, max);
		}
	}

	public void update(int progress)
	{
		for(ProgressListener listener : listeners)
		{
			listener.update(progress);
		}
	}

	public void label(String label)
	{
		for(ProgressListener listener : listeners)
		{
			listener.label(label);
		}
	}
	
	public void finish()
	{
		for(ProgressListener listener : listeners)
		{
			listener.finish();
		}
	}
	
	/**
	 * Returns and Iterator over all ProgressListeners
	 * registered with this manager.
	 */
	public Iterator<ProgressListener> iterator()
	{
		return listeners.iterator();
	}
	
	/**
	 * Returns the progress from the first registered listeners.
	 */
	public int getProgress()
	{
		return listeners.get(0).getProgress();
	}
	
	/**
	 * Returns the number of ProgressListener registered with this manager.
	 * 
	 * @return the number of ProgressListener in this manager.
	 */
	public int size()
	{
		return listeners.size();
	}
}
