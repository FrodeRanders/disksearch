package demo;

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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


class Indexer {
    private static Logger log = LogManager.getLogger(Indexer.class);

    private final Directory indexDirectory;
    private final Analyzer analyzer;
    private final Scanner scanner;

    Indexer(Directory indexDirectory, Analyzer analyzer, Scanner scanner) {
        this.indexDirectory = indexDirectory;
        this.analyzer = analyzer;
        this.scanner = scanner;
    }

    void indexDirectory(File directoryToIndex, PrintWriter out) {
        Set<String> observedContentTypes = new HashSet<>();
        Set<String> processedContentTypes = new HashSet<>();
        Set<String> ignoredContentTypes = new HashSet<>();

        try {
            IndexWriterConfig indexerConfig = new IndexWriterConfig(analyzer);
            indexerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            final Long[] fileCount = {0L};
            try (IndexWriter indexWriter = new IndexWriter(indexDirectory, indexerConfig)) {
                fileCount[0] += scanner.scanDirectory(
                        directoryToIndex, observedContentTypes,
                        /* per directory */ indexWriter::flush,
                        /* per file */ (path, contentType, major, minor, charset, reader) -> {
                            String filename = path.getFileName().toString();
                            String absolutePath = path.toAbsolutePath().toString();

                            //
                            Document doc = new Document();

                            // Things to index: filename, path, content-type, content
                            doc.add(new StringField("filename", filename, Field.Store.YES));
                            doc.add(new StringField("path", absolutePath, Field.Store.YES));
                            doc.add(new StringField("content-type", contentType, Field.Store.YES));

                            switch (major) {
                                case "audio":
                                case "video":
                                case "img": // incorrect spelling!
                                case "image":
                                case "font":
                                    ignoredContentTypes.add(contentType);
                                    break;

                                default:
                                    doc.add(new TextField("content", reader));
                                    processedContentTypes.add(contentType);

                                    break;
                            }

                            out.format("%8d %s\n", ++fileCount[0], filename);
                            out.flush();

                            indexWriter.addDocument(doc);
                            return true;
                        });

                out.println();
                out.println("Committing to database...");
                indexWriter.commit();

                //
                out.println();
                out.println();
                out.println("------------------------------------------------------------------------------------");
                out.println("  Processed " + fileCount[0] + " file(s)");
                out.println("------------------------------------------------------------------------------------");
                out.println();

                //
                if (!observedContentTypes.isEmpty()) {
                    List<String> _observedContentTypes = new LinkedList<>(observedContentTypes);
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
                if (!processedContentTypes.isEmpty()) {
                    List<String> _processedContentTypes = new LinkedList<>(processedContentTypes);
                    Collections.sort(_processedContentTypes);

                    out.println();
                    out.println("------------------------------------------------------------------------------------");
                    out.println("                             Indexed content types");
                    out.println("------------------------------------------------------------------------------------");
                    for (String contentType : _processedContentTypes) {
                        out.println("   " + contentType);
                    }
                    out.println();
                }

                //
                if (!ignoredContentTypes.isEmpty()) {
                    List<String> _ignoredContentTypes = new LinkedList<>(ignoredContentTypes);
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
}
