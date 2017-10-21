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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Provides structure and utility methods to parse and encapsulate
 * ZIP file headers.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public abstract class ZipHeader {
    public final int signature;
    public final int size;

    /**
     * Creates a new header with the given signature and size
     * initialized to the data in the byte array.
     *
     * @param signature the required signature/magical number for the header
     * @param size      the required size for the header
     */
    protected ZipHeader(int signature, int size) {
        this.signature = signature;
        this.size = size;
    }

    /*
     * Initializes this header from a byte array.
     */
    protected ByteBuffer parse(byte[] bytes) {
        if (bytes.length != size) {
            throw new IllegalArgumentException(
                    "Data for " + getClass().getName() + " has to be " +
                            size + " bytes long: " + bytes.length);
        }

        /*
         * Wrap the bytes into a buffer to easily
         * get at the multi-bytes values we need.
         */
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int actualSignature = buffer.getInt();

        if (actualSignature != signature) {
            throw new IllegalArgumentException(
                    "Data for " + getClass().getName() +
                            " doesn't start with magic number " +
                            hex(signature) + ": " +
                            hex(actualSignature));
        }

        return buffer.asReadOnlyBuffer();
    }

    /**
     * Turns an integer into a nicely formatted
     * hex string of the form 0x123ABC.
     *
     * @param i the integer to turn into a hex string
     * @return a nicely formatted hex string
     */
    protected static String hex(int i) {
        return "0x" + Integer.toHexString(i);
    }

    /**
     * Turns an long into a nicely formatted
     * hex string of the form 0x123ABC.
     *
     * @param l the long to turn into a hex string
     * @return a nicely formatted hex string
     */
    protected static String hex(long l) {
        return "0x" + Long.toHexString(l);
    }
}
