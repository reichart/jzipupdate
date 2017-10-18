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
package de.vxart.zip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Provides structure and utility methods to parse and encapsulate
 * ZIP file headers.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public abstract class ZipHeader
{
	public int signature;
	public int size;
	protected ByteBuffer buffer;


	/**
	 * Creates a new header with the given signature and size
	 * initialized to the data in the byte array.
	 *
	 * @param signature		the required signature/magical number for the header
	 * @param size			the required size for the header
	 * @param bytes			the bytes containing the actual header data
	 */
	protected ZipHeader(int signature, int size, byte[] bytes)
	{
		this.signature = signature;
		this.size = size;
		init(bytes);
	}

	/**
	 * Creates a new header with the given signature and size
	 * initialized to data read from the InputStream.
	 *
	 * @param signature		the required signature/magical number for the header
	 * @param size			the required size for the header
	 * @param bytes			a stream contain the actual header data
	 */
	protected ZipHeader(int signature, int size, InputStream in)
		throws IOException
	{
		this.signature = signature;
		this.size = size;
		init(in);
	}

	/*
	 * Initializes this header from an InputStream.
	 */
	private void init(InputStream in)
		throws IOException
	{
		byte[] bytes = new byte[size];

		int n = 0;
		do
		{
			int count = in.read(bytes, n, size - n);

			if (count < 0)
				throw new EOFException();

			n += count;
		} while (n < size);

		init(bytes);
	}

	/*
	 * Initializes this header from a byte array.
	 */
	private void init(byte[] bytes)
	{
		if(bytes.length != size)
		{
			throw new IllegalArgumentException(
				"Data for " + getClass().getName() + " has to be " +
				size + " bytes long: " + bytes.length);
		}

		/*
		 * Wrap the bytes into a buffer to easily
		 * get at the multi-bytes values we need.
		 */
		buffer = ByteBuffer.wrap(bytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		int actualSignature = buffer.getInt();

		if(actualSignature != signature)
		{
			throw new IllegalArgumentException(
				"Data for " + getClass().getName() +
				" doesn't start with magic number " +
				hex(signature) + ": " +
				hex(actualSignature));
		}
	}

	/**
	 * Turns an integer into a nicely formatted
	 * hex string of the form 0x123ABC.
	 *
	 * @param i		the integer to turn into a hex string
	 * @return		a nicely formatted hex string
	 */
	protected String hex(int i)
	{
		return "0x" + Integer.toHexString(i);
	}

	/**
	 * Turns an long into a nicely formatted
	 * hex string of the form 0x123ABC.
	 *
	 * @param i		the long to turn into a hex string
	 * @return		a nicely formatted hex string
	 */
	protected String hex(long l)
	{
		return "0x" + Long.toHexString(l);
	}
}
