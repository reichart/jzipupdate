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

/**
 * Contains constants and magical numbers used for parsing ZIP archives.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class ZipConstants {
    public static final int LOCAL_FILE_HEADER = 0x04034B50;
    public static final int CENTRAL_DIRECTORY = 0x02014B50;
    public static final int EXTENDED_FILE_HEADER = 0x08074B50;
    public static final int END_OF_CENTRAL_DIRECTORY = 0x06054B50;

    public static final int CENTRAL_DIRECTORY_LENGTH = 46;
    public static final int LOCAL_FILE_HEADER_LENGTH = 30;
    public static final int EXTENDED_FILE_HEADER_LENGTH = 16;
    public static final int END_OF_CENTRAL_DIRECTORY_LENGTH = 22;

    public static final int METHOD_STORED = 0;
    public static final int METHOD_SRHUNK = 1;
    public static final int METHOD_REDUCED_1 = 2;
    public static final int METHOD_REDUCED_2 = 3;
    public static final int METHOD_REDUCED_3 = 4;
    public static final int METHOD_REDUCED_4 = 5;
    public static final int METHOD_IMPLODED = 6;
    public static final int METHOD_TOKENIZED = 7;
    public static final int METHOD_DEFLATED = 8;
    public static final int METHOD_DEFLATE64 = 9;
    public static final int METHOD_PK_IMPLODE = 10;
    public static final int METHOD_PK_RESERVED = 11;
    public static final int METHOD_BZIP2 = 12;
}
