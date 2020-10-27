package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/**
 * 
 * @author Yannik Nelson
 * @version 1.0
 */
public class Sensor {
	private String what3words;
	private Point location;
	private Double battery;
	private String reading;
	
	public Sensor(String what3words, Point location, Double battery, String reading) {
		this.what3words = what3words;
		this.location = location;
		this.battery = battery;
		this.reading = reading;
	}
	
	@Override
	public String toString() {
		return "Sensor: {"+ what3words + ", " + Double.toString(location.longitude()) + ", " + Double.toString(location.latitude()) + ", " +  Double.toString(battery)  + ", " +  reading  + "}";
	}

	/**
	 * @return the what3words
	 */
	public String getWhat3words() {
		return what3words;
	}

	/**
	 * @return the location
	 */
	public Point getLocation() {
		return location;
	}

	/**
	 * @return the battery
	 */
	public Double getBattery() {
		return battery;
	}

	/**
	 * @return the reading
	 */
	public String getReading() {
		return reading;
	}
	
}
