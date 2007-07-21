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

import static de.vxart.zip.ZipConstants.LOCAL_FILE_HEADER;
import static de.vxart.zip.ZipConstants.LOCAL_FILE_HEADER_LENGTH;

/**
 * Encapsulates a "Local File Header" block from a ZIP file.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class LocalFileHeader extends ZipHeader
{
	public short versionNeededToExtract;
	public short flag;
	public short compressionMethod;
	public short lastModificationTime;
	public short lastModificationDate;
	public long compressedSize;
	public long uncompressedSize;
	public long crc;
	public int nameLength;
	public int extraLength;


	/**
	 * Constructs a new LFH from the given byte array.
	 *
	 * @param bytes a byte array containing a valid LFH block
	 */
	public LocalFileHeader(byte[] bytes)
	{
		super(LOCAL_FILE_HEADER, LOCAL_FILE_HEADER_LENGTH, bytes);

		versionNeededToExtract = buffer.getShort();
		flag = buffer.getShort();
		compressionMethod = buffer.getShort();
		lastModificationTime = buffer.getShort();
		lastModificationDate = buffer.getShort();
		crc = buffer.getInt() & 0xFFFFFFFFL;
		compressedSize = buffer.getInt() & 0xFFFFFFFFL;
		uncompressedSize = buffer.getInt() & 0xFFFFFFFFL;
		nameLength = buffer.getShort();
		extraLength = buffer.getShort();
	}

	/**
	 * Returns a really long String representation of a LFH instance.
	 * Use only for debugging, not for daily consumption.
	 */
    @Override
	public String toString()
	{
		return
			getClass().getName() +
			"[" +
			"sig=" + hex(signature) +
			"versionNTE=" + hex(versionNeededToExtract) +
			"flag=" + hex(flag) +
			"method=" + hex(compressionMethod) +
			"lmodTime=" + hex(lastModificationTime) +
			"lmodDate=" + hex(lastModificationDate) +
			"crc=" + hex(crc) +
			"compSize=" + compressedSize +
			"uncompSize=" + uncompressedSize +
			"nameLength=" + nameLength +
			"extraLength=" + extraLength +
			"]";
	}
}