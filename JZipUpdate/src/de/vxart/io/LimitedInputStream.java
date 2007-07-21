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
 * Allows to only read a limited number of bytes from the
 * underlying stream and then signals EOF.
 * 
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class LimitedInputStream extends FilterInputStream
{
	private long bytesRead, limit;
	
	
	/**
	 * Wraps the given InputStream and will only read as many
	 * bytes from it as the limit allows; it will then signal
	 * EOF by returning -1 from the read methods.
	 * 
	 * @param in		the stream to limit read access to
	 * @param limit		the number of bytes allowed to be read
	 */
	public LimitedInputStream(InputStream in, long limit)
	{
		super(in);
		this.limit = limit;
	}
	
	/**
	 * Returns the number of bytes already read from this stream.
	 * 
	 * @return the number of bytes read from this stream
	 */
	public long getBytesRead()
	{
		return bytesRead;
	}
	
	/**
	 * Returns the number of bytes remaining before the limit
	 * of this stream.
	 * 
	 * @return the number of bytes remaining before the limit
	 */
	public long getBytesRemaining()
	{
		long rem = limit - bytesRead;
		return rem;
	}

    @Override
	public int available() throws IOException
	{
		int available = in.available();
		long rem = getBytesRemaining();
		
		return available > rem ? (int)rem : available;
	}
	
    @Override
	public int read() throws IOException
	{
		if(bytesRead >= limit)
			return -1;
		
		int b = in.read();
		
		if(b != -1)
			bytesRead++;
		
		return b;
	}
    @Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (bytesRead >= limit)
			return -1;
		
		long rem = getBytesRemaining();
		
		if (len > rem)
			len = (int)rem;
		
        int read = in.read(b, off, len);
		bytesRead += read;
		
		return read;
	}

    @Override
	public int read(byte[] b) throws IOException
	{
		return this.read(b, 0, b.length);
	}

    @Override
	public long skip(long n) throws IOException
	{
		if (bytesRead >= limit)
			return -1;
		
		long rem = getBytesRemaining();
		
		if (n > rem)
			n = rem;
		
		long skipped = in.skip(n);
		bytesRead += skipped;
		
		return skipped;
	}
	
	/**
	 * Skips all bytes left in this stream before the limit
	 * is reached. Equal to calling in.skip(in.getBytesRemaining()).
	 * 
	 * @return the number of bytes skipped
	 * @throws IOException
	 */
	public long skipAll()
		throws IOException
	{
		return skip(getBytesRemaining());
	}
	
	/**
	 * "Closes" the InputStream by skipping all remaining bytes. 
	 */
    @Override
	public void close() throws IOException
	{
		skipAll();
	}
}
