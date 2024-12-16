package demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


public class Converter {
    private static Logger log = LogManager.getLogger(Converter.class);

    private final Scanner scanner;

    Converter(Scanner scanner) {
        this.scanner = scanner;
    }

    void convertDirectory(File directoryToConvert, PrintWriter out) {
        Set<String> observedContentTypes = new HashSet<>();
        Set<String> processedContentTypes = new HashSet<>();
        Set<String> ignoredContentTypes = new HashSet<>();

        try {
            final Long[] fileCount = {0L};
            fileCount[0] += scanner.scanDirectory(
                    directoryToConvert, observedContentTypes,
                    /* per directory */ () -> { /* ignore */ },
                    /* per file */ (path, contentType, major, minor, charset, reader) -> {
                        String filename = path.getFileName().toString();

                        switch (major) {
                            case "audio":
                            case "video":
                            case "img": // incorrect spelling!
                            case "image":
                            case "font":
                                ignoredContentTypes.add(contentType);
                                break;

                            default:
                                File outputFile = new File(filename + ".txt");
                                FileIO.writeToFile(reader, outputFile);
                                processedContentTypes.add(contentType);

                                break;
                        }

                        out.format("%8d %s\n", ++fileCount[0], filename);
                        out.flush();

                        return true;
                    });


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
                out.println("                             Converted content types");
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
        } catch (IOException ioe) {
            out.println("Failed to convert: " + ioe.getMessage());
        }
    }
}

