package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * Interface defining the minimum functions required of a client for the drone to get important data such as the desired sensors
 * 
 * @author Yannik Nelson
 *
 */
interface ClientWrapper {
	
	/**
	 * 
	 * Gets the no-fly-zones and formats them
	 * 
	 * @return	A pair holding the bounding boxes and a mapping from bounding boxes to their no-fly-zone
	 * @throws IOException
	 * @throws InterruptedException
	 */
	ArrayList<ArrayList<Location>> getNoFly() throws IOException, InterruptedException;
	
	/**
	 * 
	 * Gets the list of desired Sensors to visit for the passed in date
	 * 
	 * @param day
	 * @param month
	 * @param year
	 * @return	List of Sensors we wish for the drone to visit
	 * @throws IOException
	 * @throws InterruptedException
	 */
	ArrayList<Sensor> getDestinations(String day,String month, String year) throws IOException, InterruptedException;
	
	/**
	 * 
	 * Returns the Location correlated with the What3Words coordinate passed in
	 * 
	 * @param Input What3Words coordinate string
	 * @return	Location correlated to the passed in coordinates
	 * @throws IOException
	 * @throws InterruptedException
	 */
	Location LocationFromWords(String what3words) throws IOException, InterruptedException;
	
	/**
	 * 
	 * Attempts to read the data from the desired sensor
	 * 
	 * @param sensor The sensor that we with to read from
	 * @return A sensor object holding the desired data
	 */
	Sensor getSensorData(Sensor sensor);
}
