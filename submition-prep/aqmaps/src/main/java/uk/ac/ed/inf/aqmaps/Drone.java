package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * The interface any Drone must implement in order to be used, must be able to plane the route, fly a route and produce an output.
 * 
 * @author Yannik Nelson
 *
 */
interface Drone {
	/**
	 * 
	 * Given a date and a start location plan a minimal route around the sensors and back
	 * 
	 * @param start Start Location in the form of a sensor
	 * @param day The day 
	 * @param month The month
	 * @param year The year
	 * @return An ordered list of sensors to visit on the passed in date
	 * @throws IOException
	 * @throws InterruptedException
	 */
	ArrayList<Sensor> Plan(Sensor start, String day, String month, String year) throws IOException, InterruptedException;
	
	/**
	 * 
	 * Given an order of sensors to follow, attempt to visit all of them, giving up if the 150 steps are reached
	 * 
	 * @param centeredOrder
	 * @param start
	 */
	void Fly(ArrayList<Sensor> centeredOrder, Sensor start);
	
	/**
	 * 
	 * Given the date output the log and visualisation for the drones steps and sensor visits in a file named using that date
	 * 
	 * @param day
	 * @param month
	 * @param year
	 */
	void ProduceOutput(String day, String month, String year);
}
