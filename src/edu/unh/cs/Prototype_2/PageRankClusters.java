package edu.unh.cs.Prototype_2;

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
 * Implementation of PageRank to rank paragraphs in each cluster
 */

public class PageRankClusters {
	
	static HashMap<String, ArrayList<ArrayList<String>>> data;
	static HashMap<String, ArrayList<HashMap<String, Double>>> ranked_data;
	static HashMap<String, ArrayList<HashMap<String, Double>>> ranked_data_common_words;
	
	/*
	 * Class to create paragraph ID as query and paragraph text as result
	 */
	static class MyQueryBuilder {
		private final StandardAnalyzer analyzer;
		private List<String> tokens;
		 
		public MyQueryBuilder(StandardAnalyzer standardAnalyzer) {
			analyzer = standardAnalyzer;
			tokens = new ArrayList<>(128);
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
	
	/*
	 * Helper function to verify the data read 
	 */
	public static void checkReadData() {
		
		for (String pageID : data.keySet()) {			
			ArrayList<ArrayList<String>> clusters_in_page = data.get(pageID);
			
			for (ArrayList<String> cluster : clusters_in_page) {				
				System.out.println("Paragraphs in cluster " + cluster.size());
				
				for (String paragraphID : cluster) {
					System.out.print(paragraphID + "\t");
				}
			}
		}
	}
	
	/*
	 * Function to retrieve score from the PageRank
	 */
	public static double getScore(String paraID) {
		for (String pageID : ranked_data.keySet()) {
			ArrayList<HashMap<String, Double>> clusters_in_page = ranked_data.get(pageID);
			for (HashMap<String, Double> cluster : clusters_in_page) {
				for (String paragraphID : cluster.keySet()) {
					if (paragraphID.equals(paraID)) {
						return cluster.get(paragraphID);
					}
				}
			}
		}
		return 0.0;
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		
		System.setProperty("file.encoding","UTF-8");
		
		// Check command line argument
		if (args.length < 4) {
			System.out.println("Command line arguments : <cluster-file> <LuceneIndex> <PathToCurlScript> <run-file>");
			System.exit(-1);
		}
		
		String index_path = args[1];
		IndexSearcher searcher = setUpIndexSearcher(index_path, "paragraph.lucene");
		searcher.setSimilarity(new BM25Similarity());
		final MyQueryBuilder query_builder = new MyQueryBuilder(new StandardAnalyzer());
		
		// Read in data 
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(args[0])));
		
		try {
			data = (HashMap<String, ArrayList<ArrayList<String>>>) ois.readObject();
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		ois.close();		
		
		// checkReadData();

		int count = 0;
		ranked_data = new HashMap<String, ArrayList<HashMap<String, Double>>>();
		// ranked_data_common_words = new HashMap<String, ArrayList<HashMap<String, Double>>>();
		
		for (String pageID : data.keySet()) {
			ArrayList<ArrayList<String>> clusters_in_page = data.get(pageID);
			
			ArrayList<HashMap<String, Double>> ranked_clusters = new ArrayList<HashMap<String, Double>>();
			// ArrayList<HashMap<String, Double>> ranked_clusters_common_words = new ArrayList<HashMap<String, Double>>();
			
			for (ArrayList<String> cluster : clusters_in_page) {				
				LinkedHashMap<String, List<String>> entities_in_paragraph = new LinkedHashMap<String, List<String>>();
				
				HashMap<String, Double> page_rank_result = new HashMap<String, Double>();
				// HashMap<String, Double> page_rank_from_common_words = new HashMap<String, Double>();
				
				// Map<String, String> paragraphs_text = new HashMap<String, String>();
				
				for (String paragraphID : cluster) {
					String query_string = paragraphID;
					TopDocs tops = searcher.search(query_builder.toIDQuery(query_string), 1);
					ScoreDoc[] score_doc = tops.scoreDocs;
					ScoreDoc score = score_doc[0];
					
					final Document doc = searcher.doc(score.doc);
	 				final String paragraph_text = doc.getField("text").stringValue();
	 				
	 				
					Entities e = new Entities();
	 				e.setUp(args[2]);
	 				
	 				List<String> e_list = e.retrieveEntities(paragraph_text);
	 				entities_in_paragraph.put(paragraphID, e_list);
	 				
	 				// paragraphs_text.put(paragraphID, paragraph_text);
				}
				
				// PageRank using entities in paragraphs
				PageRank pr = new PageRank();
				page_rank_result = pr.getPageRank(entities_in_paragraph);	
				
				// PageRank using common words
				// CommonWords cw = new CommonWords();
				// page_rank_from_common_words = cw.getPageRank(paragraphs_text);
				
				ranked_clusters.add(page_rank_result);
				// ranked_clusters_common_words.add(page_rank_from_common_words);
			}
			
			// Store ranked results per page
			ranked_data.put(pageID, ranked_clusters);
			// ranked_data_common_words.put(pageID, ranked_clusters_common_words);
			
			if (count % 25 == 0)
				System.out.println();
			System.out.print(".");
			count++;
		}
		
		// printOutput(ranked_data);
		// printOutput(ranked_data_common_words);

		FileWriter fw = new FileWriter("paramap-top-kmeans-train-pr.run", true);
		System.out.println("Writing run file..");
		count = 0;
		try {
			String runFile = args[3];
			String line = null;
			FileReader fr = new FileReader(runFile);
			BufferedReader br = new BufferedReader(fr);
			while ((line = br.readLine()) != null) {
				String[] splits = line.split(" ");
				String pgID = splits[0]; 
				String paraID = splits[2];
				String cluster_score = splits[4];

				double c_score = Double.parseDouble(cluster_score);
				double new_score = getScore(paraID) * c_score;

				String final_string = pgID + " Q0 " + paraID + " 0 " + new_score + " TOP\n";
				fw.write(final_string);
				
				if (count % 50 == 0)
					System.out.println();
				System.out.print(".");
				count++;
			}
			
			br.close();
			fw.close();
		}
		
		catch(FileNotFoundException e) {
			System.out.println("Unable to open file...");
		}
		
		catch(IOException e) {
			System.out.println("Error reading file...");
		}
		System.out.println("Done..");
	}
	
	/*
	 * Helper function to check the output
	 */
	public static void printOutput(HashMap<String, ArrayList<HashMap<String, Double>>> op) {
		
		for (String pageID : op.keySet()) {
			ArrayList<HashMap<String, Double>> clusters = op.get(pageID);
			
			for (HashMap<String, Double> cluster : clusters) {
				System.out.println( cluster.toString());
				
				for (String paragraphID : cluster.keySet()) {
					System.out.println(paragraphID + " : " + cluster.get(paragraphID));
				}
				
				System.out.println("============================");
			}
		}
	}
	
	/*
	 * Function to set up the index searcher
	 */
	private static IndexSearcher setUpIndexSearcher(String index_path, String type) throws IOException {
		Path path = FileSystems.getDefault().getPath(index_path, type);
		Directory index_directory = FSDirectory.open(path);
		IndexReader reader = DirectoryReader.open(index_directory);
		return new IndexSearcher(reader);
	}	
}