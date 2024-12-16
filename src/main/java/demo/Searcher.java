package demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;


class Searcher {
    private static Logger log = LogManager.getLogger(Searcher.class);

    private final Directory indexDirectory;
    private final Analyzer analyzer;

    Searcher(Directory indexDirectory, Analyzer analyzer) {
        this.indexDirectory = indexDirectory;
        this.analyzer = analyzer;
    }

    void search(String field, PrintWriter out) throws IOException {
        try (IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);

            QueryParser contentQueryParser = new QueryParser(field, analyzer);
            contentQueryParser.setAllowLeadingWildcard(true);

            Scanner scanner = new Scanner(System.in);
            boolean oneMoreTime = true, debug = false;
            do {
                out.print("? ");
                out.flush();

                String input = scanner.nextLine().trim();
                if ("debug".equalsIgnoreCase(input)) {
                    if (debug) {
                        out.println("debug off");
                        debug = false;
                    } else {
                        out.println("debug on");
                        debug = true;
                    }
                    continue;
                }

                oneMoreTime = !"exit".equalsIgnoreCase(input);

                if (oneMoreTime) {
                    Query query;
                    if ("content".equals(field)) {
                        query = contentQueryParser.parse(input);
                    } else {
                        query = new TermQuery(new Term(field, input));
                    }
                    out.println("Searching for: " + query.toString(field));

                    out.println(searcher.count(query) + " matching documents");
                    out.flush();

                    ScoreDoc[] hits = searcher.search(query, 100).scoreDocs;

                    for (ScoreDoc hit : hits) {
                        Document document = searcher.doc(hit.doc);
                        out.println("[" + hit.doc + " : " + hit.score + "] " + document.get("filename") + " (" + document.get("content-type") + ") [" + document.get("path") + "]");
                        if (debug) {
                            Explanation explanation = searcher.explain(query, hit.doc);
                            out.println(explanation);
                        }
                        out.flush();
                    }
                }
            } while (oneMoreTime);
        } catch (ParseException pe) {
            String info = "Failed to parse query: " + pe.getMessage();
            pe.printStackTrace(System.out);
            out.println(info);
        }
    }
}
