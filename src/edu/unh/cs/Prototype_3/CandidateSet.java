package edu.unh.cs.Prototype_3;

import edu.unh.cs.treccar_v2.Data;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.io.StringReader;
import org.json.simple.parser.*;
import java.io.FileWriter;

/*
 * @author Tarun Prasad
 * 
 * Candidate Set Generation using DBPedia
 */

public class CandidateSet 
{
	String outlines_cbor;
	String index_path;
	String curl_path;
	
	Map<Data.Page, List<String>> initial_query;
	Map<Data.Page, List<String>> intermediate_query;
	Map<Data.Page, List<String>> expanded_query;
	
	/*
	 * Sub-class to build query to search the index
	 */
	static class MyQueryBuilder 
	{
		private final StandardAnalyzer analyzer;
		private List<String> tokens;
		 
		public MyQueryBuilder(StandardAnalyzer standardAnalyzer) 
		{
			analyzer = standardAnalyzer;
			tokens = new ArrayList<>(128);
		}
		
		/*
		 * Function to return paragraph text with query text input
		 */
		public BooleanQuery toQuery(String queryString) throws IOException 
		{
			TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(queryString));
			tokenStream.reset();
			tokens.clear();
			 
			while(tokenStream.incrementToken()) 
			{
				final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
				tokens.add(token);
			}
			 
			tokenStream.end();
			tokenStream.close();
			 
			BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			
			for (String token:tokens)
			{
				booleanQuery.add(new TermQuery(new Term("text", token)), BooleanClause.Occur.SHOULD);
			}
			 
			return booleanQuery.build();
		}
		
