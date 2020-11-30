package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import org.javatuples.Pair;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

/**
 * This is the Development implementation of the drone class
 * 
 * @author Yannik Nelson
 *
 */
public class DevelopmentDrone implements Drone{
	
	//Constant that defines the step size of the drone
	private final Double STEPSIZE;
	
	//the client that will be used to get all of the needed info
	private ClientWrapper client;
	//Planning objects
	private Pather pather;
	private TSPSolver tsp;
	//Logging and visualising objects
	private Logger logger;
	private Visualiser vis;
	
	public DevelopmentDrone(Pather p, TSPSolver t, Logger l, Visualiser v, ClientWrapper c, Location start, Double ub, Double lob, Double leb, Double rb, Double ss) {
		this.STEPSIZE = ss;
		this.pather = p;
		this.pather.setBounds(ub, lob, leb, rb);
		this.pather.setStepSize(this.STEPSIZE);
		this.tsp = t;
		this.logger = l;
		this.vis = v;
		this.client = c;
	}
	
	public ArrayList<Sensor> Plan(Sensor start, String day, String month, String year) throws IOException, InterruptedException {
		//get the no fly zones and bonding boxes from the client
		this.pather.setNoFlyZones(client.getNoFly());
		//get the 
		var destinations = client.getDestinations(day, month, year);
		destinations.add(0, start);
		return tsp.solve(destinations, start);
	}
	
	/**
	 * This function simulates the drone flying from sensor to sensor taking readings
	 * 
	 * @param destinations The order in which the Sensors will be visited starting at the start position
	 * @param start The start position
	 */
	public void Fly(ArrayList<Sensor> destinations, Sensor start) {
		var path = new ArrayList<Pair<Location, Integer>>();
		//initialise lists to keep track of sensors we have visited and haven't visited as well as set up to first point in the path
		var visitedSensors = new ArrayList<Sensor>();
		var SensorsNotVisited = (ArrayList<Sensor>) destinations.clone();
		SensorsNotVisited.remove(start);
		path.add(new Pair<Location, Integer>(destinations.get(0),0));
		//the limit variable indicates whether the step limit has been reached
		Integer limit = -1;
		//ProgressBarBuilder pbb = new ProgressBarBuilder().setStyle(ProgressBarStyle.ASCII).setUpdateIntervalMillis(1).setInitialMax(destinations.size()+1).setTaskName("Flying");
		//try (var pb = pbb.build()){
			for (Integer i = 1; i < destinations.size(); i++) {
				//for every sensor, move from the last point in the path and try to get within range of the sensor
				var route = pather.path(path.get(path.size()-1).getValue0(), destinations.get(i), 0.0002);
				//log the steps
				limit = logger.LogSteps(route, destinations.get(i));
				//if the logger returned a maximum index then drop all elements past that index from the route
				if (limit > -1) {
					route = (ArrayList<Pair<Location, Integer>>) route.subList(0, limit);
				}
				//if the last location in the route is within range of the desired sensor add it to the visited sensors list and remove it 
				//from the no visited list
				if (pather.findDistance(route.get(route.size()-1).getValue0(), destinations.get(i)) < 0.0002) {
					visitedSensors.add(client.getSensorData(destinations.get(i)));
					SensorsNotVisited.remove(destinations.get(i));
				}
				//remove the first point to disallow repeated points in the path and add the remaining locations to the path
				route.remove(0);
				path.addAll(route);
				//if we'd reached the step limit stop flying
				if (limit > -1) {
					break;
				}
				//pb.step();
			}
			//if we've not reached the step limit the attempt to return to the starting position in the same manner as before simply with a higher tollerance
			if (limit == -1) {
				var route = pather.path(path.get(path.size()-1).getValue0(),destinations.get(0),0.0003);
				limit = logger.LogSteps(route, destinations.get(0));
				if (limit > -1) {
					route = (ArrayList<Pair<Location, Integer>>) route.subList(0, limit);
				}
				route.remove(0);
				path.addAll(route);
			}
			//pb.step();
		//}
		//add all the visited sensors, unvisited sensors and the flight path to the visualisation
		vis.AddVisitedSensors(visitedSensors);
		vis.AddNotVisitedSensors(SensorsNotVisited);
		var t = new ArrayList<Location>();
		for (var p: path) {
			t.add(p.getValue0());
		}
		vis.AddFlightPath(t);
	}

	public void ProduceOutput(String day, String month, String year) {
		logger.OutputLogFile(String.format("flightpath-%s-%s-%s.txt", day, month, year));
		vis.OuputVisualisation(String.format("readings-%s-%s-%s.geojson", day, month, year));
	}

}
