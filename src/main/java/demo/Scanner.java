package demo;

import demo.filters.BinaryFilterReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class Scanner {
    private static final String DEFAULT_SOURCE_CHARACTER_ENCODING = "ISO-8859-1";
    private static final String CONTENT_TYPE_RE = "((?:[a-z][a-z0-9_]*))\\/((?:[a-z][a-z0-9_]*))((;.*?(charset)=((?:[a-z][a-z0-9_\\-]+)))*)";
    private static final Pattern contentTypePattern = Pattern.compile(CONTENT_TYPE_RE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static Logger log = LogManager.getLogger(Scanner.class);
    private final Parser parser;
    private final String nameOfIndexDirectory;

    Scanner(File tikaConfigFile, final String nameOfIndexDirectory) throws Exception {
        TikaConfig config = new TikaConfig(tikaConfigFile);
        // Detector detector = config.getDetector();
        this.nameOfIndexDirectory = nameOfIndexDirectory;

        parser = new AutoDetectParser(config);
    }

    /**
     * Prepares configuration.
     * <p/>
     *
     * @return true if configuration did exist and we may continue
     */
    public static boolean prepare(File tikaConfigFile) {
        // Check if config file exists and if not initiate it from template
        if (!tikaConfigFile.exists()) {
            String info = "No existing configuration - first time running?\n";
            info += "Creating an initial TIKA configuration...";
            System.out.println(info);

            try (InputStream is = Scanner.class.getResourceAsStream("tika-config-template.xml")) {
                if (null != is) {
                    tikaConfigFile.createNewFile();

                    try (FileOutputStream os = new FileOutputStream(tikaConfigFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    } catch (FileNotFoundException ignore) {
                    }
                }
            } catch (IOException ignore) {
            }

            info = "Please refer to and adjust the configuration file and then run again:\n" + tikaConfigFile;
            System.out.println(info);

            return false; // Configuration did not exist
        }
        return true; // Configuration existed
    }

    private Reader getReader(InputStream inputStream, Metadata metadata) throws IOException, TikaException, SAXException {
        ParseContext context = new ParseContext();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ContentHandler handler = new BodyContentHandler(outputStream);

            parser.parse(inputStream, handler, metadata, context);

            String bodyContent = outputStream.toString(DEFAULT_SOURCE_CHARACTER_ENCODING);
            return new BinaryFilterReader(new StringReader(bodyContent));
        }
    }

    private boolean scanFile(
            File file,
            Set<String> observedContentTypes,
            final ScanPerFileRunnable runnable
    ) throws IOException {

        final Path path = file.toPath();
        try (InputStream is = Files.newInputStream(path)) {

            if (log.isDebugEnabled()) {
                String info = "Indexing " + file.getCanonicalPath();
                log.debug(info);
            }

            Metadata metadata = new Metadata();
            final Reader reader = getReader(is, metadata);

            String contentType = metadata.get("Content-Type");
            observedContentTypes.add(contentType);

            Matcher ctm = contentTypePattern.matcher(contentType.toLowerCase());
            if (ctm.matches()) {
                String major = ctm.group(1);
                String minor = ctm.group(2);
                String _contentType = major + "/" + minor;

                String charset = ctm.group(6);
                if (null == charset || charset.isEmpty()) {
                    charset = DEFAULT_SOURCE_CHARACTER_ENCODING;
                }

                return runnable.run(path, _contentType, major, minor, charset, reader);
            }
        } catch (TikaException tikae) {
            String info = "TIKA could not process file \"" + file.getAbsolutePath() + "\": " + tikae.getMessage();
            log.info(info);
        } catch (SAXException saxe) {
            String info = "Parse error: " + saxe.getMessage();
            log.warn(info);
        } catch (Throwable t) {
            String info = "Failed to index file \"" + file.getAbsolutePath() + "\":  " + t.getMessage();
            log.info(info);
        }
        return false;
    }

    public long scanDirectory(
            File directoryToIndex,
            Set<String> observedContentTypes,
            final ScanPerDirectoryRunnable perDirectoryRunnable,
            final ScanPerFileRunnable perFileRunnable
    ) throws IOException {

        long fileCount = 0L;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryToIndex.toPath())) {
            for (Path entryPath : stream) {
                File entry = entryPath.toFile();
                if (entry.isFile() && entry.canRead() && entry.length() > 0
                ) {
                    try {
                        if (scanFile(entry, observedContentTypes, perFileRunnable)) {
                            fileCount++;
                        }
                    } catch (IOException ioe) {
                        String info = "Failed to rollback index: " + ioe.getMessage();
                        log.warn(info);
                    }
                } else if (entry.isDirectory()) {
                    if (nameOfIndexDirectory.equals(entry.getName())) {
                        log.info("Ignoring index database: " + nameOfIndexDirectory);
                    } else {
                        fileCount += scanDirectory(entry, observedContentTypes, perDirectoryRunnable, perFileRunnable);
                        perDirectoryRunnable.run();
                    }
                }
            }
        } catch (java.nio.file.FileSystemException fse) {
            String info = "Failed to scan directory: " + fse.getMessage();
            log.warn(info, fse);
        }

        return fileCount;
    }

    public interface ScanPerDirectoryRunnable {
        void run() throws IOException;
    }

    public interface ScanPerFileRunnable {
        boolean run(Path path, String contentType, String major, String minor, String charset, Reader reader) throws IOException;
    }
}
