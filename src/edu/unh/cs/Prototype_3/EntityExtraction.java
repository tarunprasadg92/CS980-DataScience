package edu.unh.cs.Prototype_3;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

/*
 * @author Tarun Prasad
 * 
 * This class uses DBPedia Spotlight for Entity Retrieval
 */
public class EntityExtraction
{
	List<String> entities;
	String command;
	String paragraph_text;
	
	public EntityExtraction(String curl_path, String ptext)
	{
		command = curl_path + "curl_command.sh";
		paragraph_text = ptext;
		entities = new ArrayList<String>();
	}
	
	public void printEntities(List<String> entities) 
	{
		System.out.println("Entities : ");
		for (String entity : entities) 
		{
			System.out.print(entity + "\t");
		}
	}
	
	public List<String> retrieveEntities() throws IOException, ParseException
	{
		ProcessBuilder pb = new ProcessBuilder(command, paragraph_text);
		Process proc = pb.start();
		
		BufferedReader output = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String out = null;
		out = output.readLine();
		
		if (out.equals(null))
		{
			System.out.println(out);
		}
		
		JSONParser parser = new JSONParser();
		JSONObject json = new JSONObject();
		json = (JSONObject)parser.parse(out);
		
		if (json.get("Resources") != null) 
		{
			JSONArray Resources = (JSONArray)json.get("Resources");
			Iterator<JSONObject> uris = Resources.iterator();
			
			while (uris.hasNext()) 
			{
				JSONParser sub_parser = new JSONParser();
				JSONObject u = (JSONObject)sub_parser.parse(uris.next().toString());
				String entity_with_path = (String)u.get("@URI");
				String[] entity_path_name = entity_with_path.split("/");
				String[] sub = entity_path_name[entity_path_name.length - 1].split("_");
				
				String entity = "";
			
				for (int i = 0; i < sub.length; i++) 
				{
					entity += sub[i] + " ";
				}
				
				entities.add(entity.trim());
			}
		}
		
		return entities;
	}
}