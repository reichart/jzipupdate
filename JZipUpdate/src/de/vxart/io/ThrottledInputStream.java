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
package de.vxart.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides a throttled InputStream which artificially slows down
 * data throughput by sleeping in the current thread.
 * 
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class ThrottledInputStream extends FilterInputStream
{
	private long kiloBytesPerSecond;
	
	
	/**
	 * Wraps the given InputStream while throttling the reading speed
	 * to the specified number of KBytes per second.
	 * 
	 * This class might cause timeouts on network connections
	 * when too much throttling is used.
	 * 
	 * @param in		the stream to throttle reading from
	 * @param kiloBytesPerSecond	the maximum speed in KBytes per second (1024 B/s)
	 */
	public ThrottledInputStream(InputStream in, long kiloBytesPerSecond)
	{
		super(in);
		this.kiloBytesPerSecond = kiloBytesPerSecond;
	}
	
	private void throttle(long bytes)
		throws IOException
	{
		try
		{
			long sleep = (1024*bytes)/(1000*kiloBytesPerSecond);
			//System.out.println("throttling " + bytes + " bytes for " + sleep + " ms");
			Thread.sleep(sleep);
		}
		catch (InterruptedException iex)
		{
			throw new IOException("Interrupted: " + iex.getMessage());
		}
	}

    @Override
	public int read() throws IOException
	{
		throttle(4);
		
		return in.read();
	}

    @Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		throttle(len-off);
		
		return in.read(b, off, len);
	}

    @Override
	public int read(byte[] b) throws IOException
	{
		throttle(b.length);
		
		return in.read(b);
	}

    @Override
	public long skip(long n) throws IOException
	{
		throttle(n);
		
		return in.skip(n);
	}
}
