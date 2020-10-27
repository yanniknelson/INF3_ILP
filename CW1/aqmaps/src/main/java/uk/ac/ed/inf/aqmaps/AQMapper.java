package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * The main class of the air quality mapping drone project.<br>
 * This is the class that runs when the jar is run.
 * 
 * @author Yannik Nelson
 * @version 1.0
 *
 */
public class AQMapper {
	
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
	static final int DAYINDX = 0;
	static final int MONTHINDX = 1;
	static final int YEARINDX = 2;
	static final int LATTINDX = 3;
	static final int LONGINDX = 4;
	static final int SEEDINDX = 5;
	static final int PORTINDX = 6;
	
	private static FeatureCollection noFlyZones;
	private static ArrayList<Sensor> data;
	private static Type listType = new TypeToken<ArrayList<Sensor>>() {}.getType();

	public static void main(String[] args) {
		gsonBuilder.registerTypeAdapter(Sensor.class, deserializer);
		SensorGson = gsonBuilder.create();
		
		//create the client that will handle all server communication
		client = new Client(Integer.parseInt(args[PORTINDX]));
		
		//Get the no fly zone GeoJson and use it to create the feature collection
		try {
			String noFlyGeoJson = client.getNoFly();
			noFlyZones = FeatureCollection.fromJson(noFlyGeoJson);
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
	}

}
