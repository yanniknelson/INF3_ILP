package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;

import org.javatuples.Pair;

/**
 * 
 * Standard interface for path findng solutions
 * 
 * @author Yannik Nelson
 *
 */
interface Pather {

	/**
	 * Sets the areas in the flying area that the drone is to avoid
	 * @param noFlyZones
	 */
	void SetNoFlyZones(ArrayList<ArrayList<Location>> noFlyZones);
	
	/**
	 * Sets the are the drone must remain within
	 * 
	 * @param ub upperbound
	 * @param lob lowerbound
	 * @param leb leftbound
	 * @param bb rightbound
	 */
	public void SetBounds(Double ub, Double lob, Double leb, Double rb);
	
	/**
	 * Sets the distance the Pather should make each step
	 * @param ss
	 */
	public void SetStepSize(Double ss);
	
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
