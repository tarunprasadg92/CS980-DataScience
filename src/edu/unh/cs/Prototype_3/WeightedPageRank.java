package edu.unh.cs.Prototype_3;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/*
 * @author Tarun Prasad
 * 
 * Class to compute the Weighted Page Rank of paragraphs inside a given cluster
 */
public class WeightedPageRank
{
	public int weight_matrix[][];
	public double transition_probability_matrix[][];
	public double page_rank_vector[];
	
	public double teleportation_probability = 0.15;
	public boolean convergence_reached = false;
	public int num_rows;
	
	public Map<String, Integer> index_positions;
	public LinkedHashMap<String, List<String>> input;
	public HashMap<String, Double> page_ranks;
	
	/*
	 * Constructor to handle all the initialization process
	 */
	public WeightedPageRank(LinkedHashMap<String, List<String>> in)
	{
		input = new LinkedHashMap<String, List<String>>();
		input.putAll(in);
		num_rows = input.size();
		
		// Initialize Weight Matrix
		weight_matrix = new int[num_rows][];
		for (int i = 0; i < num_rows; i++)
		{
			weight_matrix[i] = new int[num_rows];
		}
		
		// Initialize Transition Probability Matrix
		transition_probability_matrix = new double[num_rows][];
		for (int i = 0; i < num_rows; i++)
		{
			transition_probability_matrix[i] = new double[num_rows];
		}
		
		// Initialize PageRank vector
		page_rank_vector = new double[num_rows];
		double val = 1.0 / num_rows;
		for (int i = 0; i < num_rows; i++)
		{
			page_rank_vector[i] = val;
		}
		
		// Create a mapping from paragraphID to index position in the graph matrix
		index_positions = new HashMap<String, Integer>();
		int index = 0;
		for (String nodes : input.keySet())
		{
			index_positions.put(nodes, index);
			index++;
		}
		
		computeWeightMatrix();
		computeTransitionProbabilityMatrix();	
		
		page_ranks = new HashMap<String, Double>();
	}
	
	/*
	 * Function to return the weighted pagerank values of the paragraphs
	 */	
	public HashMap<String, Double> getPageRanks()
	{
		if (num_rows == 1)
		{
			page_ranks.put(input.keySet().toString(), 1.0);
			return page_ranks;
		}
		
		performIteration();
		assignScores();
		return page_ranks;
	}
	
	/*
	 * Function to map the paragraph with the weighted pagerank score
	 */
	public void assignScores()
	{
		for (String node : input.keySet())
		{
			double value = getRank(node);
			page_ranks.put(node, value);
		}
	}
	
	/*
	 * Function to process every iteration of the weighted pagerank algorithm
	 */
	public void performIteration()
	{
		double temp_vector[] = new double[num_rows];
		
		for (int i = 0; i < 1; i++)
		{
			for (int j = 0; j < num_rows; j++)
			{
				for (int k = 0; k < num_rows; k++)
				{
					temp_vector[j] += page_rank_vector[k] * transition_probability_matrix[k][j];
				}
			}
		}
		
		convergence_reached = true;
		
		for (int m = 0; m < num_rows; m++)
		{
			if ((Math.abs(page_rank_vector[m] - temp_vector[m]) > 0.001))
			{
				convergence_reached = false;
			}
			
			page_rank_vector[m] = temp_vector[m];
		}
	}
	
	/*
	 * Function to return the number of out-links from a given node
	 */
	public int getNoOutlinks(int index)
	{
		int no_outlinks = 0;
		
		for (int i = 0; i < weight_matrix[index].length; i++)
		{
			if (weight_matrix[index][i] != 0)
			{
				no_outlinks++;
			}
		}
		
		return no_outlinks;
	}
	
	/*
	 * Function to get the total weight of a particular node
	 */
	public int getTotalWeight(int index)
	{
		int total_weight = 0;
		
		for (int i = 0; i < weight_matrix[index].length; i++)
		{
			if (weight_matrix[index][i] != 0)
			{
				total_weight += weight_matrix[index][i];
			}
		}
		
		return total_weight;
	}
	
	/*
	 * Function to compute the transition probability matrix
	 */
	public void computeTransitionProbabilityMatrix()
	{
		int i = 0;
		
		for (String nodes : input.keySet())
		{
			int no_outlinks = getNoOutlinks(i);
			
			if (no_outlinks == 0)
			{
				for (int j = 0; j < num_rows; j++)
				{
					transition_probability_matrix[i][j] = (1.0 / num_rows);
				}
			}
			else
			{
				int total_weight = getTotalWeight(i);
				
				for (int j = 0; j < num_rows; j++)
				{
					if (weight_matrix[i][j] != 0)
					{
						transition_probability_matrix[i][j] = (teleportation_probability / num_rows) + ((1.0 - teleportation_probability) * (weight_matrix[i][j] / total_weight));
					}
					else
					{
						transition_probability_matrix[i][j] = (teleportation_probability / num_rows);
					}
				}
			}
			i++;
		}
	}
	
	/*
	 * Function to compute the weight matrix used in computation
	 */
	public void computeWeightMatrix()
	{
		for(Map.Entry<String, List<String>> source_entry : input.entrySet())
		{
			int source_id = index_positions.get(source_entry.getKey());
			List<String> source_entities = source_entry.getValue();
			
			for (Map.Entry<String, List<String>> target_entry : input.entrySet())
			{
				int target_id = index_positions.get(target_entry.getKey());
				List<String> target_entities = target_entry.getValue();
				
				int entity_count = commonEntityCount(source_entities, target_entities);
				weight_matrix[source_id][target_id] = entity_count;
			}
		}
	}
	
	/*
	 * Function to count the common number of entities between two nodes
	 */
	public int commonEntityCount(List<String> source, List<String> target)
	{
		int count = 0;
		
		for (String s_en : source)
		{
			if (target.contains(s_en))
			{
				count++;
			}
		}
		
		return count;
	}
	
	/*
	 * Function to return the weighted pagerank value of a given node
	 */
	public double getRank(String node)
	{
		int position = index_positions.get(node);
		return page_rank_vector[position];
	}
	
	/*
	 * Helper function to print out the weighted pagerank vector
	 */
	public void printPageRankVector()
	{
		double sum = 0.0;
		
		for (int i = 0; i < num_rows; i++)
		{
			System.out.print(page_rank_vector[i] + " ");
			sum += page_rank_vector[i];
		}
		
		System.out.println();
		System.out.println("\nSum : " + sum);
	}
	
	/*
	 * Helper function to print out the transition probability matrix
	 */
	public void printTransitionProbabilityMatrix()
	{
		for (int i = 0; i < num_rows; i++)
		{
			for (int j = 0; j < num_rows; j++)
			{
				System.out.print(transition_probability_matrix[i][j] + " ");
			}
			
			System.out.println();
		}
	}
	
	/*
	 * Helper function to print out the graph data
	 */
	public void printData()
	{
		for (String nodes : input.keySet())
		{
			System.out.print(nodes + " : ");
			
			for (String linked_nodes : input.get(nodes))
			{
				System.out.print(linked_nodes + " ");
			}
			
			System.out.println();
		}
	}
	
	/*
	 * Function to return the index value of a node in the matrix
	 */
	public int getIndex(String node)
	{
		return index_positions.get(node).intValue();
	}
}