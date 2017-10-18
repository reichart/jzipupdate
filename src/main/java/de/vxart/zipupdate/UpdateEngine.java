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

import de.vxart.zipupdate.ui.MultiProgressDialog;
import de.vxart.zipupdate.ui.ProgressDialog;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The work horse of the whole update system containing the diff and patch
 * functionality as well as hooks to register listeners.<br>
 * Use the <code>de.vxart.zipupdate.UpdateEngine.loglevel</code> system property to set the log
 * level. Use the normal Java Logging levels (ALL, FINEST, CONFIG, FINER, FINE,
 * INFO, WARNING, SEVERE).
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 * @author Egal, egal (AT) mojang (DOT) com
 */
public class UpdateEngine {
    protected static Logger logger = Logger.getLogger("de.vxart.zipupdate");

    static {
        try {
            final String logLevel = System.getProperty("de.vxart.zipupdate.UpdateEngine.loglevel", "INFO");
            if (logLevel.length() > 2) {
                if (logLevel.equalsIgnoreCase("ALL")) {
                    logger.setLevel(Level.ALL);
                } else if (logLevel.equalsIgnoreCase("FINEST")) {
                    logger.setLevel(Level.FINEST);
                } else if (logLevel.equalsIgnoreCase("CONFIG")) {
                    logger.setLevel(Level.CONFIG);
                } else if (logLevel.equalsIgnoreCase("FINER")) {
                    logger.setLevel(Level.FINER);
                } else if (logLevel.equalsIgnoreCase("FINE")) {
                    logger.setLevel(Level.FINE);
                } else if (logLevel.equalsIgnoreCase("INFO")) {
                    logger.setLevel(Level.INFO);
                } else if (logLevel.equalsIgnoreCase("WARNING")) {
                    logger.setLevel(Level.WARNING);
                } else if (logLevel.equalsIgnoreCase("SEVERE")) {
                    logger.setLevel(Level.SEVERE);
                }
            } else {
                logger.setLevel(Level.INFO);
            }
            logger.setUseParentHandlers(false);
            logger.addHandler(new ConsoleHandler());
            logger.addHandler(new FileHandler("%t/jzipupdate.log"));
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(logger.getLevel());
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to register log file", ex);
        }
    }

    private ProgressListenerManager multiListeners;
    private ProgressListenerManager listeners;


    /**
     * Provides a basic stand-alone way to update archives.
     */
    public static void main(String[] args)
            throws Exception {
        if (args.length < 2) {
            System.err.println(
                    "Usage: java de.vxart.zipupdate.UpdateEngine" +
                            " <ZIP file> <update URL>");
            System.err.println(
                    "   or: java de.vxart.zipupdate.UpdateEngine" +
                            " <directory> <update base URL>");
            System.exit(1);
        }

		/*
         * Use native LAF if possible
		 */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to enable native LAF: " + ex.getMessage());
        }

	    /*
	     * Parse arguments
	     */
        File input = new File(args[0]);

        URL url = null;
        try {
            url = new URL(args[1]);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Invalid URL: " + args[1]);
            System.exit(2);
        }

	    /*
		 * Create update engine and register graphical listener
		 */
        UpdateEngine engine = new UpdateEngine();

