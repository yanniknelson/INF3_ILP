package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import org.javatuples.Pair;

/**
 * 
 * 
 * 
 * @author Yannik Nelson
 *
 */
interface Pather {
	
	/**
	 * Attempts to find the shortest path from the start to the end while avoiding no-fly-zones accepting a final Location within the tolerance of the end Location
	 * 
	 * @param start The starting Location
	 * @param end The target Location
	 * @param tollerance The acceptable distance to the target
	 * @return A list of every Location visited paired with the angle the drone flew at to reach that location from the last
	 */
	ArrayList<Pair<Location, Integer>> path(Location start, Location end, Double tolerance);
	
	/**
	 * Defines a distance function between to Locations
	 * 
	 * @param p1 First Location
	 * @param p2 Second Location
	 * @return The distance between the two Locations
	 */
	Double findDistance(Location p1, Location p2);
}
