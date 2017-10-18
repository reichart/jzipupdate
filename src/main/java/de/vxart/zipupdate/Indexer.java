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

import de.vxart.zip.CentralDirectoryRecord;
import de.vxart.zip.EndOfCentralDirectory;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;

import static de.vxart.zip.ZipConstants.CENTRAL_DIRECTORY_LENGTH;
import static de.vxart.zip.ZipConstants.END_OF_CENTRAL_DIRECTORY_LENGTH;

/**
 * Creates an index file from ZIP/JAR archives used by the client-side
 * update mechanism to download individual ZIP entries from a web server.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class Indexer {
    private static Logger logger = Logger.getLogger(Indexer.class.getName());

    /**
     * Provides a basic stand-alone way to index archives.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)
            throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java de.vxart.zipupdate.Indexer <ZIP file | directory>");
            System.exit(1);
        }

        File input = new File(args[0]);

        if (input.isFile()) {
            index(input);
        } else if (input.isDirectory()) {
            File[] files = input.listFiles((dir, name) -> {
                name = name.toLowerCase();
                return name.endsWith(".zip") || name.endsWith(".jar");
            });

            if (files.length < 1) {
                logger.log(Level.WARNING, "No ZIP/JAR files found in " + input.getCanonicalPath());
                System.exit(2);
            }

            logger.log(Level.INFO, "Generating index for all files in " + input);

            for (File file : files) {
                index(file);
            }
        }

    }

    private Indexer() {
        // Empty private constructor
    }

    /**
     * Creates an index file for the specified archive in the same
     * directory. The index file will be named after the original
     * file plus and ".idx" ending.
     *
     * @param archive the archive to generate an index for
     * @throws IOException
     */
    public static void index(File archive) throws IOException {
        logger.log(Level.INFO, "Generating index for " + archive.getAbsolutePath());

        Map<Resource, Long> entries = parseZipFile(archive);

        Checksum checker = new CRC32();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, false);

        File indexFile = new File(
                archive.getParentFile(),
                archive.getName() + ".idx");

        DataOutputStream index = new DataOutputStream(
                new CheckedOutputStream(
                        new BufferedOutputStream(
                                new DeflaterOutputStream(
                                        new FileOutputStream(indexFile),
                                        deflater)
                        ),
                        checker)
        );

        for (Resource entry : entries.keySet()) {
            String name = entry.getName();
            long crc = entry.getCrc();
            long endOffset = entries.get(entry);

			/*
            System.out.println(
				name +
				" crc=0x" + Long.toHexString(crc).toUpperCase() +
				" end=" + endOffset);
			*/

            index.writeUTF(name);
            index.writeLong(crc);
            index.writeLong(endOffset);
        }

        index.writeUTF("");
        index.flush();

        long checksum = checker.getValue();
        index.writeLong(checksum);

        logger.log(Level.FINE,
                "Checksum for index data: 0x" +
                        Long.toHexString(checksum).toUpperCase());

        index.close();
    }

    /**
     * Manually parses a ZIP file without using any classes from
     * java.util.zip.* to create a list of minimal Resources in
     * the ZIP file including the end offsets of those entries.
     *
     * @param archive the ZIP file to parse
     * @throws IOException
     * @return a map of Resources and their end offsets in the ZIP file
     */
    public static Map<Resource, Long> parseZipFile(File archive)
            throws IOException {
        RandomAccessFile file = new RandomAccessFile(archive, "r");

		/*
		 * The following is ugly but necessary to account for ZIP file
		 * comments at the end of a file.
		 *
		 * Read backwards using a 44-byte array in 22-byte steps and search
		 * for EOCD signature, then just copy the 22 header bytes into an
		 * array of its own and give to EOCD constructor.
		 */
        byte[] sig = {0x50, 0x4B, 0x05, 0x06};
        byte[] buf = new byte[2 * END_OF_CENTRAL_DIRECTORY_LENGTH];

        int sigPos = -1;
        for (int i = 1; sigPos == -1; i++) {
            file.seek(file.length() - END_OF_CENTRAL_DIRECTORY_LENGTH - i * END_OF_CENTRAL_DIRECTORY_LENGTH);
            file.readFully(buf);
            sigPos = find(buf, sig);

			/*
			if(sigPos != -1)
				System.out.print("found sig at "+sigPos+" in run " + i + ": ");
			*/
        }

        byte[] endOfCentralDirectory = new byte[END_OF_CENTRAL_DIRECTORY_LENGTH];
        System.arraycopy(buf, sigPos, endOfCentralDirectory, 0, endOfCentralDirectory.length);

        EndOfCentralDirectory eocd = new EndOfCentralDirectory(endOfCentralDirectory);
        file.seek(eocd.centralDirectoryOffset);

		/*
		 * IMPORTANT: Use a Map implementation here that preserves
		 * **insertion order** because we rely on it for computing
		 * the start offset on the client later on!
		 */
        Map<Resource, Long> entries = new LinkedHashMap<>();

        Resource resource = null;

        byte[] bytes = new byte[CENTRAL_DIRECTORY_LENGTH];
        for (int i = 0; i < eocd.centralDirectoryRecordsAllDisks; i++) {
            file.readFully(bytes);

            CentralDirectoryRecord header = new CentralDirectoryRecord(bytes);

			/*
			 * Read name field
			 */
            byte[] nameBytes = new byte[header.nameLength];
            file.readFully(nameBytes);
            String name = new String(nameBytes, "us-ascii");

            file.skipBytes(header.extraLength);
            file.skipBytes(header.fileCommentLength);

			/*
			 * Take the *start* offset of the current resource minus one
			 * as *end* offset for the previous resource. This allows us
			 * to store only the end offset in the index, saving 4 bytes
			 * per resource.
			 */
            if (resource != null) {
                entries.put(resource, header.offsetToLocalFileHeader - 1L);
            }

            resource = new Resource(name);
            resource.setCrc(header.crc);
        }

		/*
		 * Don't forget the last resource!
		 */
        if (resource != null) {
            entries.put(resource, eocd.centralDirectoryOffset - 1L);
        }

        return entries;
    }


	/*
	 * Read compressed data and decompress it
	 *
	byte[] data = new byte[(int)compressedSize];

	File dataFile = new File(outputDir, name);
	if(compressedSize == 0)
	{
		dataFile.mkdirs();
		System.out.println(name + " created.");
	}
	else
	{
		dataFile.getParentFile().mkdirs();

		int dataSize = zipFile.read(data);
		if(dataSize != compressedSize)
		{
			throw new IOException("The monkey demands " + dataSize + " bananas.");
		}

		BufferedOutputStream out =
			new BufferedOutputStream(
				new FileOutputStream(dataFile));

		byte[] uncompressedData = new byte[(int)uncompressedSize];

		switch ((int)compressionMethod)
		{
			case METHOD_STORED:
				uncompressedData = data;
				break;

			case METHOD_DEFLATED:
				Inflater decompressor = new Inflater(true);
				decompressor.setInput(data);
				decompressor.inflate(uncompressedData);
				break;

			default:
				throw new IOException(
					"Unsupported compression method: " +
					hex(compressionMethod));
		}

		out.write(uncompressedData);
		out.flush();
		out.close();

		System.out.println(name + " written.");
	}
	*/

	/*
	private static void print(byte[] buf)
	{
		for (int i = 0; i < buf.length; i++)
		{
			System.out.print(Integer.toHexString(buf[i] & 0xFF).toUpperCase());
			System.out.print(" ");
		}
	}
	*/

    private static int find(byte[] source, byte[] key) {
        search:
        for (int i = 0; i < source.length - key.length; i++) {
            int k;
            for (k = 0; k < key.length; k++) {
                if (source[i + k] != key[k])
                    continue search;
            }

            return i + k - key.length;
        }

        return -1;
    }
}
