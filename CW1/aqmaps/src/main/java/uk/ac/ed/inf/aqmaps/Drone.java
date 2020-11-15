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
	
	static Random generator = new Random();
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
	
	static Pather pather = new AStarPather();
	static TSPSolver tsp = new TSPSolution();
	
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
		HashMap<Sensor, HashMap<Sensor, Integer>> connectionLengths = tsp.ConnectionMatrix(data);
		
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
		
//		ArrayList<Sensor> order = tsp.ACOTSP(connectionLengths, data);
//		System.out.println(String.format("Estimated Tour Step Cost: %d", tsp.getCost(connectionLengths, order)));
//		tsp.Two_OPT(connectionLengths, order);
//		System.out.println(String.format("Estimated Tour Step Cost: %d", tsp.getCost(connectionLengths, order)));
//		Integer startIndex = order.indexOf(start);
//		ArrayList<Sensor> centeredOrder = new ArrayList<>();
//		for (Integer i = 0; i < order.size(); i++) {
//			centeredOrder.add(order.get((startIndex+i)%order.size()));
//		}
		ArrayList<Sensor> centeredOrder = tsp.solve(connectionLengths, data, start);
		String outputLog = "";
		ArrayList<Pair<Location, Integer>> path = new ArrayList<>();
		path.add(new Pair<Location, Integer>(centeredOrder.get(0),0));
		Integer linenum = 1;
		for (Integer i = 1; i < centeredOrder.size(); i++) {
			ArrayList<Pair<Location, Integer>> route = pather.path(path.get(path.size()-1).getValue0(), centeredOrder.get(i), 0.0002);
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
		ArrayList<Pair<Location, Integer>> route = pather.path(path.get(path.size()-1).getValue0(),centeredOrder.get(0),0.0003);
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
