package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/**
 * 
 * @author Yannik Nelson
 * @version 1.0
 */
class Sensor extends Node {
	private String what3words;
	private Double battery;
	private String reading;
	
	Sensor(String what3words, Point location, Double battery, String reading) {
		super(location);
		this.what3words = what3words;
		this.battery = battery;
		this.reading = reading;
	}
	
	@Override
	public String toString() {
		return "Sensor: {"+ what3words + ", " + Double.toString(this.getLocation().longitude()) + ", " + Double.toString(this.getLocation().latitude()) + ", " +  Double.toString(battery)  + ", " +  reading  + "}";
	}

	/**
	 * @return the what3words
	 */
	String getWhat3words() {
		return what3words;
	}

	/**
	 * @return the battery
	 */
	Double getBattery() {
		return battery;
	}

	/**
	 * @return the reading
	 */
	String getReading() {
		return reading;
	}
	
}
