	package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;


/**
 * 
 * Ant Colony Optimisation and 2-Opt Heuristic hybrid implementation of TSPSolver the interface
 * 
 * @author Yannik Nelson
 * @see TSPSolver
 */
class TSPSolution implements TSPSolver {

	private Pather pather;
	private Random generator;
	private HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix;
	
	TSPSolution (Pather p, Random g){
		this.pather = p;
		this.generator = g;
	}

	@Override
	public ArrayList<Sensor> solve(ArrayList<Sensor> sensors, Location start) {
		//create The fully connected graph representation required for the Travelling Salesman Solution as precomputing
		buildConnectionMatrix(sensors);
		//perform The Ant Colony Optimisation using the precomputed graph and output its (over) estimated length
		ArrayList<Sensor> order = ACOTSP(sensors);
		System.out.println(String.format("Estimated Tour Step Cost After Ant Colony: %d", getCost(order)));
		//perform the 2-Opt heuristic on the order produced by the Ant Colony Optimisation in order to try and fix bad cross overs and output the new estimated length
		Two_OPT(order);
		System.out.println(String.format("Estimated Tour Step Cost After 2-OPT: %d", getCost(order)));
		//recenter the order so that the first 'Sensor' is the start position and return the ordering
		Integer startIndex = order.indexOf(start);
		ArrayList<Sensor> centeredOrder = new ArrayList<>();
		for (Integer i = 0; i < order.size(); i++) {
			centeredOrder.add(order.get((startIndex+i)%order.size()));
		}
		return centeredOrder;
	}
	
	/**
	 * 
	 * Takes in a List of points and find the best distance (+1) between each point and every other point and placing those in a 2D HashMap of sorts
	 * 
	 * @param destinations ArrayList of Sensors to be visited (includes the starting location as a Sensor)
	 * @return A Mapping from any Sensor to a Mapping from any other sensor to the best number of steps between them + 1
	 */
	private void buildConnectionMatrix (ArrayList<Sensor> destinations) {
		
		//Initialise each row (or row of the 2D HashMap)
		connectionMatrix = new HashMap<>();
		for (Sensor s: destinations) {
			connectionMatrix.put(s, new HashMap<Sensor, Integer>());
		}
		//Create a new progress bar
		ProgressBarBuilder pbb = new ProgressBarBuilder().setStyle(ProgressBarStyle.ASCII).setUpdateIntervalMillis(1).setInitialMax(destinations.size()*destinations.size()).setTaskName("Building Connections");
		try (ProgressBar pb = pbb.build()){
			//For every combination of Sensors (where order matters) 
			for (Sensor s1: destinations) {
				for (Sensor s2: destinations) {
					//If the sensors are the same (we're on the diagonal of the matrix) store 0
					//otherwise find the path from the row sensor to the column sensor and store its length 
					//(this is the number of steps +1 as the A* assumes starting exactly at each sensor which will not be the case for the final route, this way allows an extra step to make up the difference)
					//Note: due to the object avoidance there are some cases where the 'distance' from a to b is not the same as that from b to a, thus the full matrix must be calculated
					if (s1==s2) {
						connectionMatrix.get(s1).put(s2, 0);
					} else {
						connectionMatrix.get(s1).put(s2, pather.path(s1,s2, 0.0002).size());
					}
					//update the progress bar
					pb.step();
				}
			}
		}
		//Display the connectionLengths Matrix highlighting that it is not a diagonal matrix by putting [] around pairs that would match but don't
		System.out.println("Estimated Connection Step Costs:");
		for (Sensor s1: destinations) {
			for (Sensor s2: destinations) {
				//if the symmetric item in the connection matrix to the one being printed is not the same value, put [] around it to highlight the difference
				if (connectionMatrix.get(s1).get(s2) != connectionMatrix.get(s2).get(s1)) {
					System.out.print(String.format("[%2d],", connectionMatrix.get(s1).get(s2)));
				} else {
					System.out.print(String.format(" %2d ,", connectionMatrix.get(s1).get(s2)));
				}
			}
			System.out.println();
		}
	}

	/**
	 * 
	 * Convenience function for finding the cost of a path
	 * 
	 * @param connectionMatrix The estimated length of each connection in drone steps
	 * @param path The path who's cost is desired
	 * @return The estimated cost in drone steps of the path
	 */
	private Integer getCost(ArrayList<Sensor> path) {
		Integer totalLength = 0;
		for (Integer j = 1; j < path.size(); j++) {
			totalLength += connectionMatrix.get(path.get(j-1)).get(path.get(j));
		}
		totalLength += connectionMatrix.get(path.get(path.size()-1)).get(path.get(0));
		return totalLength;
	}
	
