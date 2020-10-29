package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Point;

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
class Client {
	
	/**
	 * The Java HttpClien that will be used to communicate with the server
	 */
	private HttpClient client = HttpClient.newHttpClient();
	
	private String baseURL;
	private JsonParser parser = new JsonParser();
	
	/**
	 * The Client must be initialised with the port it will use to communicate with the server
	 * 
	 * @param port The port the client will use to communicate to the server
	 */
	Client(Integer port) {
		baseURL = "http://localhost:" + Integer.toString(port) + "/";
	}
	
	/**
	 * 
	 * @return The GeoJson describing the no-fly zones from the server
	 * @throws IOException
	 * @throws InterruptedException
	 */
	String getNoFly() throws IOException, InterruptedException{
		//sends a request for the no-fly-zones GeoJson and returns the body of the response
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseURL + "buildings/no-fly-zones.geojson")).build();
		return client.send(request, BodyHandlers.ofString()).body();
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
	String getData(String day,String month, String year) throws IOException, InterruptedException{
		//sends a request for the air-quality-data on the specified day and returns the body of the response
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseURL +"/maps/"+ year + "/" + month + "/" + day + "/air-quality-data.json")).build();
		return client.send(request, BodyHandlers.ofString()).body();
	}
	
	/**
	 * Takes in a What3Words string and returns a Point at the corresponding location
	 * 
	 * @param Input A What3Words String
	 * @return Point at the location corresponding the passed in What3Words
	 * @throws IOException
	 * @throws InterruptedException
	 */
	Point PointFromWords(String Input) throws IOException, InterruptedException {
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
		return Point.fromLngLat(lng, lat);
	}

}
