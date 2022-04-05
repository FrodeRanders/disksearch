package demo;

import demo.filters.BinaryFilterReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class Indexer {
    private static Logger log = LogManager.getLogger(Indexer.class);

    private static final String DEFAULT_SOURCE_CHARACTER_ENCODING = "ISO-8859-1";

    private static final String CONTENT_TYPE_RE = "((?:[a-z][a-z0-9_]*))\\/((?:[a-z][a-z0-9_]*))((;.*?(charset)=((?:[a-z][a-z0-9_\\-]+)))*)";
    private static final Pattern contentTypePattern = Pattern.compile(CONTENT_TYPE_RE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final Directory indexDirectory;
    private final Analyzer analyzer;
    private final Parser parser;

    Indexer(Directory indexDirectory, Analyzer analyzer, File tikaConfigFile) throws TikaException, SAXException, IOException {
        this.indexDirectory = indexDirectory;
        this.analyzer = analyzer;

        TikaConfig config = new TikaConfig(tikaConfigFile);
        // Detector detector = config.getDetector();

        parser = new AutoDetectParser(config);
    }

    /**
     * Prepares configuration.
     * <p/>
     * @return true if configuration did exist and we may continue
     */
    public static boolean prepare(File tikaConfigFile) {
        // Check if config file exists and if not initiate it from template
        if (!tikaConfigFile.exists()) {
            String info = "No existing configuration - first time running?\n";
            info += "Creating an initial TIKA configuration...";
            System.out.println(info);

            try (InputStream is = Indexer.class.getResourceAsStream("tika-config-template.xml")) {
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
            }
            catch (IOException ignore) {
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

    private long indexFile(
          File file,
          IndexWriter indexWriter,
          Set<String> observedContentTypes,
          Set<String> indexedContentTypes,
          Set<String> ignoredContentTypes,
          long fileCount,
          PrintWriter out
    ) throws IOException {

        try (InputStream is = new FileInputStream(file)) {

            if (log.isDebugEnabled()) {
                String info = "Indexing " + file.getCanonicalPath();
                log.debug(info);
            }

            Metadata metadata = new Metadata();
            Reader reader = getReader(is, metadata);

            String contentType = metadata.get("Content-Type");
            observedContentTypes.add(contentType);

            Matcher ctm = contentTypePattern.matcher(contentType.toLowerCase());
            if (ctm.matches()) {
                String major = ctm.group(1);
                String minor = ctm.group(2);
                String charset = ctm.group(6);

                if (null == charset || charset.length() == 0) {
                    charset = DEFAULT_SOURCE_CHARACTER_ENCODING;
                }

                String _contentType = major + "/" + minor;

                Document doc = new Document();


                // Things to index: filename, path, content-type, content
                doc.add(new StringField("filename", file.getName(), Field.Store.YES));
                doc.add(new StringField("path", file.getCanonicalPath(), Field.Store.YES));
                doc.add(new StringField("content-type", _contentType, Field.Store.YES));

                switch (major) {
                    case "audio":
                    case "video":
                    case "img": // incorrect spelling!
                    case "image":
                    case "font":
                        ignoredContentTypes.add(_contentType);
                        break;

                    default:
                        doc.add(new TextField("content", reader));
                        indexedContentTypes.add(_contentType);

                        break;
                }

                out.format("%8d %s\n", fileCount, file.getName());
                out.flush();

                indexWriter.addDocument(doc);
                return 1;
            }
        }
        catch (TikaException tikae) {
            String info = "TIKA could not process file \"" + file.getAbsolutePath() + "\": " + tikae.getMessage();
            log.info(info);
        }
        catch (SAXException saxe) {
            String info = "Parse error: " + saxe.getMessage();
            log.warn(info);
        }
        catch (Throwable t) {
            String info = "Failed to index file \"" + file.getAbsolutePath() + "\":  " + t.getMessage();
            log.info(info);
        }
        return 0;
    }

    void indexDirectory(File directoryToIndex, PrintWriter out) {
        Set<String> observedContentTypes = new HashSet<>();
        Set<String> indexedContentTypes = new HashSet<>();
        Set<String> ignoredContentTypes = new HashSet<>();

        try {
            IndexWriterConfig indexerConfig = new IndexWriterConfig(analyzer);
            indexerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            long fileCount = 0L;
            try (IndexWriter indexWriter = new IndexWriter(indexDirectory, indexerConfig)) {
                fileCount += processDirectory(
                        indexWriter, observedContentTypes, indexedContentTypes, ignoredContentTypes, directoryToIndex,
                        fileCount, out
                );

                indexWriter.flush();
                indexWriter.commit();

                //
                out.println();
                out.println();
                out.println("------------------------------------------------------------------------------------");
                out.println("  Processed " + fileCount + " file(s)");
                out.println("------------------------------------------------------------------------------------");
                out.println();

                //
                if (observedContentTypes.size() > 0) {
                    List<String> _observedContentTypes = new LinkedList<>();
                    _observedContentTypes.addAll(observedContentTypes);
                    Collections.sort(_observedContentTypes);

                    out.println();
                    out.println("------------------------------------------------------------------------------------");
                    out.println("                          All observed content types");
                    out.println("------------------------------------------------------------------------------------");
                    for (String contentType : _observedContentTypes) {
                        out.println("   " + contentType);
                    }
                    out.println();
                }

                //
                if (indexedContentTypes.size() > 0) {
                    List<String> _indexedContentTypes = new LinkedList<>();
                    _indexedContentTypes.addAll(indexedContentTypes);
                    Collections.sort(_indexedContentTypes);

                    out.println();
                    out.println("------------------------------------------------------------------------------------");
                    out.println("                             Indexed content types");
                    out.println("------------------------------------------------------------------------------------");
                    for (String contentType : _indexedContentTypes) {
                        out.println("   " + contentType);
                    }
                    out.println();
                }

                //
                if (ignoredContentTypes.size() > 0) {
                    List<String> _ignoredContentTypes = new LinkedList<>();
                    _ignoredContentTypes.addAll(ignoredContentTypes);
                    Collections.sort(_ignoredContentTypes);

                    out.println();
                    out.println("------------------------------------------------------------------------------------");
                    out.println("                             Ignored content types");
                    out.println("------------------------------------------------------------------------------------");
                    for (String contentType : _ignoredContentTypes) {
                        out.println("   " + contentType);
                    }
                    out.println();
                }
            }
        } catch (IOException e) {
            String info = "Failed to close index: " + e.getMessage();
            log.warn(info, e);
        }
    }


    private long processDirectory(
            IndexWriter indexWriter,
            Set<String> observedContentTypes,
            Set<String> indexedContentTypes,
            Set<String> ignoredContentTypes,
            File directoryToIndex,
            long totalCount,
            PrintWriter out
    ) throws IOException {

        long fileCount = 0L;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryToIndex.toPath())) {
            for (Path entryPath : stream) {
                File entry = entryPath.toFile();
                if (entry.isFile() && entry.canRead() && entry.length() > 0
                ) {
                    try {
                        fileCount += indexFile(
                                entry, indexWriter, observedContentTypes, indexedContentTypes, ignoredContentTypes,
                                fileCount + totalCount, out);

                    } catch (IOException ioe) {
                        String info = "Failed to rollback index: " + ioe.getMessage();
                        log.warn(info);
                    }
                }
                else if (entry.isDirectory()) {
                    fileCount += processDirectory(
                            indexWriter, observedContentTypes, indexedContentTypes, ignoredContentTypes, entry,
                            fileCount + totalCount, out
                    );
                }
            }
        }

        return fileCount;
    }
}
