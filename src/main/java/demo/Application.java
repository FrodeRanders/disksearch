package demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Application {
    private static Logger log = LogManager.getLogger(Application.class);

    public static void main(String[] args) {

        PrintWriter out = new PrintWriter(System.out);

        try {
            // Setup Lucene index location
            Path indexPath = Paths.get(System.getProperty("user.dir"), "=index=");
            Directory indexDirectory = new NIOFSDirectory(indexPath);

            // Assume swedish language resources (mainly)
            Analyzer analyzer = new SwedishAnalyzer();

            if (args.length > 0) {
                switch (args[0]) {
                    case "convert":
                        if (args.length > 1) {
                            File tikaConfigFile = new File("tika-config.xml");
                            if (Scanner.prepare(tikaConfigFile)) {
                                Scanner scanner = new Scanner(tikaConfigFile);
                                Converter converter = new Converter(scanner);
                                File sourceDirectory = new File(args[1]);
                                converter.convertDirectory(sourceDirectory, out);
                            }
                        }
                        else {
                            String info = "You need to provide path to directory";
                            out.println(info);
                        }
                        break;

                    case "index":
                        if (args.length > 1) {
                            File tikaConfigFile = new File("tika-config.xml");
                            if (Scanner.prepare(tikaConfigFile)) {
                                Scanner scanner = new Scanner(tikaConfigFile);
                                Indexer indexer = new Indexer(indexDirectory, analyzer, scanner);
                                File sourceDirectory = new File(args[1]);
                                indexer.indexDirectory(sourceDirectory, out);
                            }
                        }
                        else {
                            String info = "You need to provide path to directory";
                            out.println(info);
                        }
                        break;

                    case "search":
                        String field = "content"; // default
                        if (args.length > 1) {
                            field = args[1];
                        }
                        Searcher searcher = new Searcher(indexDirectory, analyzer);
                        searcher.search(field, out);
                        break;

                    default:
                        String info = "Unknown function: " + args[0];
                        out.println(info);
                }
            }
            else {
                String info = "usage: index <directory> | convert <directory> | search [filename|path|content-type|content]";
                out.println(info);
            }
        } catch (Exception e) {
            String info = "Failed: " + e.getMessage();
            log.warn(info, e);
        }

        out.flush();
    }
}