		/*
		 * Function to return paragraph text with paragraphID input
		 */
		public BooleanQuery toIDQuery(String queryString) throws IOException 
		{
			TokenStream tokenStream = analyzer.tokenStream("paragraphid", new StringReader(queryString));
			tokenStream.reset();
			tokens.clear();
			 
			while(tokenStream.incrementToken()) 
			{
				final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
				tokens.add(token);
			}
			 
			tokenStream.end();
			tokenStream.close();
			 
			BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			
			for (String token:tokens)
			{
				booleanQuery.add(new TermQuery(new Term("paragraphid", token)), BooleanClause.Occur.SHOULD);
			}
			 
			return booleanQuery.build();
		}
	}
	
	/*
	 * Function to set location of lucene index for the index searcher
	 */
	private static IndexSearcher setUpIndexSearcher(String indexPath, String type) throws IOException 
	{
		Path path = FileSystems.getDefault().getPath(indexPath, type);
		Directory indexDirectory = FSDirectory.open(path);
		IndexReader reader = DirectoryReader.open(indexDirectory);
		return new IndexSearcher(reader);
	}
	
	/*
	 * Function to generate the initial query - page name
	 */
	public void generateInitialQuery() throws IOException
	{
		System.out.println("Generating initial query...");
		initial_query = new HashMap<Data.Page, List<String>>();
		
		IndexSearcher searcher = setUpIndexSearcher(index_path, "paragraph.lucene");
		searcher.setSimilarity(new BM25Similarity());
		final MyQueryBuilder query_builder = new MyQueryBuilder(new StandardAnalyzer());
		
		try
		{
			FileInputStream fis = new FileInputStream(new File(outlines_cbor));			
			
			for (Data.Page p : DeserializeData.iterableAnnotations(fis))
			{
				// System.out.println(p.getPageId() + " : " + p.getPageName());
				List<String> paragraph_ids = new ArrayList<String>();
				String page_query_string = p.getPageName(); //buildQueryString(p, Collections.<Data.Section>emptyList());
				
				TopDocs tops = searcher.search(query_builder.toQuery(page_query_string), 5);
				ScoreDoc[] score_doc = tops.scoreDocs;
				
				for (int i = 0; i < score_doc.length; i++)
				{
					ScoreDoc score = score_doc[i];
					final Document doc = searcher.doc(score.doc);
					final String paragraph_id = doc.getField("paragraphid").stringValue();
					paragraph_ids.add(paragraph_id);
				}
				
				initial_query.put(p, paragraph_ids);
				System.out.print(".");
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		// System.out.println(pages.size());
	}
	
	/*
	 * Helper function to verify the initial query
	 */
	public void printInitialQuery()
	{
		System.out.println();
		
		for (Data.Page page_id : initial_query.keySet())
		{
			List<String> para_ids = initial_query.get(page_id);
			System.out.print(page_id.getPageName() + " : ");
			
			for (String para : para_ids)
			{
				System.out.print(para + ", ");
			}
			
			System.out.println();
		}
	}
	
	/*
	 * Function to generate the intermediate query - page name + top 5 relevant paragraphs
	 */
	public void generateIntermediateQuery() throws IOException
	{
		System.out.println("\nGenerating intermediate query...");
		intermediate_query = new HashMap<Data.Page, List<String>>();
		IndexSearcher searcher = setUpIndexSearcher(index_path, "paragraph.lucene");
		searcher.setSimilarity(new BM25Similarity());
		final MyQueryBuilder query_builder = new MyQueryBuilder(new StandardAnalyzer());
		
		for (Data.Page page_id : initial_query.keySet())
		{
			List<String> para_ids = initial_query.get(page_id);
			List<String> paragraph_ids = new ArrayList<String>();
			
			for (String para : para_ids)
			{
				String query_string = para;
				TopDocs tops = searcher.search(query_builder.toIDQuery(query_string), 1);
				ScoreDoc[] score_doc = tops.scoreDocs;
				ScoreDoc score = score_doc[0];
				
				final Document doc = searcher.doc(score.doc);
				final String paragraph_text = doc.getField("text").stringValue();
				paragraph_ids.add(paragraph_text);
			}
			
			intermediate_query.put(page_id, paragraph_ids);
			System.out.print(".");
		}
	}
	
	/*
	 * Helper function to verify the intermediate query
	 */
	public void printIntermediateQuery()
	{
		System.out.println();
		
		for (Data.Page page_id : intermediate_query.keySet())
		{
			List<String> para_ids = intermediate_query.get(page_id);
			System.out.print(page_id.getPageName() + " : ");
			
			for (String para : para_ids)
			{
				System.out.print(para + ", ");
			}
			
			System.out.println();
		}
	}
	
	/*
	 * Function to generate the expanded query - page name + entities from top 5 relevant paragraphs
	 */
	public void generateExpandedQuery() throws IOException, ParseException
	{
		System.out.println("\nGenerating expanded query...");
		expanded_query = new HashMap<Data.Page, List<String>>();
		
		for (Data.Page page_id : intermediate_query.keySet())
		{
			List<String> para_ids = intermediate_query.get(page_id);
			List<String> entities_for_page = new ArrayList<String>();
			
			for (String para : para_ids)
			{
				EntityExtraction ee = new EntityExtraction(curl_path, para);
				List<String> entities_for_paragraph = ee.retrieveEntities();
				
				for (int i = 0;i < entities_for_paragraph.size(); i++)
				{
					entities_for_page.add(entities_for_paragraph.get(i));
				}
			}
			
			expanded_query.put(page_id, entities_for_page);
			System.out.print(".");
		}
	}
	
	/*
	 * Helper function to print the expanded query
	 */
	public void printExpandedQuery()
	{
		System.out.println();
		
		for (Data.Page page_id : expanded_query.keySet())
		{
			List<String> entities = expanded_query.get(page_id);
			System.out.print(page_id.getPageName() + " : " + entities.size() + " : ");
			
			for (String e : entities)
			{
				System.out.print(e + ", ");
			}
			
			System.out.println();
		}
		
		System.out.println("Number of pages : " + expanded_query.size());
	}
	
	/*
	 * Function to generate the candidate set using the expanded query as input
	 */
	public void searchParagraphs() throws IOException
	{
		System.out.println("\nRetrieving paragraphs from Lucene Index...");
		IndexSearcher searcher = setUpIndexSearcher(index_path, "paragraph.lucene");
		searcher.setSimilarity(new BM25Similarity());
		final MyQueryBuilder query_builder = new MyQueryBuilder(new StandardAnalyzer());
		ArrayList<String> run_string_page = new ArrayList<String>();
		ArrayList<String> duplicate_check = new ArrayList<String>();
		
		for (Data.Page page_id : expanded_query.keySet())
		{
			List<String> entities = expanded_query.get(page_id);
			Set<String> unique_entities = new HashSet<String>(entities);
			// System.out.println(page_id.getPageName() + " : " + unique_entities.size());
			String query_string = page_id.getPageName();
			
			for (String entity : unique_entities)
			{
				query_string += " " + entity;
			}
			
			TopDocs tops = searcher.search(query_builder.toQuery(query_string), 200);
			ScoreDoc[] scoreDoc = tops.scoreDocs;
			
			for (int i = 0; i < scoreDoc.length; i++)
			{
				ScoreDoc score = scoreDoc[i];
				final Document doc = searcher.doc(score.doc);
				final String paragraph_id = doc.getField("paragraphid").stringValue();
				final float search_score = score.score;
				final int search_rank = i + 1;				
				String run_string = page_id.getPageId() + " Q0 " + paragraph_id + " " + search_rank + " " + search_score + " QueryExpansionDBPedia\n";
				
				if (!duplicate_check.contains(paragraph_id))
				{
					duplicate_check.add(paragraph_id);
					run_string_page.add(run_string);
				}
			}
			
			duplicate_check.clear();
			System.out.print(".");
		}
		
		FileWriter fw = new FileWriter("QueryExpansionDBPedia.run", true);
		
		for (String run_string : run_string_page)
		{
			fw.write(run_string);
		}
		
		fw.close();
		System.out.println("\nCandidate set generation process complete...");
	}
	
	/*
	 * Constructor - handles the entire process of generating candidate set through query expansion
	 */
	public CandidateSet(String arg1, String arg2, String arg3) throws IOException
	{
		outlines_cbor = arg1;
		index_path = arg2;
		curl_path = arg3;
		// System.out.println("Outlines file : " + outlines_cbor + " Index Path : " + index_path);
		generateInitialQuery();
		// printInitialQuery();
		generateIntermediateQuery();
		// printIntermediateQuery();
		
		try 
		{
			generateExpandedQuery();
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
		
		// printExpandedQuery();
		searchParagraphs();
	}
	
	/*
	 * Main function to begin the process
	 */
	public static void main(String[] args) throws IOException 
	{
		System.setProperty("file.encoding","UTF-8");
		
		// Check command line argument
		if (args.length < 3) 
		{
			System.out.println("Command line arguments : <Outlines-CBOR> <LuceneIndex> <Path-to-CURL-script");
			System.exit(-1);
		}
		
		CandidateSet cs = new CandidateSet(args[0], args[1], args[2]);
	}
}