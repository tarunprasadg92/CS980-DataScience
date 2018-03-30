package edu.unh.cs.Prototype_2;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;

/*
 * @author Tarun Prasad
 * 
 * Class to produce ranking of paragraphs inside cluster using PageRank algorithm
 */
public class PageRank {
	
	public int link_matrix[][];
	public double transition_probability_matrix[][];
	
	public double teleportation_probability = 0.15;
	public double page_rank_vector[];
	
	public boolean convergence_reached = false;
	public Map<String, Integer> indexPositions;
	
	/*
	 * Function to return the page rank value after computation
	 */
	public double getPageRank(String node) {
		int position = indexPositions.get(node);
		return page_rank_vector[position];
	}
	
	/*
	 * Function to initialize the link matrix to zeros
	 */
	public void initializeLinkMatrix(int num_rows) {
		link_matrix = new int[num_rows][];
		
		for (int i = 0; i < num_rows; i++) {
			link_matrix[i] = new int[num_rows];
		}
	}
	
	/*
	 * Function to initialize the transition probability matrix to zeros
	 */
	public void initializeTransitionProbabilityMatrix(int num_rows) {
		transition_probability_matrix = new double[num_rows][];
		
		for (int i = 0; i < num_rows; i++) {
			transition_probability_matrix[i] = new double[num_rows];
		}
	}	
	
	/*
	 * Function to check if there are common entities between paragraphs
	 */
	public boolean haveCommonEntities(List<String> se, List<String> te) {
		
		for (String s_en : se) {
			if (te.contains(s_en))
				return true;
		}
		
		return false;
	}
	
	/*
	 * Function to compute the link matrix
	 */
	public void computeLinkMatrix(LinkedHashMap<String, List<String>> data, int num_rows) {
		
		for (Map.Entry<String, List<String>> source_entry : data.entrySet()) {
			int source_id = indexPositions.get(source_entry.getKey());
			List<String> source_entities = source_entry.getValue();
			
			for (Map.Entry<String, List<String>> target_entry : data.entrySet()) {
				int target_id = indexPositions.get(target_entry.getKey());
				List<String> target_entities = target_entry.getValue();
				
				boolean link = haveCommonEntities(source_entities, target_entities);
				
				if (link == true)
					link_matrix[source_id][target_id] = 1;
				else
					link_matrix[source_id][target_id] = 0;
			}
		}
	}
	
	/*
	 * Function to return the number of out links in the graph
	 */
	public int getNoOutlinks(int index) {
		int no_outlinks = 0;
		
		for (int j = 0; j < link_matrix[index].length; j++) {
			if (link_matrix[index][j] == 1)
				no_outlinks++;
		}
		
		return no_outlinks;
	}
	
	/*
	 * Function to compute the transition probability matrix
	 */
	public void computeTransitionProbabilityMatrix(LinkedHashMap<String, List<String>> input) {
		int N = input.size();
		int i = 0;
		
		for (String nodes : input.keySet()) {
			int no_out_links = getNoOutlinks(i);
			
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
			
			i++;
		}
	}
	
	/*
	 * Function to initialize the page rank vector
	 */
	public void initializePageRankVector(int num_rows) {
		page_rank_vector = new double[num_rows];
		double val = 1.0 / num_rows;	
		
		for (int i = 0; i < num_rows; i++)  {
			page_rank_vector[i] = val;
		}
	}
	
	/*
	 * Function that multiplies page rank vector and transition probability matrix
	 */
	public void performIteration(int num_rows) {
		double temp_vector[] = new double[num_rows];
		
		for (int i = 0; i < 1; i++) {			
			for (int j = 0; j < num_rows; j++) {		
				for (int k = 0; k < num_rows; k++) {
					temp_vector[j] += page_rank_vector[k] * transition_probability_matrix[k][j];
				}				
			}
		}
		
		convergence_reached = true;
		
		for (int m = 0; m < num_rows; m++) {
			if ((Math.abs(page_rank_vector[m] - temp_vector[m]) > 0.001))
				convergence_reached = false;
			page_rank_vector[m] = temp_vector[m];
		}
	}
	
	/*
	 * Helper function to print out page rank vector
	 */
	void printPageRankVector(int num_rows) {
		for (int i = 0; i < num_rows; i++) {
			System.out.print(page_rank_vector[i] + "\t");
		}
		System.out.println();
	}
	
	/*
	 * Helper function to print out transition probability matrix
	 */
	void printTransitionProbabilityMatrix(int num_rows) {
		for (int i = 0; i < num_rows; i++) {
			for (int j = 0; j < num_rows; j++) {
				System.out.print(transition_probability_matrix[i][j] + "\t");
			}
			System.out.println();
		}
	}
	
	/*
	 * Write the resultant page rank values to be returned
	 */
	public HashMap<String, Double> assignScores(int num_rows, LinkedHashMap<String, List<String>> input) {
		HashMap<String, Double> results = new HashMap<String, Double>();
		
		for (String node : input.keySet()) {
			double pr_value = getPageRank(node);
			results.put(node, pr_value);
		}
		
		return results;
	}
	
	/*
	 * Function to compute indices for paragraphs
	 */
	public void computeIndexPositions(LinkedHashMap<String, List<String>> input) {
		int index = 0;

		for (String nodes : input.keySet()) {
			indexPositions.put(nodes, index);
			index++;
		}
	}
	
	/*
	 * Function to return the index value
	 */
	public int getIndex(String node) {
		return indexPositions.get(node).intValue();
	}
	
	/*
	 * Helper function to print the data
	 */
	public void printData(LinkedHashMap<String, List<String>> data) {
		
		for (String nodes : data.keySet()) {
			System.out.print(nodes + " : ");
			
			for (String linked_nodes : data.get(nodes)) {
				System.out.print(linked_nodes +"\t");
			}
			System.out.println();
		}
	}
	
	/*
	 * Helper function to verify the page rank vector values
	 */
	public void checkPageRankVector(int num_rows) {
		double sum = 0.0;
		for (int i = 0; i < num_rows; i++) {
			sum += page_rank_vector[i];
		}
		System.out.println(sum);
	}
	
	/*
	 * Main function for the class that creates the graph and computes the PageRank
	 */
	public HashMap<String, Double> getPageRank(LinkedHashMap<String, List<String>> input) {
		int num_rows = input.size();
		
		if (num_rows == 1) {
			HashMap<String, Double> page_ranks = new HashMap<String, Double>();
			page_ranks.put(input.keySet().toString(), 1.0);
			return page_ranks;
		}
		
		initializeLinkMatrix(num_rows);
		
		initializeTransitionProbabilityMatrix(num_rows);
		// printTransitionProbabilityMatrix(num_rows);
		indexPositions = new HashMap<String, Integer>();
		computeIndexPositions(input);
		
		computeLinkMatrix(input, num_rows);
		
		computeTransitionProbabilityMatrix(input);
		
		initializePageRankVector(num_rows);
		// checkPageRankVector(num_rows);
		
		performIteration(num_rows);
		
		HashMap<String, Double> page_ranks = new HashMap<String, Double>();
		page_ranks = assignScores(num_rows, input);
		
		return page_ranks;
	}
}