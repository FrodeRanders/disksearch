package demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

import java.io.IOException;

class Optimizer {

    private final Directory indexDirectory;
    private final Analyzer analyzer;

    Optimizer(Directory indexDirectory, Analyzer analyzer)  {
        this.indexDirectory = indexDirectory;
        this.analyzer = analyzer;
    }

    void optimize() throws IOException {
        IndexWriterConfig indexerConfig = new IndexWriterConfig(analyzer);
        indexerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        long fileCounter = 0L;
        try (IndexWriter indexWriter = new IndexWriter(indexDirectory, indexerConfig)) {
            // indexWriter.optimize();
        }
    }
}
