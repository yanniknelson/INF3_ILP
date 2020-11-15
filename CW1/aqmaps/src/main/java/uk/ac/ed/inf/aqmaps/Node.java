package uk.ac.ed.inf.aqmaps;

/**
 * 
 * A Node holds the important location information for a point of interest
 * 
 * @author Yannik Nelson
 *
 */
public class Node implements Location {
	
	Double longitude;
	Double latitude;
	
	/**
	 * 
	 * @param location The MapBox Point associated with the location of interest for this Node.
	 * @see com.mapbox.geojson.Point
	 */
	Node(Double longitude, Double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}

	
	
	/**
	 * Returns the toString value of the location of the Node
	 */
	@Override
	public String toString() {
		return Double.toString(this.longitude) + "," + Double.toString(this.latitude);
	}

	/**
	 * @return the longitude location.
	 */
	public Double longitude() {
		return this.longitude;
//		return this.location.longitude();
	}

	/**
	 * @return the latitude of the location
	 */
	public Double latitude() {
		return this.latitude;
//		return this.location.latitude();
	}
	
}



