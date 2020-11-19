package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Main Class used to create the required objects and call the plan fly and output functions for the Drone based on the arguements
 *
 */
public class App 
{
	
	//Constants that hold the boundaries of the area we can fly in
	private static final Double UPPERBOUND = 55.946233;
	private static final Double LOWERBOUND = 55.942617;
	private static final Double LEFTBOUND = -3.192473;
	private static final Double RIGHTBOUND = -3.184319;
	
	//Constants that give meaning to the indices of the args array
	private static final int DAYINDX = 0;
	private static final int MONTHINDX = 1;
	private static final int YEARINDX = 2;
	private static final int LATTINDX = 3;
	private static final int LONGINDX = 4;
	private static final int SEEDINDX = 5;
	private static final int PORTINDX = 6;
	
    public static void main(String[] args) {
    	Random generator = new Random();
    	generator.setSeed(Integer.parseInt(args[SEEDINDX]));
		
		//create the client that will handle all server communication
		ClientWrapper client = new Client(Integer.parseInt(args[PORTINDX]));
		
		Sensor start = new SensorNode("", Double.parseDouble(args[LONGINDX]),Double.parseDouble(args[LATTINDX]),0.0, "");
		Pather p = new AStarPather();
		Drone drone = new DevelopmentDrone(p, new TSPSolution(p, generator), new StepLogger(p), new GeoJsonVisualiser(), client, start, UPPERBOUND, LOWERBOUND, LEFTBOUND, RIGHTBOUND, 0.0003);
		try {
			ArrayList<Sensor> order = new ArrayList<Sensor>();
			order = drone.Plan(start, args[DAYINDX], args[MONTHINDX], args[YEARINDX]);
			drone.Fly(order, start);
			drone.ProduceOutput(args[DAYINDX], args[MONTHINDX], args[YEARINDX]);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
