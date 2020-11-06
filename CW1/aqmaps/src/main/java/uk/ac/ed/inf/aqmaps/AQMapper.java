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
public class AQMapper <T extends Node> {
	
	private static Random generator = new Random();
	private static Client client;
	//Create a custom Gson in order to parse the sensor data in a usefull way
	private static GsonBuilder gsonBuilder = new GsonBuilder();
	private static Gson SensorGson;
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
	
	static Double findDistance(Point p1, Point p2) {
		return Math.sqrt(Math.pow(p1.longitude() - p2.longitude(),2) + Math.pow(p1.latitude() - p2.latitude(),2));
	}
	
	static List<Feature> connectionToFeatures(Connection conn) {
		List<Feature> Features = new ArrayList<>();
		Features.add(Feature.fromGeometry(conn.getStart().getLocation()));
		List<Point> line = new ArrayList<>();
		ArrayList<Pair<Node, Integer>> points = conn.getPath();
		for (Integer i = 0; i < points.size(); i++) {
			line.add(points.get(i).getValue0().getLocation());
//			System.out.print(String.format("%d, ", points.get(i).getValue1()));
		}
//		System.out.println(String.format("The Line has %d Points", line.size()));
		Features.add(Feature.fromGeometry(LineString.fromLngLats(line)));
		Features.add(Feature.fromGeometry(conn.getEnd().getLocation()));
		return Features;
	}
	
	static Integer nearestAngle(Point a, Point b) {
		Double angle = Math.atan2(a.longitude()-b.longitude(), a.latitude()-b.latitude());
		Integer closest = (int) Math.round(angle/5) * 5;
		return closest;
	}
	
	static ArrayList<Point> boundsFromPointsList(ArrayList<Point> pts) {
		Double north = pts.get(0).latitude();
		Double south = pts.get(0).latitude();
		Double east = pts.get(0).longitude();
		Double west = pts.get(0).longitude();
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
		ArrayList<Point> ret = new ArrayList<>();
		ret.add(Point.fromLngLat(west,north));
		ret.add(Point.fromLngLat(east,north));
		ret.add(Point.fromLngLat(east,south));
		ret.add(Point.fromLngLat(west,south));
		ret.add(Point.fromLngLat(west,north));
		return ret;
	}

	public static void main(String[] args) {
		gsonBuilder.registerTypeAdapter(Sensor.class, deserializer);
		SensorGson = gsonBuilder.create();
		
		generator.setSeed(Integer.parseInt(args[SEEDINDX]));
		
		//create the client that will handle all server communication
		client = new Client(Integer.parseInt(args[PORTINDX]));
		
		Sensor start = new Sensor("", Point.fromLngLat(Double.parseDouble(args[LONGINDX]),Double.parseDouble(args[LATTINDX])),0.0, "");
		
		String noFlyGeoJson;
		//Get the no fly zone GeoJson and use it to create the feature collection
		try {
			noFlyGeoJson = client.getNoFly();
			for (Feature f: FeatureCollection.fromJson(noFlyGeoJson).features()) {
				ArrayList<Point> t = (ArrayList<Point>) ((Polygon) f.geometry()).outer().coordinates();
				ArrayList<Point> b = boundsFromPointsList(t);
				System.out.println(b);
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
		
		data.add(0, start);
		
		Integer connectionLengthsNoMap[][] = new Integer[data.size()][data.size()];
		HashMap<Sensor, HashMap<Sensor, Integer>> connectionLengths = new HashMap<>();
		for (Sensor s: data) {
			connectionLengths.put(s, new HashMap<Sensor, Integer>());
		}
		
		ProgressBarBuilder pbb = new ProgressBarBuilder().setStyle(ProgressBarStyle.ASCII).setUpdateIntervalMillis(1).setInitialMax(data.size()*data.size()).setTaskName("Building Connections");
		
		try (ProgressBar pb = pbb.build()){
			for (Sensor s1: data) {
				for (Sensor s2: data) {
					if (s1==s2) {
						connectionLengths.get(s1).put(s2, 0);
					} else {
						connectionLengths.get(s1).put(s2, s1.AStar(s2).size());
					}
					pb.step();
				}
			}
		}
		
		for (Sensor s1: data) {
			for (Sensor s2: data) {
				System.out.print(connectionLengths.get(s1).get(s2));
				System.out.print(", ");
			}
			System.out.println();
		}
		
		//create The fully connected graph required for the Travelling Salesman Solution
//		ConnectedGraph conGraph = new ConnectedGraph(start, data);
//		System.out.println("connections found");
//		
////		Connection test = (Connection) data.get(0).connections.get(data.get(4));
////		test.AStar();
////		System.out.println(test.getCost());
//		ArrayList<Feature> testList = new ArrayList<>();
//		for (Sensor s1 : data) {
//			for (Sensor s2 : data) {
//				if (s1 != s2) {
//					testList.addAll((ArrayList<Feature>) connectionToFeatures((Connection)s1.connections.get(s2)));
//					if ((((Connection) s1.connections.get(s2)).getPath().size()) != connectionLengths.get(s1).get(s2)) {
//						System.out.println("uh oh");
//						System.out.println((((Connection) s1.connections.get(s2)).getPath().size()));
//						System.out.println(connectionLengths.get(s1).get(s2));
//						System.out.println();
//					}
//				}
//			}
////			testList.addAll((ArrayList<Feature>) connectionToFeatures((Connection)s1.connections.get(start)));
//		}
//		testList.addAll(FeatureCollection.fromJson(noFlyGeoJson).features());
//		FeatureCollection testlin = FeatureCollection.fromFeatures(testList);
//		Path pathToOutput = Paths.get(System.getProperty("user.dir"), "test.geojson");
//		try {
//			Files.write(pathToOutput, testlin.toJson().getBytes());
//			System.out.println("File saved Successfully");
//		} catch (Exception e) {
//			System.out.println("Failed to write to the file heatmap.geojson");
//		}
		
	}

}