	/**
	 * Ant Colony Optimisation Algorithm for the Travelling Salesman Problem
	 * 
	 * @param connectionMatrix Matrix of distances between nodes
	 * @param Sensors List of Locations to visit
	 * @return
	 */
	private ArrayList<Sensor> ACOTSP(ArrayList<Sensor> sensors) {
		//Q constant for tour length pheromone update
		Double Q = 1.0;
		//pheromone evaporation rate
		Double evap = 0.1;
		//initial bestLength and best route
		ArrayList<Sensor> bestRoute = (ArrayList<Sensor>) sensors.clone();
		Integer bestLength = getCost(bestRoute);
		//Create and initialise the pheromone map to have a pheromone of 1 on all connecitons
		HashMap<Sensor, HashMap<Sensor, Double>> pheromone = new HashMap<>();
		for (Sensor s1: sensors) {
			pheromone.put(s1, new HashMap<Sensor, Double>());
			for (Sensor s2: sensors) {
				pheromone.get(s1).put(s2, 1.0);
			}
		}
		
		//store the number of sensors and the number of ants each iteration for easier use
		Integer n = sensors.size();
		Integer k = n;
		
		//The weight given to the pheromone strength of a connection when choosing the next Sensor
		Double a = 1.0;
		//The weight given to the length of a connection when choosing the next Sensor
		Double b = 4.0;
		
		//Run the Ant simulation 100 times
		for (Integer t = 0; t < 100; t++) {
			//Create a new list of ants, represented as an ArrayLists of sensors the visited sensors in the order they were visited by that ant
			ArrayList<ArrayList<Sensor>> ants = new ArrayList<>();
			for (int i = 0; i < k; i++) {
				//Initialise the ant with an empty list for visited Sensors and a clone of the sensors list as the available Sensors
				ants.add(new ArrayList<Sensor>());
				ArrayList<Sensor> ant = ants.get(i);
				//Initialise a clone of the sensors list to be used as a list of the available Sensors for the ant (the sensors the ant hans't visited yet)
				ArrayList<Sensor> possibleNext = (ArrayList<Sensor>) sensors.clone();
				//Pick a random starting Sensor, add it to the visited list and remove it from the available sensors
				Sensor firstSensor = sensors.get(generator.nextInt(n));
				ant.add(firstSensor);
				possibleNext.remove(firstSensor);
				//Build the ant's tour probabilistically 
				while (possibleNext.size() > 0) {
					//get the current Sensor and sensors still available
					Sensor current = ant.get(ant.size() - 1);
					//Find the sum of the path weightings for the available Sensors
					Double sumWeight = 0.0;
					for (Sensor s: possibleNext) {
						sumWeight +=  Math.pow(pheromone.get(current).get(s), a) * Math.pow((1.0/connectionMatrix.get(current).get(s)),b);
					}
					//Pick a random value between 0 and 1
					Double p = generator.nextDouble();
					Double cumProb = 0.0;
					Sensor next = possibleNext.get(0);
					//For each sensor, if the random value chosen is less than or equal to the cumulative probability of available Sensors so far, choose that sensor next
					//This produces the desired distribution
					for (Sensor s: possibleNext) {
						//The probability of next sensor being chosen is it's weighting divided by the sum of the weightings of all the possible next sensors
						cumProb +=  ((Math.pow(pheromone.get(current).get(s), a) * Math.pow((1.0/connectionMatrix.get(current).get(s)),b))/sumWeight);
						if (p <= cumProb) {
							next = s;
							break;
						}
					}
					//add the next sensors chosen to the visited list and remove it from the available sensors
					ant.add(next);
					possibleNext.remove(next);
				}
			}
			
			//Apply the evaporation to the pheromones on the connections
			for (Sensor s1: sensors) {
				for (Sensor s2: sensors) {
					pheromone.get(s1).put(s2, pheromone.get(s1).get(s2) * (1-evap));
				}
			}
			//add pheromone to every connection travelled, proportional to the tour lengths of each path that used said connection
			//this is done by looking at every ant, finding its tour length and then adding Q/(the tour length) to every connection used
			for (int i = 0; i < k; i++) {
				ArrayList<Sensor> path = ants.get(i);
				Integer totalLength = getCost(path);
				//if the path currently being looked at is better than the previous best, store it and its length
				if (totalLength < bestLength) {
					bestLength = totalLength;
					bestRoute = path;
				}
				
				for (Integer j = 1; j < path.size(); j++) {
					pheromone.get(path.get(j-1)).put(path.get(j), pheromone.get(path.get(j-1)).get(path.get(j)) + (Q/totalLength));
				}
			}
		}
		//return the best tour found
		return bestRoute;
	}
	
	/**
	 * Convenience function for checking if reversing a subsection of a passed in order will improve the path length of the order
	 * 
	 * @param connectionMatrix Matrix of distances between nodes
	 * @param list Current ordering
	 * @param i Start index of subsection
	 * @param j End index of subsection
	 * @return A boolean representing if the verse of the subsection was better than the original
	 */
	private Boolean tryReverse(ArrayList<Sensor> list, Integer i, Integer j) {
		//initial path length
		Integer Initial = getCost(list);
		//reverse the subsection
		Collections.reverse(list.subList(i,j));
		//if the new path length is lower than the initial path length then return true and leave the order with the reversed subsection
		if (getCost(list) < Initial) {
			return true;
		}
		//otherwise undo the subsection reverse and return false
		Collections.reverse(list.subList(i,j));
		return false;
	}
	
	/**
	 * Two-Opt heuristic algorithm for TSP solution, all changes to the ordering are done 'in-place'
	 * 
	 * @param connectionMatrix Matrix of distances between nodes
	 * @param ordering Initial ordering of locations to visit
	 */
	private void Two_OPT(ArrayList<Sensor> ordering) {
		Boolean better = true;
		//run through all subsections of the ordering and check if reversing it improves the path length
		//if the ordering improves then run through all subsections again until no improvement is found
		while (better) {
			better = false;
			for (Integer j = 1; j < ordering.size(); j++) {
				for (Integer i = 0; i < j; i++) {
					better = tryReverse(ordering, i, j);
				}
			}
		}
	}

}
