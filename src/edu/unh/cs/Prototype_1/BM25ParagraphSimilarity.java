package edu.unh.cs.Prototype_1;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.CborFileTypeException;
import edu.unh.cs.treccar_v2.read_data.CborRuntimeException;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator; 
import java.util.List;
import java.io.FileWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/*
 * @author Tarun Prasad
 * Query builder and search functionality modified from examples of TREMA-UNH
 */

public class BM25ParagraphSimilarity {
	
	static class MyQueryBuilder {
		 private final StandardAnalyzer analyzer;
		 private List<String> tokens;
		 
		 public MyQueryBuilder(StandardAnalyzer standardAnalyzer) {
			 analyzer = standardAnalyzer;
			 tokens = new ArrayList<>(128);
		 }
		 
		 public BooleanQuery toQuery(String queryString) throws IOException {
			 TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(queryString));
			 tokenStream.reset();
			 tokens.clear();
			 
			 while(tokenStream.incrementToken()) {
				 final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
				 tokens.add(token);
			 }
			 
			 tokenStream.end();
			 tokenStream.close();
			 
			 BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			 for (String token:tokens)
				 booleanQuery.add(new TermQuery(new Term("text", token)), BooleanClause.Occur.SHOULD);
			 
			 return booleanQuery.build();
		 }
		 
		 public BooleanQuery toIDQuery(String queryString) throws IOException {
			 TokenStream tokenStream = analyzer.tokenStream("paragraphid", new StringReader(queryString));
			 tokenStream.reset();
			 tokens.clear();
			 
			 while(tokenStream.incrementToken()) {
				 final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
				 tokens.add(token);
			 }
			 
			 tokenStream.end();
			 tokenStream.close();
			 
			 BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			 for (String token:tokens)
				 booleanQuery.add(new TermQuery(new Term("paragraphid", token)), BooleanClause.Occur.SHOULD);
			 
			 return booleanQuery.build();
		 }
	 }
	
	static Map<String, List<String>> runFileData;
	
	public static void main(String[] args) throws IOException {
		
		System.setProperty("file.encoding","UTF-8");
		
		// Check command line argument
		if (args.length < 2) {
			System.out.println("Command line arguments : <run-file> <LuceneIndex>");
			System.exit(-1);
		}
		
		String indexPath = args[1];
		IndexSearcher searcher = setUpIndexSearcher(indexPath, "paragraph.lucene");
		searcher.setSimilarity(new BM25Similarity());
		final MyQueryBuilder queryBuilder = new MyQueryBuilder(new StandardAnalyzer());
		
		String runFile = args[0];
		
		// Extract pageID and paragraph IDs from run file
		runFileData = new HashMap<String, List<String>>();
		readDataFromFile(runFile);
		
		ArrayList<String> runStringParagraph = new ArrayList<String>();		
		Iterator<Entry<String, List<String>>> it = runFileData.entrySet().iterator();
		
		int counter = 0;
		
		// Iterate over page IDs
		while (it.hasNext()) {
			
			Map.Entry<String, List<String>> data = (Map.Entry<String, List<String>>)it.next();
			List<String> p = data.getValue();

			Iterator<String> it2 = p.iterator();
			
			while (it2.hasNext()) {
				
				String queryString = it2.next();
				TopDocs tops = searcher.search(queryBuilder.toIDQuery(queryString), 1);
				ScoreDoc[] scoreDoc = tops.scoreDocs;
				ScoreDoc score = scoreDoc[0];
				
				final Document doc = searcher.doc(score.doc);
 				final String paragraphText = doc.getField("text").stringValue();
 				
 				TopDocs topParas = searcher.search(queryBuilder.toQuery(paragraphText), 100);
 	 			ScoreDoc[] scoreParas = topParas.scoreDocs;
 	 			
 	 			for (int i = 0; i < scoreParas.length; i++) {
 	 				ScoreDoc paraScore = scoreParas[i];
 	 				final Document paraDoc = searcher.doc(paraScore.doc);
 	 				final String paragraphID = paraDoc.getField("paragraphid").stringValue();
 	 				final float searchScore = score.score;
 	 				final int searchRank = i+1;
 	 				
 	 				String runString = queryString+" Q0 "+paragraphID+" "+searchRank+" "+searchScore+" Lucene-BM25";
 	 				runStringParagraph.add(runString);
 	 				// System.out.println(runString);
 	 			}
			}	
			System.out.print(".");	
			counter++;
			if ((counter % 20) == 0) {
				System.out.println();
			}
		}
		
		FileWriter fw = new FileWriter("paragraph-similarity-bm25.run", true);
		for(String runString:runStringParagraph)
			fw.write(runString+"\n");
		
		fw.close();
		System.out.println("Process complete...");
	}
	
	// Helper function for verification
	private static void printHashMap(Map<String, List<String>> runFileData) {
		Iterator<Entry<String, List<String>>> it = runFileData.entrySet().iterator();
		
		while (it.hasNext()) {
			Map.Entry<String, List<String>> data = (Map.Entry<String, List<String>>)it.next();
			System.out.println(data.getKey() + " : " + data.getValue());
		}
		System.out.println(runFileData.size());
	}
	
	// Helper function for verification
	private static void printPageIDs(Set<String> pages) {
		Iterator<String> it = pages.iterator();
		
		while (it.hasNext()) {
			System.out.println(it.next());
		}
		System.out.println("Number of pages : " + pages.size());
	}
	
	// Read data from run file into program
	private static void readDataFromFile(String fileName) {
		String line = null;
		
		try {
			FileReader fr = new FileReader(fileName);
			BufferedReader br = new BufferedReader(fr);
			
			while ((line = br.readLine()) != null) {
				String[] splits = line.split(" ");
				
				List<String> paras = runFileData.get(splits[0]);
				if (paras == null) {
					paras = new ArrayList<String>();
				}
				paras.add(splits[2]);
				runFileData.put(splits[0], paras);				
			}
			
			// printPageIDs(pageIDs);
			// printHashMap(runFileData);
			br.close();
		}
		
		catch (FileNotFoundException ex) {
			System.out.println("Unable to open file " + fileName);
		}
		
		catch (IOException ex) {
			System.out.println("Error opening file " + fileName);
		}
		
	}
	
	// Steps to read paragraphs from index directory
	private static IndexSearcher setUpIndexSearcher(String indexPath, String type) throws IOException {
		Path path = FileSystems.getDefault().getPath(indexPath, type);
		Directory indexDirectory = FSDirectory.open(path);
		IndexReader reader = DirectoryReader.open(indexDirectory);
		return new IndexSearcher(reader);
	}	
}