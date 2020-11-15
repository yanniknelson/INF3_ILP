package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Random;
import org.javatuples.Pair;

/**
 * The main class of the air quality mapping drone project.<br>
 * This is the class that runs when the jar is run.
 * 
 * @author Yannik Nelson
 * @version 1.0
 *
 */
public class Drone {
	
	static Random generator = new Random();
	private static ClientWrapper client;
	
	//Constants that give meaning to the indices of the args array
	private static final int DAYINDX = 0;
	private static final int MONTHINDX = 1;
	private static final int YEARINDX = 2;
	private static final int LATTINDX = 3;
	private static final int LONGINDX = 4;
	private static final int SEEDINDX = 5;
	private static final int PORTINDX = 6;
	
	//Constants that hold the boundaries of the area we can fly in
	static final Double UPPERBOUND = 55.946233;
	static final Double LOWERBOUND = 55.942617;
	static final Double LEFTBOUND = -3.192473;
	static final Double RIGHTBOUND = -3.184319;
	//Constant that defines the stepsize of the drone
	static final Double STEPSIZE = 0.0003;
	
	//Planning objects
	static Pather pather = new AStarPather();
	static TSPSolver tsp = new TSPSolution();
	//Logging and visualising objects
	static Logger logger = new StepLogger();
	static Visualiser vis = new GeoJsonVisualiser();
	
	//store the bounding boxes for each no flyzone in their own list and as the index to a hashmap where they map to the appropriate noflyzone
	static ArrayList<ArrayList<Location>> boundingBoxes = new ArrayList<>();
	static HashMap<ArrayList<Location>, ArrayList<Location>> noFlyZones = new HashMap<>();
	//store all the Sensors we want to visit
	private static ArrayList<Sensor> destinations;
	
	/**
	 * This function simulates the drone flying from sensor to sensor taking readings
	 * 
	 * @param centeredOrder The order in which the Sensors will be visited starting at the start position
	 * @param start The start position
	 */
	static void fly(ArrayList<Sensor> centeredOrder, Sensor start) {
		ArrayList<Pair<Location, Integer>> path = new ArrayList<>();
		//initialise lists to keep track of sensors we have visited and haven't visited aswell as set up to first point in the path
		ArrayList<Sensor> visitedSensors = new ArrayList<>();
		ArrayList<Sensor> SensorsNotVisited = (ArrayList<Sensor>) destinations.clone();
		SensorsNotVisited.remove(start);
		path.add(new Pair<Location, Integer>(centeredOrder.get(0),0));
		//the limit variable indicates whether the step limit has been reached
		Integer limit = -1;
		for (Integer i = 1; i < centeredOrder.size(); i++) {
			//for every sensor, move from the last point in the path and try to get within range of the sensor
			ArrayList<Pair<Location, Integer>> route = pather.path(path.get(path.size()-1).getValue0(), centeredOrder.get(i), 0.0002);
			//log the steps
			limit = logger.LogSteps(route, centeredOrder.get(i));
			//if the logger returned a maximum index then drop all elements past that index from the route
			if (limit > -1) {
				route = (ArrayList<Pair<Location, Integer>>) route.subList(0, limit);
			}
			//if the last location in the route is within range of the desired sensor add it to the visited sensors list and remove it 
			//from the no visited list
			if (pather.findDistance(route.get(route.size()-1).getValue0(), centeredOrder.get(i)) < 0.0002) {
				visitedSensors.add(client.getSensorData(centeredOrder.get(i)));
				SensorsNotVisited.remove(centeredOrder.get(i));
			}
			//remove the first point to disallow repeated points in the path and add the remaining locations to the path
			route.remove(0);
			path.addAll(route);
			//if we'd reached the step limit stop flying
			if (limit > -1) {
				break;
			}
		}
		//if we've not reached the step limit the attempt to return to the starting position in the same manner as before simply with a higher tollerance
		if (limit == -1) {
			ArrayList<Pair<Location, Integer>> route = pather.path(path.get(path.size()-1).getValue0(),centeredOrder.get(0),0.0003);
			limit = logger.LogSteps(route, centeredOrder.get(0));
			if (limit > -1) {
				route = (ArrayList<Pair<Location, Integer>>) route.subList(0, limit);
			}
			route.remove(0);
			path.addAll(route);
		}
		//add all the visited sensors, unvisited sensors and the flight path to the visualisation
		vis.AddVisitedSensors(visitedSensors);
		vis.AddNotVisitedSensors(SensorsNotVisited);
		vis.AddFlightPath(path);
	}

	public static void main(String[] args) {
		generator.setSeed(Integer.parseInt(args[SEEDINDX]));
		
		//create the client that will handle all server communication
		client = new Client(Integer.parseInt(args[PORTINDX]));
		
		Sensor start = new SensorNode("", Double.parseDouble(args[LONGINDX]),Double.parseDouble(args[LATTINDX]),0.0, "");
		//Get the no fly zone GeoJson and create the structures used to check collision
		try {
			Pair<ArrayList<ArrayList<Location>>, HashMap<ArrayList<Location>, ArrayList<Location>>> temp = client.getNoFly();
			boundingBoxes = temp.getValue0();
			noFlyZones = temp.getValue1();
		} catch (IOException e) {
			//Catch exception and print the message for quick clarity and then stack trace
			System.out.println("IOException");
			System.out.println(e.getMessage());
			System.out.println();
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			//Catch exception and print the message for quick clarity and then stack trace
			System.out.println("InterruptedException");
			System.out.println(e.getMessage());
			System.out.println();
			e.printStackTrace();
			return;
		}
		
		//Get the Sensor data for specified date and use it to create ArrayList of Sensors
		try {
			destinations = client.getDestinations(args[DAYINDX], args[MONTHINDX], args[YEARINDX]);
			destinations.add(0, start);
		} catch (IOException e) {
			//Catch exception and print the message for quick clarity and then stack trace
			System.out.println("IOException");
			System.out.println(e.getMessage());
			System.out.println();
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			//Catch exception and print the message for quick clarity and then stack trace
			System.out.println("InterruptedException");
			System.out.println(e.getMessage());
			System.out.println();
			e.printStackTrace();
			return;
		}
		
		//Get the order of sensors to visit
		ArrayList<Sensor> order = tsp.solve(destinations, start);
		//Fly around the sensors in the order we found
		fly(order, start);
		
		//Output the flighpath and readings files
		logger.OutputLogFile(String.format("flightpath-%s-%s-%s.txt", args[DAYINDX], args[MONTHINDX], args[YEARINDX]));
		vis.OuputVisualisation(String.format("readings-%s-%s-%s.geojson", args[DAYINDX], args[MONTHINDX], args[YEARINDX]));
		
	}

}
