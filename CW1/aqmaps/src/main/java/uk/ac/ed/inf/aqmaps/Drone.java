package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.Random;
import org.javatuples.Pair;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

/**
 * The main class of the air quality mapping drone project.<br>
 * This is the class that runs when the jar is run.
 * 
 * @author Yannik Nelson
 * @version 1.0
 *
 */
public class Drone {
	
	private static Random generator = new Random();
	private static Client client;
	//Create a custom Gson in order to parse the sensor data in a usefull way
	private static GsonBuilder gsonBuilder = new GsonBuilder();
	private static Gson SensorGson;
	//Custom deserializer for the Sensor Json that makes converting the Json to Sensor Objects easier
	private static JsonDeserializer<Sensor> deserializer = new JsonDeserializer<Sensor>() {
		@Override
		public SensorNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	        JsonObject jsonObject = json.getAsJsonObject();
	        Point location;
	        //convert the what3words string into a Point and pass that and the json attributes into the Sensor Constructor
			try {
				location = client.PointFromWords(jsonObject.get("location").getAsString());
				return new SensorNode(jsonObject.get("location").getAsString(), location, jsonObject.get("battery").getAsDouble(), jsonObject.get("reading").getAsString());
			} catch (IOException e) {
				//Catch exception and print the message for quick clarity and then stack trace
				System.out.println("IOException");
				System.out.println(e.getMessage());
				System.out.println();
				e.printStackTrace();
				return null;
			} catch (InterruptedException e) {
				//Catch exception and print the message for quick clarity and then stack trace
				System.out.println("InterruptedException");
				System.out.println(e.getMessage());
				System.out.println();
				e.printStackTrace();
				return null;
			}
	    }
	};
	
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
	
	static final Double STEPSIZE = 0.0003;
	
	static ArrayList<ArrayList<Point>> boundingBoxes = new ArrayList<>();
	static HashMap<ArrayList<Point>, ArrayList<Point>> noFlyZones = new HashMap<>();
	private static ArrayList<Sensor> data;
	private static Type listType = new TypeToken<ArrayList<SensorNode>>() {}.getType();
	
	/**
	 * 
	 * Takes in a list of points and returns the corners of their bounding box
	 * 
	 * @param pts List of points
	 * @return ArrayList containing the four corners of the box that surrounds all the points
	 */
	static ArrayList<Point> boundsFromPointsList(ArrayList<Point> pts) {
		//initialise the max and min longitudes and latitudes to those of the first point, this ensures they will converge on the correct values
		Double north = pts.get(0).latitude();
		Double south = pts.get(0).latitude();
		Double east = pts.get(0).longitude();
		Double west = pts.get(0).longitude();
		
		//run through the points and find the maximum and minimum longitude and latitude values
		for (Point p: pts) {
			if (p.longitude() > east) {
				east = p.longitude();
			}
			if (p.longitude() < west) {
				west = p.longitude();
			}
			if (p.latitude() > north) {
				north = p.latitude();
			}
			if (p.latitude() < south) {
				south = p.latitude();
			}
		}
		//create the list of corner points
		ArrayList<Point> ret = new ArrayList<>();
		ret.add(Point.fromLngLat(west,north));
		ret.add(Point.fromLngLat(east,north));
		ret.add(Point.fromLngLat(east,south));
		ret.add(Point.fromLngLat(west,south));
		ret.add(Point.fromLngLat(west,north));
		return ret;
	}
	
	/**
	 * 
	 * Takes in a List of points and find the best distance (+1) between each point and every other point and placing those in a 2D HashMap of sorts
	 * 
	 * @param destinations ArrayList of Sensors to be visited (includes the starting location as a Sensor)
	 * @return A Mapping from any Sensor to a Mapping from any other sensor to the best number of steps between them + 1
	 */
	static HashMap<Sensor, HashMap<Sensor, Integer>> ConnectionMatrix(ArrayList<Sensor> destinations) {
		
		//Initialise each row (or row of the 2D HashMap)
		HashMap<Sensor, HashMap<Sensor, Integer>> connectionLengths = new HashMap<>();
		for (Sensor s: destinations) {
			connectionLengths.put(s, new HashMap<Sensor, Integer>());
		}
		
		//Create a new progress bar
		ProgressBarBuilder pbb = new ProgressBarBuilder().setStyle(ProgressBarStyle.ASCII).setUpdateIntervalMillis(1).setInitialMax(destinations.size()*destinations.size()).setTaskName("Building Connections");
		try (ProgressBar pb = pbb.build()){
			//For every combination of Sensors (where order matters) 
			for (Sensor s1: destinations) {
				for (Sensor s2: destinations) {
					//If the sensors are the same (we're on the diagonal of the matrix) store 0
					//otherwise find the path from the row sensor to the column sensor and store its length 
					//(this is the number of steps +1 as the A* assumes starting exactly at each sensor which will not be the case for the final route, this way allows an extra step to make up the difference)
					//Note: due to the object avoidance there are some cases where the 'distance' from a to b is not the same as that from b to a, thus the full matrix must be calculated
					if (s1==s2) {
						connectionLengths.get(s1).put(s2, 0);
					} else {
						connectionLengths.get(s1).put(s2, s1.path(s2, 0.0002).size());
					}
					//update the progress bar
					pb.step();
				}
			}
		}
		
		return connectionLengths;
	}
	
	/**
	 * 
	 * Convenience function for finding the cost of a path
	 * 
	 * @param connectionMatrix The estimated length of each connection in drone steps
	 * @param path The path who's cost is desired
	 * @return The estimated cost in drone steps of the path
	 */
	static Integer getCost(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> path) {
		Integer totalLength = 0;
		for (Integer j = 1; j < path.size(); j++) {
			totalLength += connectionMatrix.get(path.get(j-1)).get(path.get(j));
		}
		totalLength += connectionMatrix.get(path.get(path.size()-1)).get(path.get(0));
		return totalLength;
	}
	
	/**
	 * Ant Colony Optimisation Algorithm for the Travelling Salesman Problem
	 * 
	 * @param connectionMatrix Matrix of distances between nodes
	 * @param Sensors List of Locations to visit
	 * @return
	 */
	static ArrayList<Sensor> ACOTSP(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> sensors) {
		//Q constant for tour length pheromone update
		Double Q = 1.0;
		//pheromone evaporation rate
		Double evap = 0.1;
		//initial bestLength and best route (set to 1000 to ensure it's large than any intial route and a new arraylist to ensure compilation)
		Integer bestLength = 1000;
		ArrayList<Sensor> bestRoute = new ArrayList<>();
		//Create and initialise the pheromone map to have a pheromone of 1 on all connecitons
		HashMap<Sensor, HashMap<Sensor, Double>> pheromone = new HashMap<>();
		for (Sensor s1: sensors) {
			pheromone.put(s1, new HashMap<Sensor, Double>());
			for (Sensor s2: sensors) {
				pheromone.get(s1).put(s2, 1.0);
			}
		}
		
		//store the number of sensors and the number of ants each iteration for easier use
		Integer n = sensors.size();
		Integer k = n;
		
		//The weight given to the pheromone strenght of a connection when choosing the next Sensor
		Double a = 1.0;
		//The weight given to the length of a connection when choosing the next Sensor
		Double b = 4.0;
		
		//Run the Ant simulation 100 times
		for (Integer t = 0; t < 100; t++) {
			//Create a new list of ants, represented as an ArrayLists of sensors the visited sensors in the order they were visited by that ant
			ArrayList<ArrayList<Sensor>> ants = new ArrayList<>();
			for (int i = 0; i < k; i++) {
				//Initialise the ant with an empty list for visited Sensors and a clone of the sensors list as the available Sensors
				ants.add(new ArrayList<Sensor>());
				ArrayList<Sensor> ant = ants.get(i);
				//Initialise a clone of the sensors list to be used as a list of the available Sensors for the ant (the sensors the ant hans't visited yet)
				ArrayList<Sensor> possibleNext = (ArrayList<Sensor>) sensors.clone();
				//Pick a random starting Sensor, add it to the visited list and remove it from the available sensors
				Sensor firstSensor = sensors.get(generator.nextInt(n));
				ant.add(firstSensor);
				possibleNext.remove(firstSensor);
				//Build the ant's tour probabilistically 
				while (possibleNext.size() > 0) {
					//get the current Sensor and sensors still available
					Sensor current = ant.get(ant.size() - 1);
					//Find the sum of the path weightings for the available Sensors
					Double sumWeight = 0.0;
					for (Sensor s: possibleNext) {
						sumWeight +=  Math.pow(pheromone.get(current).get(s), a) * Math.pow((1.0/connectionMatrix.get(current).get(s)),b);
					}
					//Pick a random value between 0 and 1
					Double p = generator.nextDouble();
					Double cumProb = 0.0;
					Sensor next = possibleNext.get(0);
					//For each sensor, if the random value chosen is less than or equal to the cumulative probability of available Sensors so far, choose that sensor next
					//This produces the desired distribution
					for (Sensor s: possibleNext) {
						//The probability of next sensor being chosen is it's weighting divided by the sum of the weightings of all the possible next sensors
						cumProb +=  ((Math.pow(pheromone.get(current).get(s), a) * Math.pow((1.0/connectionMatrix.get(current).get(s)),b))/sumWeight);
						if (p <= cumProb) {
							next = s;
							break;
						}
					}
					//add the next sensors chosen to the visited list and remove it from the available sensors
					ant.add(next);
					possibleNext.remove(next);
				}
				//Once every sensor has been added, ensure the tour is complete by placing the initial sensor back at the end of the list
//				ant.add(ant.get(0));
			}
			
			//Apply the evaporation to the pheromones on the connections
			for (Sensor s1: sensors) {
				for (Sensor s2: sensors) {
					pheromone.get(s1).put(s2, pheromone.get(s1).get(s2) * (1-evap));
				}
			}
			//add pheromone to every connection travelled, proportional to the tour lengths of each path that used said connection
			//this is done by looking at every ant, finding its tour length and then adding Q/(the tour length) to every connection used
			for (int i = 0; i < k; i++) {
				ArrayList<Sensor> path = ants.get(i);
				Integer totalLength = getCost(connectionMatrix, path);
				//if the path currently being looked at is better than the previous best, store it and its length
				if (totalLength < bestLength) {
					bestLength = totalLength;
					bestRoute = path;
				}
				
				for (Integer j = 1; j < path.size(); j++) {
					pheromone.get(path.get(j-1)).put(path.get(j), pheromone.get(path.get(j-1)).get(path.get(j)) + (Q/totalLength));
				}
			}
		}
		//return the best tour found
		return bestRoute;
	}
	
	/**
	 * Convenience function for checking if reversing a subsection of a passed in order will improve the path length of the order
	 * 
	 * @param connectionMatrix Matrix of distances between nodes
	 * @param list Current ordering
	 * @param i Start index of subsection
	 * @param j End index of subsection
	 * @return A boolean representing if the verse of the subsection was better than the original
	 */
	static Boolean tryReverse(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> list, Integer i, Integer j) {
		//initial path length
		Integer Initial = getCost(connectionMatrix, list);
		//reverse the subsection
		Collections.reverse(list.subList(i,j));
		//if the new path length is lower than the initial path length then return true and leave the order with the reversed subsection
		if (getCost(connectionMatrix, list) < Initial) {
			return true;
		}
		//otherwise undo the subsection reverse and return false
		Collections.reverse(list.subList(i,j));
		return false;
	}
	
	/**
	 * Two-Opt heuristic algorithm for TSP solution, all changes to the ordering are done 'in-place'
	 * 
	 * @param connectionMatrix Matrix of distances between nodes
	 * @param ordering Initial ordering of locations to visit
	 */
	static void Two_OPT(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> ordering) {
		Boolean better = true;
		//run through all subsections of the ordering and check if reversing it improves the path length
		//if the ordering improves then run through all subsections again until no improvement is found
		while (better) {
			better = false;
			for (Integer j = 1; j < ordering.size(); j++) {
				for (Integer i = 0; i < j; i++) {
					better = tryReverse(connectionMatrix, ordering, i, j);
				}
			}
		}
	}
	
	/**
	 * Method to convert a visited Sensor into a Feature with the appropriate properties
	 * 
	 * @param Sensor we have visted and would like the feature and properties of
	 * @return Feature corresponding to the Sensor and the appropriate features
	 */
	static Feature getVisitedSensorFeature(Sensor sensor) {
		//create the feature and add the location property with the value of the what3words location of the sensor
		Feature sensorPoint = Feature.fromGeometry(sensor.getLocation());
		sensorPoint.addStringProperty("location", sensor.getWhat3words());
		//get the reading from the sensor and check if its valid based on the battery and the reading
		//if it's not a valid reading set the rgb-string, marker-color and marker-symbol to the appropriate values
		String value = sensor.getReading();
		if (sensor.getBattery() < 10.0 || value.equals("NaN") || value.equals("null")) {
			sensorPoint.addStringProperty("rgb-string", "#000000");
			sensorPoint.addStringProperty("marker-color", "#000000");
			sensorPoint.addStringProperty("marker-symbol", "cross");
			return sensorPoint;
		}
		//otherwise set the rgb-string, marker-color and marker-symbol to the appropriate values based off of the value of the reading
		Double val = Double.parseDouble(value);
		String colour = "";
		String markerSymbol = "";
		if (val < 32) {
			colour = "#00ff00";
			markerSymbol = "lighthouse";
		} else if (val < 64) {
			colour = "#40ff00";
			markerSymbol = "lighthouse";
		} else if (val < 96) {
			colour = "#80ff00";
			markerSymbol = "lighthouse";
		} else if (val < 128) {
			colour = "#c0ff00";
			markerSymbol = "lighthouse";
		} else if (val < 160) {
			colour = "#ffc000";
			markerSymbol = "danger";
		} else if (val < 192) {
			colour = "#ff8000";
			markerSymbol = "danger";
		} else if (val < 224) {
			colour = "#ff4000";
			markerSymbol = "danger";
		} else {
			colour = "#ff0000";
			markerSymbol = "danger";
		}
		sensorPoint.addStringProperty("rgb-string", colour);
		sensorPoint.addStringProperty("marker-color", colour);
		sensorPoint.addStringProperty("marker-symbol", markerSymbol);
		return sensorPoint;
	}

	public static void main(String[] args) {
		gsonBuilder.registerTypeAdapter(SensorNode.class, deserializer);
		SensorGson = gsonBuilder.create();
		
		generator.setSeed(Integer.parseInt(args[SEEDINDX]));
		
		//create the client that will handle all server communication
		client = new Client(Integer.parseInt(args[PORTINDX]));
		
		Sensor start = new SensorNode("", Point.fromLngLat(Double.parseDouble(args[LONGINDX]),Double.parseDouble(args[LATTINDX])),0.0, "");
		
		String noFlyGeoJson;
		//Get the no fly zone GeoJson and create the structures used to check collision
		try {
			noFlyGeoJson = client.getNoFly();
			for (Feature f: FeatureCollection.fromJson(noFlyGeoJson).features()) {
				ArrayList<Point> t = (ArrayList<Point>) ((Polygon) f.geometry()).outer().coordinates();
				ArrayList<Point> b = boundsFromPointsList(t);
				boundingBoxes.add(b);
				noFlyZones.put(b, t);
			}
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
			String dataJson = client.getData(args[DAYINDX], args[MONTHINDX], args[YEARINDX]);
			data = SensorGson.fromJson(dataJson, listType);
			data.add(0, start);
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
		
		//create The fully connected graph representation required for the Travelling Salesman Solution
		HashMap<Sensor, HashMap<Sensor, Integer>> connectionLengths = ConnectionMatrix(data);
		
		//Display the connectionLengths Matrix highlighting that it is not a diagonal matrix by putting [] around pairs that would match but don't
		System.out.println("Estimated Connection Step Costs:");
		for (Sensor s1: data) {
			for (Sensor s2: data) {
				if (connectionLengths.get(s1).get(s2) != connectionLengths.get(s2).get(s1)) {
					System.out.print(String.format("[%2d],", connectionLengths.get(s1).get(s2)));
				} else {
					System.out.print(String.format(" %2d ,", connectionLengths.get(s1).get(s2)));
				}
			}
			System.out.println();
		}
		
		ArrayList<Sensor> order = ACOTSP(connectionLengths, data);
		System.out.println(String.format("Estimated Tour Step Cost: %d",getCost(connectionLengths, order)));
		Two_OPT(connectionLengths, order);
		System.out.println(String.format("Estimated Tour Step Cost: %d",getCost(connectionLengths, order)));
		Integer startIndex = order.indexOf(start);
		ArrayList<Sensor> centeredOrder = new ArrayList<>();
		for (Integer i = 0; i < order.size(); i++) {
			centeredOrder.add(order.get((startIndex+i)%order.size()));
		}
		String outputLog = "";
		ArrayList<Pair<Location, Integer>> path = new ArrayList<>();
		path.add(new Pair<Location, Integer>(centeredOrder.get(0),0));
		Integer linenum = 1;
		for (Integer i = 1; i < centeredOrder.size(); i++) {
			ArrayList<Pair<Location, Integer>> route = path.get(path.size()-1).getValue0().path(centeredOrder.get(i), 0.0002);
			for (Integer j = 1; j < route.size(); j++) {
				Location from = route.get(j-1).getValue0();
				Location to = route.get(j).getValue0();
				String ln = Integer.toString(linenum) + "," + Double.toString(from.getLocation().longitude()) + "," + Double.toString(from.getLocation().latitude()) + "," + Integer.toString(route.get(j).getValue1()) + "," + Double.toString(to.getLocation().longitude()) + "," + Double.toString(to.getLocation().latitude()) + ",";
				if (j == route.size()-1) {
					ln += centeredOrder.get(i).getWhat3words();
				} else {
					ln += "null";
				}
				ln += "\n";
				outputLog += ln;
				linenum++;
			}
			route.remove(0);
			path.addAll(route);
		}
		ArrayList<Pair<Location, Integer>> route = path.get(path.size()-1).getValue0().path(centeredOrder.get(0),0.0003);
		for (Integer j = 1; j < route.size(); j++) {
			Location from = route.get(j-1).getValue0();
			Location to = route.get(j).getValue0();
			String ln = Integer.toString(linenum) + "," + Double.toString(from.getLocation().longitude()) + "," + Double.toString(from.getLocation().latitude()) + "," + Integer.toString(route.get(j).getValue1()) + "," + Double.toString(to.getLocation().longitude()) + "," + Double.toString(to.getLocation().latitude()) + ",";
			ln += "null";
			if (j != route.size()-1) {
				ln += "\n";
			}
			outputLog += ln;
			linenum++;
		}
		route.remove(0);
		path.addAll(route);
		
		System.out.println(String.format("Final Step Count: %s", linenum));
		System.out.println(String.format("Final Path Length: %s", path.size()));
		ArrayList<Feature> testList = new ArrayList<>();
		ArrayList<Point> line = new ArrayList<>();
		
		for (Pair<Location, Integer> p: path) {
			line.add(p.getValue0().getLocation());
		}
		
		testList.add(Feature.fromGeometry(LineString.fromLngLats(line)));
		for (Sensor s: data) {
			if (s != start) {
				testList.add(getVisitedSensorFeature(s));
			}
		}
		FeatureCollection testlin = FeatureCollection.fromFeatures(testList);
		Path pathToOutputJson = Paths.get(System.getProperty("user.dir"), String.format("readings-%s-%s-%s.geojson", args[DAYINDX], args[MONTHINDX], args[YEARINDX]));
		Path pathToOutput = Paths.get(System.getProperty("user.dir"), String.format("flightpath-%s-%s-%s.txt", args[DAYINDX], args[MONTHINDX], args[YEARINDX]));
		try {
			Files.write(pathToOutputJson, testlin.toJson().getBytes());
			Files.write(pathToOutput, outputLog.getBytes());
			System.out.println("Files saved Successfully");
		} catch (Exception e) {
			System.out.println("Failed to write to the files");
		}
		
	}

}
