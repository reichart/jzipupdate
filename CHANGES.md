# Change Log

## Version 0.9

* Highly reduced spam on System.out and System.err by using
  the Java Logging API, a single log file will be created
  in the temp directory, only four lines per archive end up
  in the console
* Fixed a bug with parsing the mulitpart MIME boundary from
  HTTP Content Type response headers that don't include a
  charset attribute. This caused the multipart parser to
  throw in readLine().

## Version 0.8

* This is going to be the first publicly released version
  available on SourceForge.net
* Full compatibility with JAR files
  (requires META-INF/MANIFEST.MF to always be first entry)
* Full compatibility with sealed and even signed JAR files
  (w00t!)
* Easy-to-use API and progress UI for updates of multiple
  ZIP/JAR files

## Version 0.7

* Download method doesn't fail when resources only need to
  be removed from the client-side
  (no more 416 response code from server because of empty
  byte-range header)

## Version 0.6

* Finally works when a single resource gets downloaded
  (don't use multipart parser but plain data payload)

## Version 0.5

* Fix Indexer to account for ZIP file comments which make
  the End of Central Directory block start before filesize -
  22 bytes.
  (makes it more compliant to the ZIP file format spec and
  allows for archives with ZIP comment)

## Version 0.4

* The backup-rename stuff at the end of the patch method now
  works on Windows, too
  (there were some problems with moving around open files)
  
* The main() method now creates a dummy ZIP file if the
  update target doesn't exist
  The update() method of the public API still won't work
  with non-existing files, because it's supposed to update,
  not create; you'll have a hard time creating a ZipFile
  instance for a non-existing file anyway ;)

## Version 0.3

* The change to the Indexer to be more compatible included a
  rewrite of the manual ZIP parsing routines which made the
  code a lot cleaner and easier to maintain (client-side
  parser and indexer finally use a single code base)
  
* Indexer and ZIP entry parser now work with ZIP files
  created in non-seekable mode (like those by
  java.util.zip.ZipOutputStream) by using the Central
  Directory instead of the Local File Headers to gather the
  required information (name, CRC and offset)
  (The parser somehow didn't need any fixing at all and
  worked happily with non-seekable-mode files
  out-of-the-box; I blame it on the Inflater knowing by
  itself when it got enough data)

## Version 0.2

* no more "LFH magic number" crash
  (first resource transferred from server was truncated
  because of wrong offset)

## Version 0.1

* initial release