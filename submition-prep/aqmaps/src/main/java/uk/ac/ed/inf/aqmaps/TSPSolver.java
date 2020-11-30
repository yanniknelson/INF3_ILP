package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

/**
 * 
 * Standard framework to interact with A TSP solution
 * 
 * @author Yannik Nelson
 *
 */
interface TSPSolver {
	
	/**
	 *  
	 *  Runs a TSP solution to visit all the passed in sensors start and returning to the passed in location
	 *  
	 * @param sensors List of sensors to visit
	 * @param start desired start and end Location
	 * @return A list of sensors in the order to be visited with the start location at the beginning of the list
	 */
	ArrayList<Sensor> solve(ArrayList<Sensor> sensors, Location start);
}
