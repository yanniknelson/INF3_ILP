package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Random;
import java.util.List;

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
import com.mapbox.geojson.BoundingBox;
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
public class Drone <T extends Node> {
	
	private static Random generator = new Random();
	private static Client client;
	//Create a custom Gson in order to parse the sensor data in a usefull way
	private static GsonBuilder gsonBuilder = new GsonBuilder();
	private static Gson SensorGson;
	//Custom deserializer for the Sensor Json that makes converting the Json to Sensor Objects easier
	private static JsonDeserializer<Sensor> deserializer = new JsonDeserializer<Sensor>() {
		@Override
		public Sensor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	        JsonObject jsonObject = json.getAsJsonObject();
	        Point location;
	        //convert the what3words string into a Point and pass that and the json attributes into the Sensor Constructor
			try {
				location = client.PointFromWords(jsonObject.get("location").getAsString());
				return new Sensor(jsonObject.get("location").getAsString(), location, jsonObject.get("battery").getAsDouble(), jsonObject.get("reading").getAsString());
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
	private static Type listType = new TypeToken<ArrayList<Sensor>>() {}.getType();
	
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
						connectionLengths.get(s1).put(s2, s1.AStar(s2).size());
					}
					//update the progress bar
					pb.step();
				}
			}
		}
		
		return connectionLengths;
	}
	/**
	 * Ant Colony Optimisation Algorithm for the Travelling Salesman Problem
	 * 
	 * @param connectionMatrix
	 * @param Sensors
	 * @return
	 */
	static ArrayList<Sensor> ACOTSP(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> sensors) {
		Double Q = 1.0;
		Double evap = 0.1;
		Integer bestLength = 1000;
		ArrayList<Sensor> bestRoute = new ArrayList<>();
		//Create and initialise the pheromone map
		HashMap<Sensor, HashMap<Sensor, Double>> pheromone = new HashMap<>();
		for (Sensor s1: sensors) {
			pheromone.put(s1, new HashMap<Sensor, Double>());
			for (Sensor s2: sensors) {
				pheromone.get(s1).put(s2, 1.0);
			}
		}
		
		Integer n = sensors.size();
		Integer k = (int) Math.floor(n*1);
		
		Double a = 1.0;
		Double b = 4.0;
		
		for (Integer t = 0; t < 100; t++) {
			ArrayList<Pair<ArrayList<Sensor>, ArrayList<Sensor>>> ants = new ArrayList<>();
			for (int i = 0; i < k; i++) {
				ants.add(new Pair<ArrayList<Sensor>,ArrayList<Sensor>>(new ArrayList<Sensor>(),(ArrayList<Sensor>) sensors.clone()));
				Sensor firstSensor = sensors.get(generator.nextInt(n));
				ants.get(i).getValue0().add(firstSensor);
				ants.get(i).getValue1().remove(firstSensor);
				
				while (ants.get(i).getValue1().size() > 0) {
					Sensor current = ants.get(i).getValue0().get(ants.get(i).getValue0().size() - 1);
					ArrayList<Sensor> possibleNext = ants.get(i).getValue1();
					Double sumWeight = 0.0;
					for (Sensor s: possibleNext) {
						sumWeight +=  Math.pow(pheromone.get(current).get(s), a) * Math.pow((1.0/connectionMatrix.get(current).get(s)),b);
					}
					Double p = generator.nextDouble();
					Double cumProb = 0.0;
					Sensor next = possibleNext.get(0);
					for (Sensor s: possibleNext) {
						cumProb +=  ((Math.pow(pheromone.get(current).get(s), a) * Math.pow((1.0/connectionMatrix.get(current).get(s)),b))/sumWeight);
						if (p <= cumProb) {
							next = s;
							break;
						}
					}
				
					ants.get(i).getValue0().add(next);
					ants.get(i).getValue1().remove(next);
				}
				ants.get(i).getValue0().add(ants.get(i).getValue0().get(0));
			}
			
			for (Sensor s1: sensors) {
				for (Sensor s2: sensors) {
					pheromone.get(s1).put(s2, pheromone.get(s1).get(s2) * (1-evap));
				}
			}
			
			for (int i = 0; i < k; i++) {
				ArrayList<Sensor> path = ants.get(i).getValue0();
				Integer totalLength = 0;
				for (Integer j = 1; j < path.size(); j++) {
					totalLength += connectionMatrix.get(path.get(j-1)).get(path.get(j));
				}
				if (totalLength < bestLength) {
					System.out.println("newBest");
					System.out.println(totalLength);
					bestLength = totalLength;
					bestRoute = path;
				}
				
				for (Integer j = 1; j < ants.get(i).getValue0().size(); j++) {
					pheromone.get(path.get(j-1)).put(path.get(j), pheromone.get(path.get(j-1)).get(path.get(j)) + (Q/totalLength));
				}
			}
		}
		
		System.out.println(bestLength);
		return bestRoute;
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
		gsonBuilder.registerTypeAdapter(Sensor.class, deserializer);
		SensorGson = gsonBuilder.create();
		
		generator.setSeed(Integer.parseInt(args[SEEDINDX]));
		
		//create the client that will handle all server communication
		client = new Client(Integer.parseInt(args[PORTINDX]));
		
		Sensor start = new Sensor("", Point.fromLngLat(Double.parseDouble(args[LONGINDX]),Double.parseDouble(args[LATTINDX])),0.0, "");
		
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
		
		ArrayList<Feature> testList = new ArrayList<>();
		ArrayList<Point> line = new ArrayList<>();
		
		for (Sensor s: order) {
			line.add(s.getLocation());
		}
		
		testList.add(Feature.fromGeometry(LineString.fromLngLats(line)));
		for (Sensor s: data) {
			if (s != start) {
				testList.add(getVisitedSensorFeature(s));
			}
		}
		System.out.println(order.get(0));
		System.out.println(order.get(order.size()-1));
		FeatureCollection testlin = FeatureCollection.fromFeatures(testList);
		Path pathToOutput = Paths.get(System.getProperty("user.dir"), "Sensortest.geojson");
		try {
			Files.write(pathToOutput, testlin.toJson().getBytes());
			System.out.println("File saved Successfully");
		} catch (Exception e) {
			System.out.println("Failed to write to the file heatmap.geojson");
		}
		
	}

}
