package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;

import org.javatuples.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * The Client class handles all communication with the webserver that holds the required data such as:<br>
 * <ul>
 * <li>Air quality data</li>
 * <li>No-fly zones</li>
 * <li>What3Word longitude and lattidudes</li>
 * </ul>
 * Note: All of the functions of Client throw their errors as exceptions here are critical to the function of the program <br>
 * as such the exceptions are thrown to allow for better management of said exceptions.
 * @author Yannik Nelson
 * @version 1.0
 *
 */
class Client implements ClientWrapper{
	
	/**
	 * The Java HttpClient that will be used to communicate with the server
	 * @see java.net.http.HttpClient
	 */
	private HttpClient client = HttpClient.newHttpClient();
	
	private String baseURL;
	private JsonParser parser = new JsonParser();
	//Create a custom Gson in order to parse the sensor data in a usefull way
	private static GsonBuilder gsonBuilder = new GsonBuilder();
	private static Gson SensorGson;
	//Custom deserializer for the Sensor Json that makes converting the Json to Sensor Objects easier
	private JsonDeserializer<Sensor> deserializer = new JsonDeserializer<Sensor>() {
		@Override
		public SensorNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	        JsonObject jsonObject = json.getAsJsonObject();
	        Location location;
	        //convert the what3words string into a Point and pass that and the json attributes into the Sensor Constructor
			try {
				location = LocationFromWords(jsonObject.get("location").getAsString());
				return new SensorNode(jsonObject.get("location").getAsString(), location.longitude(), location.latitude(), jsonObject.get("battery").getAsDouble(), jsonObject.get("reading").getAsString());
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
	private static Type listType = new TypeToken<ArrayList<SensorNode>>() {}.getType();
	
	/**
	 * 
	 * Takes in a list of points and returns the corners of their bounding box
	 * 
	 * @param pts List of points
	 * @return ArrayList containing the four corners of the box that surrounds all the points
	 */
	static ArrayList<Location> boundsFromPointsList(ArrayList<Location> pts) {
		//initialise the max and min longitudes and latitudes to those of the first point, this ensures they will converge on the correct values
		Double north = pts.get(0).latitude();
		Double south = pts.get(0).latitude();
		Double east = pts.get(0).longitude();
		Double west = pts.get(0).longitude();
		
		//run through the points and find the maximum and minimum longitude and latitude values
		for (Location p: pts) {
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
		ArrayList<Location> ret = new ArrayList<>();
		ret.add(new Node(west,north));
		ret.add(new Node(east,north));
		ret.add(new Node(east,south));
		ret.add(new Node(west,south));
		ret.add(new Node(west,north));
		return ret;
	}
	
	/**
	 * The Client must be initialised with the port it will use to communicate with the server
	 * 
	 * @param port The port the client will use to communicate to the server
	 */
	Client(Integer port) {
		baseURL = "http://localhost:" + Integer.toString(port) + "/";
		gsonBuilder.registerTypeAdapter(SensorNode.class, deserializer);
		SensorGson = gsonBuilder.create();
	}
	
	/**
	 * 
	 * @return The GeoJson describing the no-fly zones from the server
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Pair<ArrayList<ArrayList<Location>>, HashMap<ArrayList<Location>, ArrayList<Location>>> getNoFly() throws IOException, InterruptedException {
		ArrayList<ArrayList<Location>> boundingBoxes = new ArrayList<>();
		HashMap<ArrayList<Location>, ArrayList<Location>> noFlyZones = new HashMap<>();
		//sends a request for the no-fly-zones GeoJson and returns the body of the response
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseURL + "buildings/no-fly-zones.geojson")).build();
		String noFlyGeoJson = client.send(request, BodyHandlers.ofString()).body();
		for (Feature f: FeatureCollection.fromJson(noFlyGeoJson).features()) {
			ArrayList<Point> t = (ArrayList<Point>) ((Polygon) f.geometry()).outer().coordinates();
			ArrayList<Location> z = new ArrayList<>();
			for (Point p: t) {
				z.add(new Node(p.longitude(), p.latitude()));
			}
			ArrayList<Location> b = boundsFromPointsList(z);
			boundingBoxes.add(b);
			noFlyZones.put(b, z);
		}
		return new Pair<ArrayList<ArrayList<Location>>, HashMap<ArrayList<Location>, ArrayList<Location>>>(boundingBoxes, noFlyZones);
	}
	
	/**
	 * Takes in date parameters and returns the air quality data JSON from said date
	 * 
	 * @param day The day of the desired date
	 * @param month The month of the desired date
	 * @param year The year of the desired date
	 * @return The JSON containing the data about the sensors to be read on the specified date
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public ArrayList<Sensor> getDestinations(String day,String month, String year) throws IOException, InterruptedException {
		//sends a request for the air-quality-data on the specified day and returns the body of the response
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseURL +"/maps/"+ year + "/" + month + "/" + day + "/air-quality-data.json")).build();
		String json = client.send(request, BodyHandlers.ofString()).body();
		return SensorGson.fromJson(json, listType);
	}
	
	/**
	 * Takes in a What3Words string and returns a Point at the corresponding location
	 * 
	 * @param Input A What3Words String
	 * @return Point at the location corresponding the passed in What3Words
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Location LocationFromWords(String Input) throws IOException, InterruptedException {
		//split the What3Words into its three components
		String words[] = Input.split("\\.",3);
		//use the components to build the path to the details for that What3Words location on the server and send a request for it.
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseURL +"/words/"+ words[0] + "/" + words[1] + "/" + words[2] + "/details.json")).build();
		//get the JSON in the body of the response and use it to build a JsonObject, then getting it's coordinates attribute also as a JsonObject
		String JSON = client.send(request, BodyHandlers.ofString()).body();
		JsonObject coords = parser.parse(JSON).getAsJsonObject().getAsJsonObject("coordinates");
		//get the lat and lng attributes of the coords JsonObject as Doubles and use them to construct the Point that is returned
		Double lat = coords.get("lat").getAsDouble();
		Double lng = coords.get("lng").getAsDouble();
		return new Node(lng, lat);
	}

	/**
	 * Stand in for getting data from sensors
	 * 
	 * @return Sensor holding data
	 */
	public Sensor getSensorData(Sensor sensor) {
		return sensor;
	}

}
