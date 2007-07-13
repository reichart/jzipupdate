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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import java.util.zip.InflaterInputStream;

import de.vxart.io.ThrottledInputStream;
import de.vxart.io.ZipEntryInputStream;
import de.vxart.net.MultipartMessage;

/**
 * Encapsulates an URL-based location which holds the up-to-date version
 * of an archive and provides the fine-grained access functionality to a
 * remote archive's contents.
 * 
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class UpdateLocation
{
	protected Logger logger = UpdateEngine.logger;
	
	private final static String CONTENT_TYPE = "Content-Type";
	private final static String BOUNDARY_DELIM = "boundary=";
	
	private static long DOWNLOAD_SPEED;
	
	private URL url;
	private Set<Resource> resources; 
	
	private ProgressListenerManager listeners;
	
	private Map<Map<Resource, String>, CacheEntry> cache;
	
	private final Map<String, Range> namedRanges;
	final Map<String, String> rangedNames;
	
	
	/**
	 * Creates a new UpdateLocation sourced from the specified URL.
	 * 
	 * @param url the URL to use as source of the up-to-date data
	 */
	public UpdateLocation(URL url)
	{
		this.url = url;
		
		try
		{
			String prop = System.getProperty("de.vxart.zipupdate.UpdateLocation.downloadSpeed");
			DOWNLOAD_SPEED = Long.parseLong(prop);
			
			if(DOWNLOAD_SPEED < 0)
				throw new IllegalAccessException("Illegal value: " + prop);
			
			logger.log(Level.CONFIG, "Enabling throttling: " + DOWNLOAD_SPEED + " KB/s max");
		}
		catch (Exception ex)
		{
			logger.log(Level.CONFIG, "Disabling throttling: " + ex.getMessage());
			DOWNLOAD_SPEED = -1;
		}
		
		this.listeners = new ProgressListenerManager();
		this.cache = new HashMap<Map<Resource, String>, CacheEntry>();
		this.namedRanges = new HashMap<String, Range>();
		this.rangedNames = new HashMap<String, String>();
		this.resources = new LinkedHashSet<Resource>();
	}
	
	public URL getUrl()
	{
		return url;
	}
	
	/**
	 * Fetches the resources available from this UpdateLocation
	 * 
	 * @return the resources available from this UpdateLocation
	 * @throws IOException
	 */
	public Set<Resource> getResources()
		throws IOException
	{
		resources.clear();
		namedRanges.clear();
		rangedNames.clear();
		
		Checksum checker = new CRC32();
		
		DataInputStream index = new DataInputStream(
			new CheckedInputStream(
				new InflaterInputStream(
					new BufferedInputStream(
						new URL(url.toString() + ".idx").openStream()
					)
				), checker)
			);
		
		/*
		 * Read all the resource meta-data until we reach the
		 * empty name marking the end of the resource list.
		 */
		String name;
		long previousEndOffset = -1;
		
		while(!"".equals(name = index.readUTF()))
		{
			/*
			 * Create a Resource for the index Set
			 * we're going to return.
			 */
			Resource resource = new Resource(name);
			resource.setCrc(index.readLong());
			resources.add(resource);
			
			/*
			 * Store the start and end offsets for all
			 * Resources away from the index, no need for
			 * the higher-level stuff to know about it.
			 */
			long endOffset = index.readLong();
			
			Range range = new Range(previousEndOffset, endOffset);
			rangedNames.put(range.toString(), name);
			namedRanges.put(name, range);
			
			previousEndOffset = endOffset;
		}
		
		/*
		 * Finally get both the checksum computed from the
		 * input stream as well as the stored checksum and
		 * see if they match. Most data corruption will be
		 * catched by the inflater anyway.
		 */
		long computedChecksum = checker.getValue();
		long storedChecksum = index.readLong();
		
		if(computedChecksum != storedChecksum)
		{
			throw new IOException("Index file corrupted or out-of-date: " + url);
		}
		
		return resources;
	}
	
	/**
	 * Fetches any data required by the specified diff into a temporary cache. 
	 * 
	 * @param diff the diff to fetch data for
	 */
	public void fetchData(Map<Resource, String> diff)
		throws IOException
	{
		/*
		 * We have to sort the byte-ranges of the resource we're
		 * going to download for these reasons:
		 * 
		 * 1) This guarantees that the ordering of entries we're going to
		 *    download will be the *same* as in the remote ZIP file.
		 *    This is important when udpating JAR files where the manifest
		 *    file has to be the first entry.
		 * 
		 * 2) If the byte-ranges in the HTTP request hit the server in
		 *    *ascending* order, the server can reply more quickly because
		 *    it has to do only forward-seeking in the ZIP file (no jumping
		 *    around to provide the ranges).
		 */
		SortedSet<Range> sortedRanges = new TreeSet<Range>(); 
		for(Resource resource : diff.keySet())
		{
			String flag = diff.get(resource);
			if(flag == Resource.FLAG_ADD || flag == Resource.FLAG_UPDATE)
				sortedRanges.add(namedRanges.get(resource.getName()));
		}
		
		if(sortedRanges.size() == 0)
		{
			return;
		}
		
		/*
		 * Build the byte ranges header.
		 */
		StringBuilder byteRangesHeader = new StringBuilder("bytes=");
		
		int estimatedSize = 0;
		
		//System.out.println("### Resources to be downloaded:");
		
		boolean first = true;
		for(Range range : sortedRanges)
		{
			if(!first)
			{
				byteRangesHeader.append(',');
			}
			
			//System.out.println("### \t" + range + "\t\t\t" + rangedNames.get(range.toString()));
			
			byteRangesHeader.append(range.start+1);
			byteRangesHeader.append('-');
			byteRangesHeader.append(range.end);
			
			estimatedSize += range.end-range.start;
			
			first = false;
		}
		
		/*
		 * Connect to URL. 
		 */
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestProperty("Range", byteRangesHeader.toString());
		conn.connect();
		
		/*
		 * Download the data into a temp file.
		 */
		InputStream remote;
        // TODO Egal added try-catch
        try {
            remote = conn.getInputStream();
        } catch (IOException e) {
            logger.log(Level.INFO, "IOException while connecting to source: " + url + " , " + e.getMessage());
            final Map lHeaderFields = conn.getHeaderFields();
            final Iterator lHeaders = lHeaderFields.keySet().iterator();
            while (lHeaders.hasNext()) {
                Object lHeaderObject = lHeaders.next();
                logger.log(Level.INFO, "HTTPResponseHeader - " + lHeaderObject + ": " + lHeaderFields.get(lHeaderObject));
            }
            throw e;
        }
		
		if(DOWNLOAD_SPEED > 0)
			remote = new ThrottledInputStream(remote, DOWNLOAD_SPEED);
		
		remote = new BufferedInputStream(remote);
		File cacheFile = File.createTempFile("banana", null);
		cacheFile.delete();
		cacheFile.deleteOnExit();
		
		logger.log(Level.FINE, "Downloading data into cache: source=" + url + " cache=" + cacheFile.getAbsolutePath());
		
		byte[] buf = new byte[4096];
		int len;
		int bytesRead = 0;
		
		listeners.init("Downloading new resources...", 0, estimatedSize);
		
		OutputStream cacheOut = new FileOutputStream(cacheFile);
		while((len = remote.read(buf)) != -1)
		{
			cacheOut.write(buf, 0, len);
			bytesRead += len;
			listeners.update(bytesRead);
		}
		
		CacheEntry cacheEntry = new CacheEntry();
		cacheEntry.file = cacheFile;
		cacheEntry.headers = conn.getHeaderFields();
		cache.put(diff, cacheEntry);
		
		logger.log(Level.FINE, "Downloaded data successfully: source=" + url + " cache=" + cacheFile.getAbsolutePath());
	}
	
	/**
	 * Provides an Iterator over any remote Resources
	 * that are flagged as ADD or UPDATE.
	 * 
	 * A call to the hasNext() or next() methods of the Iterator returned by
	 * this method will automatically close the InputStream of the previous
	 * Resource.
	 * 
	 */
	public Iterator<Resource> getData(Map<Resource, String> diff)
		throws IOException
	{
		CacheEntry cacheEntry = cache.get(diff);
		
		if(cacheEntry == null)
		{
			return null;
		}
			
		InputStream input = new FileInputStream(cacheEntry.file);
        
		/*
		 * If only a single resource got requested, the server will respond
		 * with a normal request containing only the byte-range data as body,
		 * no need to run the multipart parser all over it.
		 */
		int remoteResources = 0;
		for(Resource resource : diff.keySet())
		{
			String flag = diff.get(resource);
			if(flag == Resource.FLAG_ADD || flag == Resource.FLAG_UPDATE)
				remoteResources++;
		}
		
		if(remoteResources == 1)
		{
			/*
			 * Ugly but it works ;)
			 */
			InputStream data = null;
			
			try
			{
				data = new ZipEntryInputStream(new DataInputStream(input));
			}
			catch (IOException ioex)
			{
				throw new RuntimeException(ioex);
			}
			
			Resource resource = new Resource(diff.keySet().iterator().next().getName());
			resource.setData(data);
			
			List<Resource> list = new LinkedList<Resource>();
			list.add(resource);
			
			return list.iterator();
		}
		
		String contentType = cacheEntry.headers.get(CONTENT_TYPE).get(0);
		String boundary = getBoundary(contentType);
		
        /*
         * Parse the multipart response from the server and wrap
         * it into a custom Iterator to be passed to the caller.
         */
        final MultipartMessage multi = new MultipartMessage(input, boundary);
        
		return new Iterator<Resource>()
		{
			public boolean hasNext()
			{
				return multi.hasNext();
			}

			public Resource next()
			{
				MultipartMessage.Part part = multi.next();
				
				String rangeHeader = part.getHeaders().get(
					"Content-Range".toLowerCase());
				
				String range = rangeHeader.substring(
					rangeHeader.indexOf(' ')+1,
					rangeHeader.indexOf('/'));
				
				String name = rangedNames.get(range);
				
				InputStream data = null;
				
				try
				{
					data = new ZipEntryInputStream(
						new DataInputStream(part.openStream()));
				}
				catch (IOException ioex)
				{
					throw new RuntimeException(ioex);
				}
				
				Resource resource = new Resource(name);
				resource.setData(data);
				return resource;
			}
			
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
			
			public String toString()
			{
				return getClass().getName() + ":Iterator[multipart]";
			}
		};
	}
	
	private static String getBoundary(String contentType)
	{
		/*
		 * Find the beginning of the boundary
		 * in the content type header.
		 */
		int boundaryIndex = contentType.indexOf(BOUNDARY_DELIM);
        if (boundaryIndex < 0)
        {
            throw new NullPointerException("No boundary was found.");
        }
        
        String boundary = null;
        
        /*
		 * Look for a charset component following the boundary
		 * in the content type header.
		 */
		int charsetIndex = contentType.indexOf(";", boundaryIndex);
        if(charsetIndex > 0)
        {
        	/*
        	 * There is a charset component, so chop
        	 * that off from the boundary.
        	 */
        	boundary = contentType.substring(
        		boundaryIndex + BOUNDARY_DELIM.length(),
        		charsetIndex);
        }
        else
        {
        	/*
        	 * No charset, boundary goes up to the end of the header.
        	 */
        	boundary = contentType.substring(
        		boundaryIndex + BOUNDARY_DELIM.length());
        }
        
        return boundary;
	}
	
	/*
	private static void copy(InputStream in, OutputStream out)
		throws IOException
	{
		byte[] buf = new byte[4096];
		int len;
		
		while((len = in.read(buf)) != -1)
		{
			out.write(buf, 0, len);
		}
	
	}
	*/
	
	/**
	 * Encapsulates the cached data and headers
	 * downloaded for a specific diff.
	 */
	protected class CacheEntry
	{
		File file;
		Map<String, List<String>> headers;
	}
	
	/**
	 * Encapsulates a numerical range between two long values.
	 * 
	 * Note: This class has a natural ordering that is
     *       inconsistent with equals.
	 */
	protected class Range implements Comparable
	{
		long start, end;
		
		Range(long start, long end)
		{
			this.start = start;
			this.end = end;
		}
		
		public String toString()
		{
			return (start+1) + "-" + end;
		}

		public int compareTo(Object o)
		{
			if (o instanceof Range)
			{
				Range r = (Range)o;
				return (int)start - (int)r.start;
			}
			else
			{
				throw new ClassCastException("Different class: " + o.getClass()); 
			}
		}
	}
	
	public void addProgressListener(ProgressListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeProgressListener(ProgressListener listener)
	{
		listeners.remove(listener);
	}
	
	public String toString()
	{
		String speed =
			(DOWNLOAD_SPEED > 0)
				? String.valueOf(DOWNLOAD_SPEED) + " KB/s"
				: "no throttling";
		return getClass().getName() + "["+url+";" + speed + "]";
	}
}
