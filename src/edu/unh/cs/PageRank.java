package edu.unh.cs;

import java.io.IOException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Tarun Prasad
 * Implementation of PageRank algorithm
 */

public class PageRank {
	
	public static int link_matrix[][];
	public static double transition_probability_matrix[][];
	
	public static List<String> data;
	public static List<String> nodes;
	
	public static double teleportation_probability = 0.15;
	public static double page_rank_vector[];
	
	public static boolean convergence_reached = false;
	
	public static void findTopPages() {
		double top_10[] = new double[10];
		int top_pages[] = new int[10];
		double max = 0.0;
		int index = 0;
		int i;
		
		for (int j = 0; j < 10; j++) {
			max = page_rank_vector[0];
			index = 0;
			
			for (i = 1; i < page_rank_vector.length; i++) {
				if (max < page_rank_vector[i]) {
					max = page_rank_vector[i];
					index = i;
				}
			}
			
			top_10[j] = max;
			top_pages[j] = index;
			page_rank_vector[index] = Double.MIN_VALUE;
			
			System.out.println(top_10[j] + " : " + nodes.get(index));
		}
	}
	
	public static void performIteration() {
		double temp_vector[] = new double[data.size()];
		
		for (int i = 0; i < 1; i++) {
			
			for (int j = 0; j < data.size(); j++) {
		
				for (int k = 0; k < data.size(); k++) {
					temp_vector[j] += page_rank_vector[k] * transition_probability_matrix[k][j];
				}				
			}
		}
		
		convergence_reached = true;
		
		for (int m = 0; m < data.size(); m++) {
			if (Math.abs(page_rank_vector[m] - temp_vector[m]) > 0.0000001)
				convergence_reached = false;
			page_rank_vector[m] = temp_vector[m];
		}
	}
	
	public static void initializePageRankVector() {
		page_rank_vector = new double[data.size()];
		double val = 1.0 / data.size();
		
		for (int i = 0; i < data.size(); i++)  {
			page_rank_vector[i] = val;
		}
	}
	
	public static void initializePersonalizedPageRankVector(String[] node) {
		page_rank_vector = new double[data.size()];
		List<Integer> seed = new ArrayList<Integer>();
		
		for (int m = 0; m < node.length; m++) {
			int val = getIndex(node[m]);
			seed.add(val);
		}
			
		for (int i = 0; i < data.size(); i++)  {
			if (seed.contains(i)) {
				page_rank_vector[i] = 1.0 / node.length;
			}
			else {
				page_rank_vector[i] = 0;
			}
		}
				
	}
	
	public static void printPageRankVector() {
		
		for (int i = 0; i < data.size(); i++) {
			System.out.print(page_rank_vector[i] + " ");
		}
	}
	
	public static void computeTransitionProbabilityMatrix() {
		int N = data.size();
		
		for (int i = 0; i < N; i++) {
			int no_out_links = data.get(i).split("\t").length - 1;
			
			if (no_out_links == 0) {
				for (int j = 0; j < N; j++) {
					transition_probability_matrix[i][j] = (1.0 / N);
				}
			}
			else {
				for (int j = 0; j < N; j++) {
					if (link_matrix[i][j] == 1) {					
						transition_probability_matrix[i][j] = (teleportation_probability / N) + ((1.0 - teleportation_probability) * (1.0 / no_out_links));
					}
					else {
						transition_probability_matrix[i][j] = (teleportation_probability / N);
					}
				}
			}
			
		}
	}
	
	public static void initializeLinkMatrix() {
		link_matrix = new int[data.size()][];
		
		for (int i = 0; i < data.size(); i++) {
			link_matrix[i] = new int[data.size()];
		}
	}
	
	public static void initializeTransitionProbabilityMatrix() {
		transition_probability_matrix = new double[data.size()][];
		
		for (int i = 0; i < data.size(); i++) {
			transition_probability_matrix[i] = new double[data.size()];
		}
	}
	
	public static void printLinkMatrix() {
		
		for (int i = 0; i < data.size(); i++) {
			
			for (int j = 0; j < data.size(); j++) {
				System.out.print(link_matrix[i][j] + " ");
			}
			
			System.out.println();
		}
	}
	
	public static void printTransitionProbabilityMatrix() {
		
		for (int i = 0; i < data.size(); i++) {
			
			for (int j = 0; j < data.size(); j++) {
				System.out.print(transition_probability_matrix[i][j] + " ");
			}
			
			System.out.println();
		}
	}
	
	public static void computeLinkMatrix() {
		
		for (String element : data) {
			String[] links = element.split("\t");
			int source_id = getIndex(links[0]);
			
			for (int i = 1; i < links.length; i++) {
				int target_id = getIndex(links[i]);
				link_matrix[source_id][target_id] = 1;
			}
		}
	}
	
	public static void generateNodeID() {		
		nodes = new ArrayList<String>();
		
		for (String element : data) {
			nodes.add(element.split("\t")[0]);
		}
	}
	
	public static void printNodes() {
		
		for (String element : nodes) {
			System.out.println(element);
		}
	}
	
	public static void printData() {
		
		for (String element : data) {
			System.out.println(element);
		}
	}
	
	public static int getIndex(String node_id) {
		return nodes.indexOf(node_id);
	}
	
	public static void readData(String file) {
		try {
			data = new ArrayList<String>();
			FileReader file_reader = new FileReader(file);
			BufferedReader br = new BufferedReader(file_reader);
			String line = null;
			
			while ((line = br.readLine()) != null) {
				data.add(line);
			}
			br.close();
		}
		catch (FileNotFoundException ex) {
			System.out.println("Unable to open file..");
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}		
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 0) {
			System.out.println("Command line arguments : <Graph txt file>");
 			System.exit(-1);
		}
		
		String input_file = args[0];
		
		readData(input_file);
		
		initializeLinkMatrix();
		
		generateNodeID();
		
		computeLinkMatrix();
		
		initializeTransitionProbabilityMatrix();
		
		computeTransitionProbabilityMatrix();
		
		initializePageRankVector();
		
		int iteration_count = 0;
		while(!convergence_reached) {
			System.out.println("Iteration : " + iteration_count);
			performIteration();
			iteration_count++;
		}
		
		System.out.println("Convergence reached...");
		System.out.println("PageRank : ");
		findTopPages();
		
		System.out.println("----------------------------------");
		System.out.println("Personalized PageRank with seed 8551571, 9372953 and 9557678 : ");
		String[] seeds = {"8551571", "9372953", "9557678"};
				
		initializePersonalizedPageRankVector(seeds);
		
		printPageRankVector();
		
		iteration_count = 0;
		convergence_reached = false;
		while(!convergence_reached) {
			System.out.println("Iteration : " + iteration_count);
			performIteration();
			iteration_count++;
		}
		
		System.out.println("Convergence reached...");
		System.out.println("PageRank : ");
		findTopPages();
	}
}