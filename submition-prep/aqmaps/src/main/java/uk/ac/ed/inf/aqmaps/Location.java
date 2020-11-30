package uk.ac.ed.inf.aqmaps;

/**
 * Internal representation of a map point
 * 
 * @author Yannik Nelson
 *
 */
interface Location {
	/**
	 * 
	 * @return The longitude value of the Location
	 */
	Double longitude();
	
	/**
	 * 
	 * @return The latitude value of the Location
	 */
	Double latitude();
}
