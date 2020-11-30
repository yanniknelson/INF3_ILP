package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import org.javatuples.Pair;

/**
 * 
 * Interface defining the minimum functions required of a module to log the steps of the drone's flight
 * 
 * @author Yannik Nelson
 *
 */
interface Logger {
	
	/**
	 * 
	 * Logs a list of steps strarting from one sensor ending at the next, this will indicate if the step total has reached 150
	 * 
	 * @param route
	 * @param sensor
	 * @return
	 */
	Integer LogSteps(ArrayList<Pair<Location, Integer>> route, Sensor sensor);
	
	/**
	 * Will save the txt file containing the logged steps to the passed in path (Note the path must end in .txt)
	 * @param filePath Path to save the visualisation to
	 */
	void OutputLogFile(String filePath);
}
