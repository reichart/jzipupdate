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

import de.vxart.zip.LocalFileHeader;

import java.io.*;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static de.vxart.zip.ZipConstants.*;

/**
 * Extracts the data from a ZIP file block.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class ZipEntryInputStream extends FilterInputStream {
    /**
     * Creates am InputStream that will read the uncompressed data
     * of a ZIP entry. This class expects the given InputStream to
     * be a complete ZIP local file block, starting with the magical
     * number 0x04034B50.
     *
     * @param in
     * @throws IOException
     */
    public ZipEntryInputStream(DataInputStream in)
            throws IOException {
        super(in);

        byte[] headerBytes = new byte[LOCAL_FILE_HEADER_LENGTH];
        in.readFully(headerBytes);
        LocalFileHeader header = new LocalFileHeader(headerBytes);

        in.skipBytes(header.nameLength);
        in.skipBytes(header.extraLength);

        switch (header.compressionMethod) {
            case METHOD_STORED:
                break;

            case METHOD_DEFLATED:
                Inflater decomp = new Inflater(true);

                /*
                 * This ugly construct is brought to you by the weird
                 * behavior of the Inflater class when in "nowrap" mode:
                 *
                 * From the javadoc of the Inflater(boolean) constructor:
                 *     "When using the 'nowrap' option it is also
                 *      necessary to provide an extra 'dummy' byte
                 *      as input."
                 *
                 * Luckily Java provides some ready-to-go classes :)
                 */
                this.in = new InflaterInputStream(
                        new SequenceInputStream(
                                this.in,
                                new ByteArrayInputStream(new byte[1])),
                        decomp);
                break;

            default:
                throw new IOException(
                        "Unsupported compression method: " +
                                Integer.toHexString(header.compressionMethod));
        }
    }
}
