package uk.ac.ed.inf.aqmaps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.javatuples.Pair;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

/**
 * Visualiser Implementation for a GeoJson Output
 * 
 * @author Yannik Nelson
 * @see Visualiser
 */
class GeoJsonVisualiser implements Visualiser {
	
	//Stores all the features to be in the output GeoJson
	ArrayList<Feature> features = new ArrayList<>();

	/**
	 * Method to convert a visited Sensor into a Feature with the appropriate properties
	 * 
	 * @param Sensor we have visted and would like the feature and properties of
	 * @return Feature corresponding to the Sensor and the appropriate features
	 */
	Feature getVisitedSensorFeature(Sensor sensor) {
		//create the feature and add the location property with the value of the what3words location of the sensor
		Feature sensorPoint = Feature.fromGeometry(Point.fromLngLat(sensor.longitude(), sensor.latitude()));
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
	
	/**
	 * Method to convert an unvisited Sensor into a Feature with the appropriate properties
	 * 
	 * @param Sensor we have not visted and would like the feature and properties of
	 * @return Feature corresponding to the Sensor and the appropriate features
	 */
	Feature getNotVisitedSensorFeature(Sensor sensor) {
		//create the feature and add the location property with the value of the what3words location of the sensor
		Feature sensorPoint = Feature.fromGeometry(Point.fromLngLat(sensor.longitude(), sensor.latitude()));
		//set the appropriate properties
		sensorPoint.addStringProperty("location", sensor.getWhat3words());
		sensorPoint.addStringProperty("rgb-string", "#aaaaaa");
		sensorPoint.addStringProperty("marker-color", "#aaaaaa");
		return sensorPoint;
	}

	@Override
	public void AddVisitedSensors(ArrayList<Sensor> sensors) {
		//run through the sensors and add the result of getVisitedSensorFeature to the feature list
		for (Sensor s: sensors) {
			features.add(getVisitedSensorFeature(s));
		}
	}

	@Override
	public void AddNotVisitedSensors(ArrayList<Sensor> sensors) {
		//run through the sensors and add the result of getNotVisitedSensorFeature to the feature list
		for (Sensor s: sensors) {
			features.add(getNotVisitedSensorFeature(s));
		}
	}

	@Override
	public void AddFlightPath(ArrayList<Pair<Location, Integer>> path) {
		//print details about the path adn linestring being output
		System.out.println(String.format("Final Path Length: %d (%d Points added to LineString)", path.size()-1, path.size()));
		//Convert all of the locations visited to points and add then to the line list in order
		ArrayList<Point> line = new ArrayList<>();
		for (Pair<Location, Integer> p: path) {
			line.add(Point.fromLngLat(p.getValue0().longitude(), p.getValue0().latitude()));
		}
		//convert the list of points into a linestring, then into a feature and add it to the features list
		features.add(Feature.fromGeometry(LineString.fromLngLats(line)));
	}

	@Override
	public void OuputVisualisation(String filePath) {
		//convert the feature list into a FeatureCollection and save it's Json to the file relative to where the jar is being run
		FeatureCollection coll = FeatureCollection.fromFeatures(features);
		Path pathToOutput = Paths.get(System.getProperty("user.dir"), filePath);
		try {
			Files.write(pathToOutput, coll.toJson().getBytes());
			System.out.println("GeoJson saved Successfully");
		} catch (Exception e) {
			System.out.println("Failed to write GeoJson to the files");
		}
	}

}
