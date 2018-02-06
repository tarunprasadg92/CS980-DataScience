package edu.unh.cs;

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
import org.jetbrains.annotations.NotNull;

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

/*
 * @author Tarun Prasad
 * Based on the examples from TREMA-UNH
 */
 
 public class Search {
	 
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
	 }
 	
 	public static void main(String[] args) throws IOException {
 		System.setProperty("file.encoding", "UTF-8");
 		
 		// Invalid number of arguments
 		if (args.length < 2) {
 			System.out.println("Command line arguments : <OutlinesCBOR> <LuceneIndex>");
 			System.exit(-1);
 		}
 			
		// Query pages 
 		String indexPath = args[1];
 		IndexSearcher searcher = setUpIndexSearcher(indexPath, "paragraph.lucene");
 		searcher.setSimilarity(new BM25Similarity());
 		final MyQueryBuilder queryBuilder = new MyQueryBuilder(new StandardAnalyzer()); 		
 		
 		final String pagesFile = args[0];
 		final FileInputStream fis = new FileInputStream(new File(pagesFile));
 		ArrayList<String> runStringPage = new ArrayList<String>();
 		ArrayList<String> duplicateCheck = new ArrayList<String>();
 		
 		for (Data.Page page : DeserializeData.iterableAnnotations(fis)) {
 			final String queryId = page.getPageId(); 			
 			String queryString = buildQueryString(page, Collections.<Data.Section>emptyList());
 			
 			TopDocs tops = searcher.search(queryBuilder.toQuery(queryString), 100);
 			ScoreDoc[] scoreDoc = tops.scoreDocs;
 			 			
 			for (int i = 0; i < scoreDoc.length; i++) {
 				ScoreDoc score = scoreDoc[i];
 				final Document doc = searcher.doc(score.doc);
 				final String paragraphID = doc.getField("paragraphid").stringValue();
 				final float searchScore = score.score;
 				final int searchRank = i+1;
 				
 				String runString = queryId+" Q0 "+paragraphID+" "+searchRank+" "+searchScore+" Lucene-BM25";
 				if (!duplicateCheck.contains(paragraphID)) {
 					duplicateCheck.add(paragraphID);
 	 				runStringPage.add(runString);
 				} 				
 			}
 			duplicateCheck.clear();
 			System.out.print("-");
 		}
 		FileWriter fw = new FileWriter("pages.run", true);
		for(String runString:runStringPage)
			fw.write(runString+"\n");
		
		fw.close();
 		
		System.out.println("pages done...");
 		
 		// Query Sections
 		final String pageFile = args[0];
 		final FileInputStream fis2 = new FileInputStream(new File(pageFile));
 		ArrayList<String> runStringSection = new ArrayList<String>();
 		duplicateCheck.clear();
 		
 		for (Data.Page page : DeserializeData.iterableAnnotations(fis2)) {
 			for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
 				final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);
 				String queryString = buildQueryString(page, sectionPath);
 				
 				TopDocs tops = searcher.search(queryBuilder.toQuery(queryString), 100);
 				ScoreDoc[] scoreDoc = tops.scoreDocs;
 		 				
 				for (int i = 0; i < scoreDoc.length; i++) {
 					ScoreDoc score = scoreDoc[i];
 					final Document doc = searcher.doc(score.doc);
 					final String paragraphID = doc.getField("paragraphid").stringValue();
 					final float searchScore = score.score;
 					final int searchRank = i+1;
 					
 					String runString = queryId+" Q0 "+paragraphID+" "+searchRank+" "+searchScore+" Lucene-BM25";
 					if (!duplicateCheck.contains(paragraphID)) {
 						duplicateCheck.add(paragraphID);
 						runStringSection.add(runString); 						
 					} 	 				
 				}			
 			}
 			System.out.print("-");
 		}
 		FileWriter fw2 = new FileWriter("sections.run", true);
		for(String runString:runStringSection)
			fw2.write(runString+"\n");
		
		fw2.close();
 		System.out.println("sections done...");
 	}
 	
 	private static IndexSearcher setUpIndexSearcher(String indexPath, String type) throws IOException {
 		Path path = FileSystems.getDefault().getPath(indexPath, type);
 		Directory indexDirectory = FSDirectory.open(path);
 		IndexReader reader = DirectoryReader.open(indexDirectory);
 		return new IndexSearcher(reader);
 	}
 	
 	private static String buildQueryString(Data.Page page, List<Data.Section> sectionPath) {
 		StringBuilder queryString = new StringBuilder();
 		queryString.append(page.getPageName());
 		for(Data.Section section: sectionPath)
 			queryString.append(" ").append(section.getHeading());
 		return queryString.toString();
 	}
 }