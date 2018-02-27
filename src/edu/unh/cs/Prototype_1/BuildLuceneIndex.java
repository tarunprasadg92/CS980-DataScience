package edu.unh.cs.Prototype_1;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.CborFileTypeException;
import edu.unh.cs.treccar_v2.read_data.CborRuntimeException;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

import java.util.ArrayList;
import java.util.List;

/*
 * @author Tarun Prasad
 * Based on examples from TREMA-UNH
 */

public class BuildLuceneIndex {
	
	public static void main(String[] args) throws IOException {
		System.setProperty("file.encoding", "UTF-8");
		
		// Invalid number of arguments
		if (args.length < 2) {
			System.out.println("Command line arguments : <paragraphCBOR> <LuceneINDEX>");
			System.exit(-1);
		}
		
		String indexPath = args[1];
		final String paragraphsFile = args[0];
		final FileInputStream fis = new FileInputStream(new File(paragraphsFile));
		
		System.out.println("Creating index in " + indexPath);
		final IndexWriter iw = setUpIndexWriter(indexPath, "paragraph.lucene");
		
		final Iterator<Data.Paragraph> para_iterator = DeserializeData.iterParagraphs(fis);
		for (int i = 0; para_iterator.hasNext(); i++) {
			final Document doc = paragraphToLuceneDoc(para_iterator.next());
			iw.addDocument(doc);
			
			if (i % 10000 == 0) {
				System.out.println(i + " paragraphs indexed...");
				iw.commit();
			}
		}
		
		System.out.println("Indexing complete...");
		
		iw.commit();
		iw.close();		
	}
	
	private static Document paragraphToLuceneDoc(Data.Paragraph p) {
		final Document doc = new Document();
		final String content = p.getTextOnly();
		doc.add(new TextField("text", content, Field.Store.YES));
		doc.add(new StringField("paragraphid", p.getParaId(), Field.Store.YES));
		return doc;
	}
	
	private static IndexWriter setUpIndexWriter(String indexPath, String typeIndex) throws IOException {
		Path path = FileSystems.getDefault().getPath(indexPath, typeIndex);
		Directory indexDir = FSDirectory.open(path);
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		return new IndexWriter(indexDir, config);
	}
}