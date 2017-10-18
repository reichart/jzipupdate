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
package de.vxart.net;

import de.vxart.io.LimitedInputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Multipart MIME Messages as defined in RFC 2046 with
 * the exception that it relies on optional headers to parse the
 * size of the data payload of each MIME part ("Content-Length" or
 * "Content-Range") which are usually present in HTTP responses.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class MultipartMessage
        implements
        Iterable<MultipartMessage.Part>,
        Iterator<MultipartMessage.Part> {
    private final static byte[] DASH_DELIM = {0x2D, 0x2D};

    private final static String CONTENT_LENGTH = "Content-Length";
    private final static String CONTENT_RANGE = "Content-Range";

    private final String DASH_BOUNDARY;
    private DataInputStream input;
    private Part previousPart;


    /**
     * Constructs a MultipartMessage that parses from the given
     * stream using the specified boundary to separate parts.
     *
     * @param input    the stream to read the multipart MIME message from
     * @param boundary the boundary used to delimit the MIME parts
     */
    public MultipartMessage(InputStream input, String boundary) {
        DASH_BOUNDARY = new String(DASH_DELIM) + boundary;

		/*
         * Wrapping the original InputStream into that class
		 * is mandatory for the multipart parsing to work!
		 * (regular BufferedInputStreams read stuff in
		 *  a very annoying and inconvenient manner)
		 */
        this.input = new DataInputStream(input);
    }


    /**
     * Tries to extract the size of the data
     * payload from the headers of a part.
     * <p>
     * TODO conform to RFC by not relying on optional header
     * and just read data up to the next boundary
     */
    private long getDataSize(Map<String, String> headers)
            throws IOException {
        long size;

        String contentLength = headers.get(CONTENT_LENGTH);
        if (contentLength == null) {
			/*
			 * The value of the Content-Range header is structured
			 * like "bytes START-END/TOTAL", so we need to substract
			 * values of the START and END offsets to get the actual
			 * size of the data.
			 */
            String contentRange = headers.get(CONTENT_RANGE.toLowerCase());

            if (contentRange == null)
                throw new IOException("Missing header to extract length of data.");

            Pattern numberPattern = Pattern.compile("(\\d)+");
            Matcher matcher = numberPattern.matcher(contentRange);

			/*
			 * This arrays will hold the number in this order:
			 * START, END, OFFSET
			 */
            long[] rangeNumbers = new long[3];

            try {
                for (int i = 0; matcher.find(); i++) {
                    rangeNumbers[i] = Long.parseLong(matcher.group());
                }
            } catch (NumberFormatException nfex) {
                throw new IOException(
                        "Invalid " + CONTENT_RANGE + " header to " +
                                "extract length of data: " + contentRange);
            }

            size = rangeNumbers[1] - rangeNumbers[0];
        } else {
			/*
			 * The value of the Content-Length header
			 * is a plain number denoting the data size.
			 */
            try {
                size = Long.parseLong(contentLength);
            } catch (NumberFormatException nfex) {
                throw new IOException(
                        "Invalid " + CONTENT_LENGTH + " header to " +
                                "extract length of data: " + contentLength);
            }
        }

        return size;
    }

    /**
     * Reads a line from the input stream terminated by CRLF.
     * This method uses the US-ASCII encoding to create the
     * returned String.
     */
    private String readLine()
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int bite;
        while ((bite = input.read()) != -1) {
            if (bite == '\r') {
                if (input.read() == '\n') {
                    break;
                } else {
                    throw new IllegalArgumentException("Found CR in line but not followed by LF.");
                }
            }

            baos.write(bite);
        }

        return new String(baos.toByteArray(), "us-ascii");
    }

    /**
     * Ensures that the parser is right before
     * the beginning of the next part.
     */
    private void ensurePosition() {
        if (previousPart != null)
            previousPart.skip();
    }

    /**
     * Returns an Iterator over all parts of this multipart message.
     *
     * @return an Iterator over all parts
     */
    public Iterator<MultipartMessage.Part> iterator() {
        return this;
    }

    /**
     * Returns true if there's a next part available.
     *
     * @return true if there's a next part available, false otherwise
     */
    public boolean hasNext() {
        ensurePosition();

        try {
            String line;

			/*
			 * Find the boundary preceding the first part
			 * (discarding the preamble if there is one)
			 */
            do {
                line = readLine();

				/* TODO
				 * Differences in multipart-responses from Apache and Tomcat?
				 *
				if(line.equals(""))
				{
					throw new RuntimeException("Empty line in wrong place.");
				}
				*/
            }
            while (!line.startsWith(DASH_BOUNDARY));

            if (line.endsWith("--")) {
				/*
				 * This closes any streams to the tmp file so deleteOnExit works,
				 * see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4171239.
				 * Thanks pepe :)
				 */
                input.close();
                return false;
            }

            return true;
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    /**
     * Returns the next part from this multipart message.
     *
     * @return the next part from this multipart message
     */
    public MultipartMessage.Part next() {
        ensurePosition();

        try {
			/*
			 * Parse any headers for this part
			 */
            Map<String, String> headers = new HashMap<>();

            String line;
            while (!(line = readLine()).equals("")) {
                int colon = line.indexOf(':');

                // TODO lower-casing header names might be a problem
                String headerName = line.substring(0, colon).trim().toLowerCase();
                String headerValue = line.substring(colon + 1).trim();

                headers.put(headerName, headerValue);
            }

            Part part = new Part(headers, input, getDataSize(headers));

            previousPart = part;

            return part;
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    /**
     * This method is unsupported for MultipartMessages.
     *
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public class Part {
        private Map<String, String> headers;
        private LimitedInputStream data;


        /**
         * Constructs a new Part with the specified headers
         * and InputStream as data source. The InputStream
         * will be wrapped into another stream that prevents
         * reading more bytes than specified by the limit.
         *
         * @param headers the multipart headers for this Part
         * @param input   the InputStream from which this Part is fed
         * @param size    the size of the data payload
         */
        public Part(
                Map<String, String> headers,
                DataInputStream input,
                long size) {
            this.headers = headers;
            this.data = new LimitedInputStream(input, size + 1);
        }

        /**
         * Skips over the data of this Part. Calling this
         * method more than once does not have any effect.
         *
         * @return true if any actually data has been skipped, false otherwise
         */
        public boolean skip() {
            try {
                return data.skipAll() > 0;
            } catch (IOException ioex) {
                throw new RuntimeException("Failed to skip data", ioex);
            }
        }

        /**
         * Provides a stream containing the data of this part.
         *
         * @return a stream containing the data of this part
         */
        public InputStream openStream() {
            return data;
        }

        /**
         * Returns all headers associated with this part.
         *
         * @return all headers associated with this part
         */
        public Map<String, String> getHeaders() {
            return headers;
        }
    }
}
