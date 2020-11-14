package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/**
 * 
 * Sensors are a special case of the Node class which includes sensor readings
 * 
 * @author Yannik Nelson
 * @version 1.0
 * @see Node
 */
class SensorNode extends Node implements Sensor {
	private String what3words;
	private Double battery;
	private String reading;
	
	/**
	 * 
	 * @param what3words The what3words String associated with the sensor
	 * @param location The point associated with the sensor
	 * @param battery The battery level of the sensor
	 * @param reading The reading of the sensor, can be a value, can be "null" or "NaN"
	 */
	SensorNode(String what3words, Point location, Double battery, String reading) {
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
	public String getWhat3words() {
		return what3words;
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
