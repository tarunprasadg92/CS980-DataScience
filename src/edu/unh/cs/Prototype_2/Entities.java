package edu.unh.cs.Prototype_2;

import java.io.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/*
 * @author Tarun Prasad
 * 
 * 
 * This class uses DBPedia Spotlight for Entity Retrieval
 */
public class Entities {
	
	List<String> entities;
	String command;
	
	/*
	 * Function to set up the command script
	 */
	public void setUp(String curl_script) {
		command = curl_script + "curl_command.sh";
		entities = new ArrayList<String>();
	}
	
	/*
	 * Helper function to print out the entities retrieved
	 */
	public void printEntities(List<String> entities) {
		System.out.println("Entities : ");
		
		for (String entity : entities) {
			System.out.print(entity + "\t");
		}
	}
	
	/*
	 * Function to retrieve the list of entities
	 */
	public List<String> retrieveEntities(String paragraph_text) throws IOException, ParseException{
		
		ProcessBuilder pb = new ProcessBuilder(command, paragraph_text);
		Process proc = pb.start();
		
		BufferedReader output = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		
		String out = null;
		out = output.readLine();
		
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
				
				entities.add(entity.trim());
			}
		}
		
		return entities;
	}
}