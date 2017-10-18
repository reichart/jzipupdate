/*
 * Created 2005 by Philipp Reichart, philipp.reichart@vxart.de
 *
 * The contents of this file are donated to Mojang Specifications.
 * All rights by the original author dismissed.
 * Use, distribute and flavor at will :)
 */
package de.vxart.wurm;

import de.vxart.zipupdate.UpdateEngine;
import de.vxart.zipupdate.UpdateLocation;
import de.vxart.zipupdate.ui.MultiProgressDialog;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * A wrapper for JZIPUpdate to easily update Wurm Online pack files.
 *
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class PackUpdateEngine {
    private static Logger logger = Logger.getLogger(PackUpdateEngine.class.getName());

    public final static String PACK_DESCRIPTOR = "pack.txt";


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java de.vxart.wurm.PackUpdateEngine <pack file | directory>");
            System.exit(1);
        }

        File input = new File(args[0]);

        File[] files = null;

        if (input.isFile()) {
            logger.log(Level.INFO, "Generating index for " + input);
            files = new File[1];
            files[0] = input;
        } else if (input.isDirectory()) {
            files = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    name = name.toLowerCase();
                    return name.endsWith(".zip") || name.endsWith(".jar");
                }
            });
        } else {
            logger.log(Level.WARNING, "Neither directory nor ZIP/JAR file: " + input);
            System.exit(2);
        }

        try {
            update(files);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to update packs", ex);
        }
    }

    public static void update(File[] files)
            throws IOException, IllegalArgumentException {
        ZipFile[] archives = new ZipFile[files.length];
        UpdateLocation[] locations = new UpdateLocation[files.length];
        String[] messages = new String[files.length];

        for (int i = 0; i < files.length; i++) {
            archives[i] = new ZipFile(files[i]);
            Properties pack = getPackDescriptor(archives[i]);

            String urlString = pack.getProperty("url").replaceAll("www.wurmonline.com", "wurm.vxart.de");
            locations[i] = new UpdateLocation(new URL(urlString));
            messages[i] = pack.getProperty("description");
        }

        UpdateEngine engine = new UpdateEngine();
        engine.addProgressListener(new MultiProgressDialog());
        engine.update(archives, locations, messages);
    }

    private static Properties getPackDescriptor(ZipFile archive)
            throws IOException {
        InputStream stream = archive.getInputStream(
                archive.getEntry(PACK_DESCRIPTOR));

        Properties packDescriptor = new Properties();
        packDescriptor.load(stream);

        return packDescriptor;
    }
}
