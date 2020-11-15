package uk.ac.ed.inf.aqmaps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Precision;
import org.javatuples.Pair;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


/**
 * 
 * A Node links a Point object with methods that will be used to find paths to another Node.
 * 
 * @author Yannik Nelson
 * @see com.mapbox.geojson.Point
 *
 */
public class Node implements Location {
	
	protected Point location;
	
	/**
	 * 
	 * @param location The MapBox Point associated with the location of interest for this Node.
	 * @see com.mapbox.geojson.Point
	 */
	Node(Point location) {
		this.location = location;
	}

	/**
	 * @return the location (in the form of a MapBox Point).
	 * @see com.mapbox.geojson.Point
	 */
	public Point getLocation() {
		return location;
	}
	
	/**
	 * Returns the toString value of the location of the Node
	 */
	@Override
	public String toString() {
		return this.location.toString();
	}
	
}



