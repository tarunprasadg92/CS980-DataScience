package edu.unh.cs.EntityLinking;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

import java.io.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/*
 * @author Tarun Prasad
 * 
 * The program takes in a list paragraph IDs and produces a list of entities
 * contained in the paragraph using DBPedia SpotLight. The results are then
 * compared with the ground truth and F-1 score is computed 
 */

public class Entity {
	
	/*
	 * Helper function to print out the map structure built
	 */
	public static void printMap(Map<String, List<String>> map) {
		for (Map.Entry<String, List<String>> l : map.entrySet()) {
			System.out.println(l.getKey() + " : " + l.getValue());	
		}
	}
	
	/*
	 * Function to compute F-1 score 
	 */
	public static void computeF1Score(Map<String, List<String>> ground_truth, Map<String, List<String>> dbpedia_result) {
		
		int total_retrieved_docs = 0;
		int total_relevant_docs = 0;
		int total_retrieved_relevant_docs = 0;
		
		for (Map.Entry<String, List<String>> l : ground_truth.entrySet()) {
			List<String> rel_docs = new ArrayList<String>();
			List<String> ret_docs = new ArrayList<String>();
			
			rel_docs = l.getValue(); 
						
			String key = l.getKey();
			ret_docs = dbpedia_result.get(key);
			
			total_relevant_docs += rel_docs.size();
			total_retrieved_docs += ret_docs.size();
			
			for (int i = 0; i < ret_docs.size(); i++) {
				String e = ret_docs.get(i);
				if (rel_docs.contains(e)) {
					total_retrieved_relevant_docs++;
				}
			}
		}
		
		// Compute F-1 score
		double retrieved_docs = (double)total_retrieved_docs / ground_truth.size();
		double relevant_docs = (double)total_relevant_docs / ground_truth.size();
		double relevant_docs_retrieved = total_retrieved_relevant_docs / ground_truth.size();
		
		double precision = relevant_docs_retrieved / retrieved_docs;
		double recall = relevant_docs_retrieved / relevant_docs;
		
		double f1 = (2 * precision* recall) / (precision + recall);
		System.out.println("\nF-1 Score : " + f1);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, ParseException {
		System.setProperty("file.encoding", "UTF-8");
		
		if (args.length < 2) {
			System.out.println("Usage : <CBOR File> <Location of CURL script>");
			System.exit(-1);
		}
		
		Map<String, List<String>> ground_truth = new HashMap<String, List<String>>();
		Map<String, List<String>> dbpedia_result = new HashMap<String, List<String>>();
		
		final String paragraphsFile = args[0];
		final FileInputStream fis = new FileInputStream(new File(paragraphsFile));
		
		String command = args[1] + "curl_command.sh";
		int counter = 0;
		
		for (Data.Paragraph p : DeserializeData.iterableParagraphs(fis)) {
			ground_truth.put(p.getParaId(), p.getEntitiesOnly());
			
			ProcessBuilder pb = new ProcessBuilder(command, p.getTextOnly());
			Process proc = pb.start();
			
			BufferedReader output = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			String out = null;
			out = output.readLine();
			
			List<String> dbpedia_entities = new ArrayList<String>();
			
			JSONParser parser = new JSONParser();
			JSONObject json = new JSONObject();
			json = (JSONObject)parser.parse(out);
			
			if (json.get("Resources") != null) {
				JSONArray Resources = (JSONArray)json.get("Resources");
				Iterator<JSONObject> uris = Resources.iterator();
				while (uris.hasNext()) {
					JSONParser sub_parser = new JSONParser();
					JSONObject u = (JSONObject)sub_parser.parse(uris.next().toString());
					String entity_with_path = (String)u.get("@URI");
					String[] entity_path_name = entity_with_path.split("/");
					String[] sub = entity_path_name[entity_path_name.length - 1].split("_");
					
					String entity = "";
					for (int i = 0; i < sub.length; i++) {
						entity += sub[i] + " ";
					}

					dbpedia_entities.add(entity.trim());
				}
			}			
			
			dbpedia_result.put(p.getParaId(), dbpedia_entities);
			
			counter++;
			System.out.print(".");
			if ((counter % 25) == 0) {
				System.out.println();
			}
				
		}
		
		//printMap(ground_truth);
		//System.out.println("=============================");
		//printMap(dbpedia_result);	
		
		computeF1Score(ground_truth, dbpedia_result);
	}	
}