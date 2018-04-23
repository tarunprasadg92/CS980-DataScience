package edu.unh.cs.Prototype_3;

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

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.io.StringReader;
import org.json.simple.parser.*;
import java.util.LinkedHashMap;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectOutputStream;

/*
 * @author Tarun Prasad
 * 
 * Implementation of Weighted PageRank to rank paragraphs in each cluster
 */

public class ClusterRank
{
	HashMap<String, ArrayList<ArrayList<String>>> cluster_data;
	HashMap<String, ArrayList<HashMap<String, Double>>> ranked_cluster_data;
	
	String index_path;
	String curl_path;
	String cluster_file;
	String run_file;
	String method;
	
	/*
	 * Class to create paragraph ID as query and paragraph text as result
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
	
	public void checkReadData()
	{
		for (String page_id : cluster_data.keySet())
		{
			ArrayList<ArrayList<String>> clusters_in_page = cluster_data.get(page_id);
			for (ArrayList<String> cluster : clusters_in_page)
			{
				System.out.println("\nNo of paragraphs in cluster : " + cluster.size());
				for (String paragraph_id : cluster)
				{
					System.out.print(paragraph_id + ", ");
				}
			}
		}
	}
	
	public void rankClusters() throws IOException, ParseException
	{
		IndexSearcher searcher = setUpIndexSearcher(index_path, "paragraph.lucene");
		searcher.setSimilarity(new BM25Similarity());
		final MyQueryBuilder query_builder = new MyQueryBuilder(new StandardAnalyzer());
		
		// checkReadData();
		System.out.println("\nRanking the clusters using : ");
		ranked_cluster_data = new HashMap<String, ArrayList<HashMap<String, Double>>>();
		int count = 0;
		
		if (method.equals("common-words"))
		{
			System.out.print("Common-words...");
			for (String page_id : cluster_data.keySet())
			{
				ArrayList<ArrayList<String>> clusters_in_page = cluster_data.get(page_id);
				ArrayList<HashMap<String, Double>> ranked_clusters = new ArrayList<HashMap<String, Double>>();
				
				for (ArrayList<String> cluster : clusters_in_page)
				{
					LinkedHashMap<String, List<String>> words_in_paragraph = new LinkedHashMap<String, List<String>>();
					HashMap<String, Double> page_rank_result = new HashMap<String, Double>();
					
					for (String paragraph_id : cluster)
					{
						String query_string = paragraph_id;
						TopDocs tops = searcher.search(query_builder.toIDQuery(query_string), 1);
						ScoreDoc[] score_doc = tops.scoreDocs;
						ScoreDoc score = score_doc[0];
						
						final Document doc = searcher.doc(score.doc);
						final String paragraph_text = doc.getField("text").stringValue();
						
						List<String> words_for_paragraph = getParagraphTextAsList(paragraph_text);
						words_in_paragraph.put(paragraph_id, words_for_paragraph);
					}
					WeightedPageRank wpre = new WeightedPageRank(words_in_paragraph);
					page_rank_result = wpre.getPageRanks();
					ranked_clusters.add(page_rank_result);
				}
				ranked_cluster_data.put(page_id, ranked_clusters);
				if (count % 25 == 0)
					System.out.println();
				System.out.print(".");
				count++;
			}
		}
		
		else if (method.equals("common-entity"))
		{
			for (String page_id : cluster_data.keySet())
			{
				ArrayList<ArrayList<String>> clusters_in_page = cluster_data.get(page_id);
				ArrayList<HashMap<String, Double>> ranked_clusters = new ArrayList<HashMap<String, Double>>();
				
				for (ArrayList<String> cluster : clusters_in_page)
				{
					LinkedHashMap<String, List<String>> entities_in_paragraph = new LinkedHashMap<String, List<String>>();
					HashMap<String, Double> page_rank_result = new HashMap<String, Double>();
					
					for (String paragraph_id : cluster)
					{
						String query_string = paragraph_id;
						TopDocs tops = searcher.search(query_builder.toIDQuery(query_string), 1);
						ScoreDoc[] score_doc = tops.scoreDocs;
						ScoreDoc score = score_doc[0];
						
						final Document doc = searcher.doc(score.doc);
						final String paragraph_text = doc.getField("text").stringValue();
						
						EntityExtraction ee = new EntityExtraction(curl_path, paragraph_text);
						List<String> entities_for_paragraph = ee.retrieveEntities();
						entities_in_paragraph.put(paragraph_id, entities_for_paragraph);
					}
					WeightedPageRank wpre = new WeightedPageRank(entities_in_paragraph);
					page_rank_result = wpre.getPageRanks();
					ranked_clusters.add(page_rank_result);
				}
				ranked_cluster_data.put(page_id, ranked_clusters);
				if (count % 25 == 0)
					System.out.println();
				System.out.print(".");
				count++;
			}
		}		
		
		FileWriter fw;
		if (method.equals("common-words"))
		{
			fw = new FileWriter("WeightedPageRankCommonWords.run", true);
		}
		else
		{
			fw = new FileWriter("WeightedPageRankCommonEntities.run", true);
		}
		
		System.out.println("\nWriting run file...");
		count = 0;
		try
		{
			String line = null;
			FileReader fr = new FileReader(run_file);
			BufferedReader br = new BufferedReader(fr);
			while ((line = br.readLine()) != null)
			{
				String[] splits = line.split(" ");
				String pg_id = splits[0];
				String para_id = splits[2];
				String cluster_score = splits[4];
				
				double c_score = Double.parseDouble(cluster_score);
				double new_score = getScore(para_id) * c_score;
				
				String final_string = pg_id + " Q0 " + para_id + " 0 " + new_score + " WeightedPRCommonEntities\n";
				fw.write(final_string);
				
				if (count % 25 == 0)
					System.out.print(".");
				if (count % 1000 == 0)
					System.out.println();
				count++;
			}
			
			br.close();
			fw.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public List<String> getParagraphTextAsList(String para_text)
	{
		List<String> result = new ArrayList<String>();
		String[] splits = para_text.split("\\s+");
		for (int i = 0; i < splits.length; i++)
		{
			result.add(splits[i]);
		}
		return result;
	}
	
	public double getScore(String para_id)
	{
		for (String page_id : ranked_cluster_data.keySet())
		{
			ArrayList<HashMap<String, Double>> clusters_in_page = ranked_cluster_data.get(page_id);
			for (HashMap<String, Double> cluster : clusters_in_page)
			{
				for (String paragraph_id : cluster.keySet())
				{
					if (paragraph_id.equals(para_id))
						return cluster.get(paragraph_id);
				}
			}
		}
		return 0.0;
	}
	
	public ClusterRank(String arg1, String arg2, String arg3, String arg4, String arg5) throws IOException, ParseException
	{
		cluster_file = arg1;
		index_path = arg2;
		curl_path  = arg3;
		run_file = arg4;
		method = arg5;
		
		// System.out.println("Cluster file : " + cluster_file + " Index Path : " + index_path + " Curl Path : " + curl_path);
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(cluster_file)));
		try
		{
			cluster_data = (HashMap<String, ArrayList<ArrayList<String>>>)ois.readObject();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		ois.close();
		
		rankClusters();		
	}
	
	/*
	 * Function to set up the index searcher
	 */
	private static IndexSearcher setUpIndexSearcher(String index_path, String type) throws IOException 
	{
		Path path = FileSystems.getDefault().getPath(index_path, type);
		Directory index_directory = FSDirectory.open(path);
		IndexReader reader = DirectoryReader.open(index_directory);
		return new IndexSearcher(reader);
	}
	
	public static void main(String[] args) throws IOException, ParseException
	{
		System.setProperty("file.encoding", "UTF-8");
		
		// Check command line arguments
		if (args.length < 5)
		{
			System.out.println("Command line arguments : <cluster-file> <lucene-index> <path-to-curl-script> <run-file> <method>");
			System.exit(-1);
		}
		
		ClusterRank cr = new ClusterRank(args[0], args[1], args[2], args[3], args[4]);
	}
}