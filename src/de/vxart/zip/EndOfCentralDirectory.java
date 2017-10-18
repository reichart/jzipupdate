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

import static de.vxart.zip.ZipConstants.END_OF_CENTRAL_DIRECTORY;
import static de.vxart.zip.ZipConstants.END_OF_CENTRAL_DIRECTORY_LENGTH;

/**
 * Encapsulates a "End of Central Directory" block from a ZIP file.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class EndOfCentralDirectory extends ZipHeader {

    public short currentDiskNumber;
    public short cdStartDiskNumber;
    public short centralDirectoryRecordsThisDisk;
    public short centralDirectoryRecordsAllDisks;
    public int centralDirectorySize;
    public int centralDirectoryOffset;
    public short commentLength;


    /**
     * Constructs a new ECD from the given byte array.
     *
     * @param bytes a byte array containing a valid ECD block
     */
    public EndOfCentralDirectory(byte[] bytes) {
        super(END_OF_CENTRAL_DIRECTORY, END_OF_CENTRAL_DIRECTORY_LENGTH, bytes);

        currentDiskNumber = buffer.getShort();
        cdStartDiskNumber = buffer.getShort();
        centralDirectoryRecordsThisDisk = buffer.getShort();
        centralDirectoryRecordsAllDisks = buffer.getShort();
        centralDirectorySize = buffer.getInt();
        centralDirectoryOffset = buffer.getInt();
        commentLength = buffer.getShort();
    }

}
