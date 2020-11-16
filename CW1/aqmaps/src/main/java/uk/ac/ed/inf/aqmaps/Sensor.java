package uk.ac.ed.inf.aqmaps;

/**
 * 
 * Minimum required internal representation of a Sensor extends the Location interface
 * 
 * @author Yannik Nelson
 * @see Location
 */
interface Sensor extends Location{
	
	/**
	 * 
	 * @return The What3Words coordinates of the Sensor
	 */
	String getWhat3words();
	
	/**
	 * 
	 * @return The battery reading of the Sensor
	 */
	Double getBattery();
	
	/**
	 * 
	 * @return The air-quality reading of the sensor
	 */
	String getReading();
}
