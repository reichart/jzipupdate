# JZipUpdate

JZipUpdate is a Java library to update ZIP and JAR files incrementally over HTTP
transferring only the data that has changed in compressed form.

Does not require any pre- or on-the-fly compression or any application/code
running server-side except a HTTP 1.1-compliant web server and an index file
created at time of deployment.


## How to use

JZipUpdate can be used both as stand-alone application from a command line, and
of course programmatically from your own code.

The current stand-alone application always creates a UI progress dialog, so it's
currently not possible to run in a pure text-based environment like terminals.


### Programmatically Updating ZIP/JAR Files
 
Updating a single ZIP file:

```
ZipFile archive = new ZipFile(...);
URL url = new URL("http://www.example.com/files/bar.zip");

UpdateEngine engine = new UpdateEngine();
engine.update(archive, new UpdateLocation(url));
```

Updating multiple ZIP files:

```
ZipFile[] archives = ...;                    
UpdateLocation[] locations = ...;            
String[] messages = ...;                     
                                             
UpdateEngine engine = new UpdateEngine();    
engine.update(archives, locations, messages);
```

### Manually Updating ZIP/JAR Files from the Command Line
 
Updating a single archive (replace the x.y by the actual version you're using):

    java -jar jzipupdate-x.y.jar archive.zip http://www.example.com/files/bar.zip

or for multiple archives:

    java -jar jzipupdate-x.y.jar /somedirectory-with-ZIP-files/ http://www.example.com/files/

Note that when updating multiple files from the command line, you specify a
directory containing archives rather than a list of archives, and you provide a
"base URL" to which the name of any archive found in the directory will be
appended.

For example, when the directory contains `monkey.zip` and `banana.jar`,
JZipUpdate will update from `http://www.example.com/files/monkey.zip` and
`http://www.example.com/files/banana.jar` respectively.


## License

JZipUpdate is licensed under the open-source [Apache 2.0 license](LICENSE).