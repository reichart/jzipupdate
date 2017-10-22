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

import java.io.InputStream;

/**
 * Encapsulates information relevant to update a ZIP entry and
 * access to its data, similar to java.util.zip.ZipEntry.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class Resource {
    private final String name;
    private final InputStream data;
    private final long crc;

    public final static String FLAG_NOOP = "===";
    public final static String FLAG_ADD = "+++";
    public final static String FLAG_UPDATE = "!!!";
    public final static String FLAG_REMOVE = "---";


    public Resource(String name, long crc, InputStream data) {
        this.name = name;
        this.crc = crc;
        this.data = data;
    }

    public Resource(String name, long crc) {
        this(name, crc, null);
    }

    public Resource(String name, InputStream data) {
        this(name, -1, data);
    }

    public InputStream getData() {
        return data;
    }

    public long getCrc() {
        return crc;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getName() +
                "[name=" + name +
                ";crc=" + Long.toHexString(crc) +
                "]";
    }
}
