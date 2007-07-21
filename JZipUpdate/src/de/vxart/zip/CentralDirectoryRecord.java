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

import static de.vxart.zip.ZipConstants.CENTRAL_DIRECTORY;
import static de.vxart.zip.ZipConstants.CENTRAL_DIRECTORY_LENGTH;

/**
 * Encapsulates a single "Central Directory Record" block from a ZIP file.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class CentralDirectoryRecord extends ZipHeader
{
	public short versionMadeBy;
	public short versionNeededToExtract;
	public short flag;
	public short compressionMethod;
	public short lastModificationTime;
	public short lastModificationDate;
	public long crc;
	public long compressedSize;
	public long uncompressedSize;
	public short nameLength;
	public short extraLength;
	public short fileCommentLength;
	public short startDiskNumber;
	public short internalFileAttributes;
	public int externalFileAttributes;
	public int offsetToLocalFileHeader;


	/**
	 * Constructs a new CDR from the given byte array.
	 *
	 * @param bytes a byte array containing a valid CDR block
	 */
	public CentralDirectoryRecord(byte[] bytes)
	{
		super(CENTRAL_DIRECTORY, CENTRAL_DIRECTORY_LENGTH, bytes);

		versionMadeBy = buffer.getShort();
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
		fileCommentLength = buffer.getShort();
		startDiskNumber = buffer.getShort();
		internalFileAttributes = buffer.getShort();
		externalFileAttributes = buffer.getInt();
		offsetToLocalFileHeader = buffer.getInt();
	}

}
