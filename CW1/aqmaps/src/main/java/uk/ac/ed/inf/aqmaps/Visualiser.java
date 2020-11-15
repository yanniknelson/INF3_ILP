package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import org.javatuples.Pair;

/**
 * Interface defining the minimum functions required of a module to visualise the drone's flight
 * 
 * @author Yannik Nelson
 *
 */
interface Visualiser {
	/**
	 * Will add the appropriate features to the internal feature list for each sensor
	 * @param sensors An ArrayList of Sensors that have been visited
	 */
	void AddVisitedSensors(ArrayList<Sensor> sensors);
	
	/**
	 * Will add the appropriate features to the internal feature list for each sensor
	 * @param sensors An ArrayList of Sensors that have not been visited
	 */
	void AddNotVisitedSensors(ArrayList<Sensor> sensors);
	
	/**
	 * Will convert the flight path into a line and add that line to the internal feature list
	 * @param path The flight path
	 */
	void AddFlightPath(ArrayList<Pair<Location, Integer>> path);
	
	/**
	 * Will save the GeoJson of the internal feature list to the specified path
	 * @param filePath Path to save the visualisation to
	 */
	void OuputVisualisation(String filePath);
}
