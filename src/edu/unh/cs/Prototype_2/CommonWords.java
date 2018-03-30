package edu.unh.cs.Prototype_2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * @author Tarun Prasad
 * 
 * This class creates a graph with paragraphs as nodes and links are created
 * if they have common recurring words above the threshold.
 */
public class CommonWords {
	
	List<String> paragraphs_text;
	
	public int link_matrix[][];
	public double common_words_score[][];
	public double transition_probability_matrix[][];
	
	public double teleportation_probability = 0.15;
	public double page_rank_vector[];
	
	public boolean convergence_reached = false;
	public Map<String, Integer> indexPositions;
	public double common_words_average = 0.0;
	
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
	 * Function to initialize the common words score matrix to zeros
	 */
	public void initializeCommonWordsMatrix(int num_rows) {
		common_words_score = new double[num_rows][];
		
		for (int i = 0; i < num_rows; i++) {
			common_words_score[i] = new double[num_rows];
		}
	}
	
	/*
	 * Function to compute indices for paragraphs
	 */
	public void computeIndexPositions(Map<String, String> input) {
		int index = 0;

		for (String nodes : input.keySet()) {
			indexPositions.put(nodes, index);
			index++;
		}
	}
	
	/*
	 * Function to compute the link matrix from the common word matrix
	 */
	public void computeLinkMatrix(Map<String, String> input, int num_rows) {
		
		for (int i = 0; i < num_rows; i++) {
			
			for (int j = 0; j < num_rows; j++) {
				if (common_words_score[i][j] > common_words_average) {
					link_matrix[i][j] = 1;
				}
				else {
					link_matrix[i][j] = 0;
				}
			}
		}
	}
	
	/*
	 * Function to get the common word score
	 */
	public double getScore(String[] p1, String[] p2) {
		int total = Math.max(p1.length, p2.length);
		int common_count = 0;
		
		for (int i = 0; i < p1.length; i++) {
			for (int j = 0; j < p2.length; j++) {
				if (p1[i] == p2[j]) { 
					common_count++;
				}
			}
		}
		
		if (common_count == 0)
			return 0.0;
		
		return (double)(total / common_count);
	}
	
	/*
	 * Function to compute the common words matrix
	 */
	public void computeCommonWordsMatrix(Map<String, String> input, int num_rows) {
		int i = 0;
		
		for(String source_para : input.keySet()) {
			String[] paragraph_1_text = source_para.split("\\s+");
			int j = 0;
			
			for (String target_para : input.keySet()) {
				String[] paragraph_2_text = target_para.split("\\s+");
				common_words_score[i][j] = getScore(paragraph_1_text, paragraph_2_text);				
			}
			
			j++;
		}
		
		i++;
	}
	
	/*
	 * Function to compute the common word average, to be used as the threshold
	 */
	public void computeCommonWordsAverage(int num_rows) {
		double sum = 0.0;
		
		for (int i = 0; i < num_rows; i++) {
			
			for (int j = 0; j < num_rows; j++) {
				sum += common_words_score[i][j];
			}
		}
		
		common_words_average = (double)(sum / (num_rows * num_rows));
	}
	
	/*
	 * Function to return the number of outlinks in the graph
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
	public void computeTransitionProbabilityMatrix(Map<String, String> input, int num_rows) {
		int i = 0;
		
		for (String nodes : input.keySet()) {
			int no_out_links = getNoOutlinks(i);
			
			if (no_out_links == 0) {
				
				for (int j = 0; j < num_rows; j++) {
					transition_probability_matrix[i][j] = (1.0 / num_rows);
				}
			}
			else {
				
				for (int j = 0; j < num_rows; j++) {
					if (link_matrix[i][j] == 1) {					
						transition_probability_matrix[i][j] = (teleportation_probability / num_rows) + ((1.0 - teleportation_probability) * (1.0 / no_out_links));
					}
					else {
						transition_probability_matrix[i][j] = (teleportation_probability / num_rows);
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
	 * Function to return the page rank value after computation
	 */
	public double getPageRank(String node) {
		int position = indexPositions.get(node);
		return page_rank_vector[position];
	}
	
	/*
	 * Write the resultant page rank values to be returned
	 */
	public HashMap<String, Double> assignScores(int num_rows, Map<String, String> input) {
		HashMap<String, Double> results = new HashMap<String, Double>();
		
		for (String node : input.keySet()) {
			double pr_value = getPageRank(node);
			results.put(node, pr_value);
		}
		
		return results;
	}
	
	public HashMap<String, Double> getPageRank(Map<String, String> input) {
		int num_rows = input.size();
		
		if (num_rows == 1) {
			HashMap<String, Double> page_ranks = new HashMap<String, Double>();
			page_ranks.put(input.toString(), 1.0);
			return page_ranks;
		}
		
		initializeLinkMatrix(num_rows);
		
		initializeTransitionProbabilityMatrix(num_rows);
		
		initializeCommonWordsMatrix(num_rows);
		
		indexPositions = new HashMap<String, Integer>();
		computeIndexPositions(input);
		
		computeCommonWordsMatrix(input, num_rows);
		
		computeCommonWordsAverage(num_rows);
		
		computeLinkMatrix(input, num_rows);
		
		computeTransitionProbabilityMatrix(input, num_rows);
		
		initializePageRankVector(num_rows);
		
		performIteration(num_rows);
		
		HashMap<String, Double> page_ranks = new HashMap<String, Double>();
		page_ranks = assignScores(num_rows, input);
		
		return page_ranks;
	}
}