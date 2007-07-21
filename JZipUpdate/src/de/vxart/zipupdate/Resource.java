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
public class Resource
{
	private String name;
	private InputStream data;
	private long crc;

	public final static String FLAG_NOOP = "===";
	public final static String FLAG_ADD = "+++";
	public final static String FLAG_UPDATE = "!!!";
	public final static String FLAG_REMOVE = "---";


	public Resource(String name)
	{
		this.name = name;
	}

	public InputStream getData() {
		return data;
	}
	public void setData(InputStream data) {
		this.data = data;
	}
	public long getCrc() {
		return crc;
	}
	public void setCrc(long crc) {
		this.crc = crc;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

    @Override
	public String toString()
	{
		return getClass().getName() +
			"[name=" + name +
			";crc=" + Long.toHexString(crc) +
			"]";
	}
}