        if (input.isDirectory()) {
			/*
			 * Filter out anything but ZIP and JAR files
			 */
            File[] files = input.listFiles(
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            name = name.toLowerCase();
                            return name.endsWith(".zip") || name.endsWith(".jar");
                        }
                    });

            if (files.length < 1) {
                logger.log(Level.WARNING, "No ZIP/JAR files found at " + input.getAbsolutePath());
                System.exit(3);
            }

            MultiProgressDialog mpd = new MultiProgressDialog();
            mpd.init("Monkeys!");
            engine.addProgressListener(mpd);

            ZipFile[] archives = new ZipFile[files.length];
            UpdateLocation[] locations = new UpdateLocation[files.length];
            String[] messages = new String[files.length];

            for (int i = 0; i < files.length; i++) {
                try {
                    archives[i] = new ZipFile(files[i]);
                    locations[i] = new UpdateLocation(new URL(url, files[i].getName()));
                    messages[i] = files[i].getName();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Failed to initialize arguments for: " + files[i].getAbsolutePath());
                    System.exit(4);
                }
            }

            engine.update(archives, locations, messages);
        } else {
            if (!input.exists()) {
				/*
				 * Create a minimal dummy ZIP file as update target.
				 */
                logger.log(Level.WARNING, "Update target doesn't exist, creating dummy: " + input.getAbsolutePath());

                ZipOutputStream zos = new ZipOutputStream(
                        new FileOutputStream(input));
                zos.putNextEntry(new ZipEntry("banana"));
                zos.write(0xCA);
                zos.write(0xFE);
                zos.closeEntry();
                zos.close();
            }

            ZipFile archive = new ZipFile(input);
            engine.addProgressListener(new ProgressDialog());
            engine.update(archive, new UpdateLocation(url));
        }
    }

    /**
     * Constructs a new UpdateEngine instance.
     */
    public UpdateEngine() {
        this.listeners = new ProgressListenerManager();
        this.multiListeners = new ProgressListenerManager();
    }

    /**
     * Updates multiple ZIP files from the given URLs.
     * <p>
     * Displays a message for each ZIP file as it is
     * being updated; if there are not enough messages
     * for all ZIP files or the messages are NULL, the
     * file name will be used.
     *
     * @param archives  the ZIP files to update
     * @param locations the locations where to look for the up-to-date ZIP files
     * @param messages  the messages to display for each ZIP file
     * @return the number of ZIP files that have actually been updated
     */
    public int update(ZipFile[] archives, UpdateLocation[] locations, String[] messages)
            throws IOException {

        if (
                archives.length != locations.length ||
                        locations.length != messages.length ||
                        locations.length != archives.length) {
            throw new IllegalArgumentException(
                    "Argument arrays are unequal in length: archives="
                            + archives.length + ", locations="
                            + locations.length + ", messages="
                            + messages.length);
        }

        multiListeners.init(
                "Updating " + archives.length + " archives...",
                0,
                archives.length);

        int updated = 0;
        for (int i = 0; i < archives.length; i++) {
            multiListeners.label(messages[i]);

            if (update(archives[i], locations[i]))
                updated++;

            multiListeners.update(i + 1);
        }

        multiListeners.finish();

        return updated;
    }

    /**
     * Updates a ZIP file from a given URL.
     *
     * @param archive  the ZIP file to update
     * @param location the location where to look for the up-to-date ZIP file
     * @return true if any updates have actually been performed, false otherwise
     */
    public boolean update(ZipFile archive, UpdateLocation location)
            throws IOException {
        logger.log(Level.INFO, "TODO Updating " + archive.getName() + " from " + location.getUrl());

		/*
		 * Register any listeners on the UpdateLocation as well.
		 *
		 * TODO Why won't a simplified for(Foo f : Bar) work here?!
		 */
        Iterator<ProgressListener> it = listeners.iterator();
        while (it.hasNext()) {
            location.addProgressListener(it.next());
        }

        listeners.init("Initializing...");

		/*
		 * Initalize the patch set
		 */
        logger.log(Level.FINE, "Initializing patch set...");
        startTimer();
        Set<Resource> client = init(archive);
        logger.log(Level.FINE, "Initialized patch set (" + stopTimer() + " ms)");

		/*
		 * Fetch server-side CRC list
		 */
        logger.log(Level.FINE, "Fetching server-side CRC list...");
        startTimer();
        Set<Resource> server = location.getResources();
        logger.log(Level.FINE, "Fetched server-side CRC list (" + stopTimer() + " ms)");

		/*
		 * Diff contents of ZIP files
		 */
        logger.log(Level.FINE, "Diffing " + (server.size() + client.size()) + " items...");
        startTimer();
        Map<Resource, String> diff = diff(client, server);
        logger.log(Level.FINE, "Diffing finished (" + stopTimer() + " ms)");

		/*
		 * Some nice output for the people watching the program run :)
		 */

        logger.log(Level.FINE, "Total items on server: " + server.size());
        logger.log(Level.FINE, "Total items on client: " + archive.size());
        printDiff(diff);

		/*
		 * Patch the ZIP file
		 */
        logger.log(Level.FINE, "Patching " + archive.getName() + "...");
        startTimer();
        boolean patched = patch(archive, diff, location);
        if (!patched) {
            logger.log(Level.INFO, "No update necessary for " + archive.getName() + " (" + stopTimer() + " ms)");
        } else {
            logger.log(Level.INFO, "Updated " + archive.getName() + " (" + stopTimer() + " ms)");
        }

        listeners.finish();

        return patched;
    }

    private static void printDiff(Map<Resource, String> diff) {
        if (logger.isLoggable(Level.FINE)) {
            int add, upd, noop, rem;
            add = upd = noop = rem = 0;

            logger.log(Level.FINE, "Final patch set size: " + diff.size());

            logger.log(Level.FINE, "This is what I would do:");

            if (diff.size() < 1) {
                logger.log(Level.FINE, "\tKeep all. No patching necessary.");
                return;
            }

            for (Resource resource : diff.keySet()) {
                String flag = diff.get(resource);

                if (flag == Resource.FLAG_NOOP) {
                    noop++;
                    continue;
                } else if (flag == Resource.FLAG_ADD)
                    add++;
                else if (flag == Resource.FLAG_UPDATE)
                    upd++;
                else if (flag == Resource.FLAG_REMOVE)
                    rem++;
                else
                    assert false : ("Undefined flag: " + flag);

                logger.log(Level.FINER, "\t" + flag + " " + resource.getName());
            }

            logger.log(Level.FINE,
                    "add: " + add + " " +
                            "update: " + upd + " " +
                            "remove: " + rem + " " +
                            "keep: " + noop);
        }
    }

    /**
     * Initializes a resource set for a client-side ZipFile
     * by creating Resource intances for every ZipEntry.
     * <p>
     * Note: The resource set returned is in the same order as
     * the ZIP entries in the ZIP file. The ordering must
     * not be changed or the InputStreams to the ZIP
     * entries are invalidated˙
     *
     * @param archive the ZIP file to create a resource set for
     * @return
     */
    private Set<Resource> init(ZipFile archive) {
        assert archive != null;

        Set<Resource> patchSet = new LinkedHashSet<Resource>();

        for (Enumeration<? extends ZipEntry> entries = archive.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = entries.nextElement();

            Resource resource = new Resource(entry.getName());
            resource.setCrc(entry.getCrc());

            try {
                resource.setData(archive.getInputStream(entry));
            } catch (Exception e) {
                throw new RuntimeException("Unable to open stream to " + entry.getName());
            }

            patchSet.add(resource);
        }

        return patchSet;
    }

    /**
     * Creates a diff from two resource sets with instructions to update
     * the resource set given as first parameter to the resource set
     * given as second.
     *
     * @param client the resource set to be updated
     * @param server the "reference" resource set to be updated to
     * @return
     */
    private Map<Resource, String> diff(Set<Resource> client, Set<Resource> server) {
        Map<Resource, String> diff = new HashMap<Resource, String>(client.size());

		/*
		 * Populate diff with all client resource flagged REMOVE.
		 */
        for (Resource resource : client) {
            diff.put(resource, Resource.FLAG_REMOVE);
        }

		/*
		 * Loop over all server resources and diff them with the
		 * client resources to get the patch information we need.
		 */
        for (Resource serverResource : server) {
            String name = serverResource.getName();

            Resource clientResource = null;
            for (Resource _clientResource : client) {
                if (_clientResource.getName().equals(name)) {
                    clientResource = _clientResource;
                    break;
                }
            }

            if (clientResource == null) {
				/*
				 * Resource new to client, flag ADD.
				 */
                diff.put(serverResource, Resource.FLAG_ADD);
            } else {
                long serverCRC = serverResource.getCrc();
                long clientCRC = clientResource.getCrc();

                if (serverCRC == clientCRC) {
					/*
					 * Resources are the same.
					 * Remove it from the diff because only differences go
					 * in here; this also keeps the diff small speeding up
					 * lookups and allows patching to fail fast if nothing
					 * needs to be patched.
					 */
                    diff.remove(clientResource);
                } else {
					/*
					 * Resource changed, update required.
					 */
                    diff.put(clientResource, Resource.FLAG_UPDATE);
                }
            }
        }

        return diff;
    }

    /**
     * Patches a ZipFile from an UpdateLocation using the specified diff.
     *
     * @param archive  ZIP file to be updated
     * @param diff     diff containing update instructions
     * @param location location from which the ZIP file is to be updated
     * @throws IOException if any IO error occured during downloading, parsing or patching
     * @return true if the ZIP file has been patched, false if nothing has been done (i.e. file is up to date)
     */
    private boolean patch(ZipFile archive, Map<Resource, String> diff, UpdateLocation location)
            throws IOException {
		/*
		 * Nothing to do.
		 */
        if (diff.size() < 1)
            return false;

		/*
		 * For compatibility with JAR files, the first entry needs
		 * to be the manifest file (if it exists).
		 *
		 * As both sources, the local file and the downloaded resources,
		 * use the same ordering one of them will always start with the
		 * manifest entry. So we just need to figure out which one it is
		 * and start the patching from that source.
		 */
        final String MANIFEST = "META-INF/MANIFEST.MF";
        boolean remoteFirst = false;

        for (Resource resource : diff.keySet()) {
            String name = resource.getName();
            if (name.equals(MANIFEST)) {
                String flag = diff.get(resource);
                remoteFirst = flag.equals(Resource.FLAG_ADD) || flag.equals(Resource.FLAG_ADD);
                break;
            }
        }

		/*
		 * Create a tmp file in the same directory as they original
		 * file so that it can be quickly renamed after patching.
		 */
        File tmpFile = new File(archive.getName() + ".tmp");
        if (!tmpFile.delete() && tmpFile.exists()) {
            throw new IOException("Failed to delete existing tmp file: " + tmpFile);
        }

        ZipOutputStream zipFile = new ZipOutputStream(
                new FileOutputStream(tmpFile));

		/*
		 * Fetch any resources that need to be updated/added
		 */
        location.fetchData(diff);

        Iterator<Resource> serverResources = location.getData(diff);

		/*
		 * Init progress listeners for patching
		 */
        int items = archive.size() + diff.size();
        listeners.init("Patching...", 0, items);

        logger.log(Level.FINE, "Starting to patch...");

		/*
		 * Start patching
		 */
        if (remoteFirst) {
            logger.log(Level.FINER, "Patching first from REMOTE source.");
            patchRemotely(zipFile, diff, serverResources);
            patchLocally(zipFile, diff, archive);
        } else {
            logger.log(Level.FINER, "Patching first from LOCAL source.");
            patchLocally(zipFile, diff, archive);
            patchRemotely(zipFile, diff, serverResources);
        }

        logger.log(Level.FINE, "Finalizing patched file...");
        listeners.init("Finalizing...");

        zipFile.close();

		/*
		 * Close archive or the renaming below will fail!
		 */
        archive.close();

        logger.log(Level.FINE, "Finalized patched file.");

        File originalFile = new File(archive.getName());
        File backupFile = new File(archive.getName() + ".bck");

        logger.log(Level.FINE, "Replacing original by patched file...");

        if (originalFile.renameTo(backupFile)) {
            if (tmpFile.renameTo(originalFile)) {
                if (!backupFile.delete())
                    logger.log(Level.WARNING, "Failed to delete backup: " + backupFile);
            } else {
                throw new IOException("Failed to move patched file into place: " + tmpFile);
            }
        } else {
            throw new IOException("Failed to backup original file: " + originalFile);
        }

        return true;
    }

    /**
     * Copies or skips entries from original ZIP file
     * to satisfy any NOOP or REMOVE instructions.
     *
     * @param zipFile stream to patched ZIP file
     * @param diff    diff containing update information
     * @param archive the original archive to be copied from
     * @throws IOException
     */
    private void patchLocally(ZipOutputStream zipFile, Map<Resource, String> diff, ZipFile archive)
            throws IOException {
        logger.log(Level.FINER, "Patching with local resources...");

        byte[] buf = new byte[4096];

        Enumeration<? extends ZipEntry> entries = archive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            String name = entry.getName();

            listeners.update(listeners.getProgress() + 1);

			/*
			 * Not the fastest way to look up how a
			 * Resource is flagged but good enough.
			 */
            String flag = Resource.FLAG_NOOP;
            for (Resource resource : diff.keySet()) {
                if (name.equals(resource.getName())) {
                    flag = diff.get(resource);
                    break;
                }
            }

            if (Resource.FLAG_REMOVE.equals(flag)) {
                logger.log(Level.FINEST, "\t--- " + name);
                continue;
            } else if (Resource.FLAG_NOOP.equals(flag)) {
                logger.log(Level.FINEST, "\t=== " + name);

                zipFile.putNextEntry(new ZipEntry(name));

                BufferedInputStream in = new BufferedInputStream(archive.getInputStream(entry));

                int len;
                while ((len = in.read(buf)) != -1) {
                    zipFile.write(buf, 0, len);
                }

                zipFile.closeEntry();
                in.close();
            }
        }
    }

    /**
     * Downloads and add any resources from the server
     * to satisfy any ADD or UPDATE instructions.
     *
     * @param zipFile  stream to patched ZIP file
     * @param diff     diff containing update information
     * @param location location to download new/updated data from
     * @throws IOException
     */
    private void patchRemotely(ZipOutputStream zipFile, Map<Resource, String> diff, Iterator<Resource> serverResources)
            throws IOException {
        if (serverResources == null) {
            logger.log(Level.FINE, "No patching with remote resources required.");
            return;
        }

        logger.log(Level.FINER, "Patching with remote resources...");

        byte[] buf = new byte[4096];
        while (serverResources.hasNext()) {
            Resource resource = serverResources.next();
            String name = resource.getName();

            logger.log(Level.FINEST, "\t+++/!!! " + name);

            listeners.update(listeners.getProgress() + 1);

            ZipEntry entry = new ZipEntry(name);
            zipFile.putNextEntry(entry);

            BufferedInputStream in = new BufferedInputStream(resource.getData());

            int len;
            while ((len = in.read(buf)) != -1) {
                zipFile.write(buf, 0, len);
            }

            zipFile.closeEntry();

            in.close();
        }
    }

    /*
     * ---
     * Utility methods for timing
     */
    private static long startTime;

    private static void startTimer() {
        startTime = System.currentTimeMillis();
    }

    private static long stopTimer() {
        assert startTime != 0 : "Called stopTimer() before startTimer()";
        long time = System.currentTimeMillis() - startTime;
        startTime = 0;
        return time;
    }
	/* ---
	 */

    /**
     * Registers a ProgressListener with this instance of an UpdateEngine.
     */
    public void addProgressListener(ProgressListener listener) {
		/*
		 * For MultiProgressListeners, get the actual "inner"
		 * ProgressListener that represents the overall progress
		 * and register it in the multi-listener list.
		 */
        if (listener instanceof MultiProgressListener) {
            multiListeners.add(((MultiProgressListener) listener).getOverallProgressListener());
        }

        listeners.add(listener);
    }

    /**
     * Unregisters a ProgressListener with this instance of an UpdateEngine.
     */
    public void removeProgressListener(ProgressListener listener) {
		/*
		 * For MultiProgressListeners, get the actual "inner"
		 * ProgressListener that represents the overall progress
		 * and unregister it from the multi-listener list.
		 */
        if (listener instanceof MultiProgressListener) {
            multiListeners.remove(((MultiProgressListener) listener).getOverallProgressListener());
        }

        listeners.remove(listener);
    }
}
