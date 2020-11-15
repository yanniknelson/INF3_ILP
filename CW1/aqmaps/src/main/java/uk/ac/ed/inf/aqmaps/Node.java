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

	/**
	 * 
	 * Takes in two points and return the Euclidean distance between them.
	 * 
	 * @param p1 First Point
	 * @param p2 Second Point
	 * @return The Distance between the passed in points as a Double.
	 * @see com.mapbox.geojson.Point
	 */
	public Double findDistance(Point p1, Point p2) {
		return Math.sqrt(Math.pow(p1.longitude() - p2.longitude(),2) + Math.pow(p1.latitude() - p2.latitude(),2));
	}
	
	/**
	 * 
	 * Checks to see if Point b in on the line segment between Point a and Point c.
	 * 
	 * @param a Start of the line segment
	 * @param b Point to be checked
	 * @param c End of the line segment
	 * @return Boolean representing if point b is on the line segment.
	 */
	static boolean onSegment(Point a, Point b, Point c) { 
		if (b.longitude() <= Math.max(a.longitude(), c.longitude()) && b.longitude() >= Math.min(a.longitude(), c.longitude()) && 
	            b.latitude() <= Math.max(a.latitude(), c.latitude()) && b.latitude() >= Math.min(a.latitude(), c.latitude())) {
			return true; 
	    } 
	    return false; 
	} 
	  
	/**
	 * Determines the orientation of the ordered triplet (a, b, c)
	 * <ul>
	 * 	<li>-1 --> Anti-Clockwise</li>
	 *  <li> 0 --> a, b and c are colinear</li>
	 *  <li> 1 --> Clockwise</li>
	 * </ul>
	 * 
	 * @param a First Point
	 * @param b	Second Point
	 * @param c	Third Point
	 * @return Integer corresponding to the orientation of the three passed in points.
	 */
	static int orientation(Point a, Point b, Point c) { 
	        Double val = (b.latitude() - a.latitude()) * (c.longitude() - b.longitude()) - (c.latitude() - b.latitude()) * (b.longitude() - a.longitude()); 
	        return val.compareTo(0.0);
	} 
	  
    /**
     * 
     * Returns True if the line segments ab and cd intersect.
     *  
     * @param a Start of first line segment
     * @param b	End of first line segment
     * @param c	Start of second line segment
     * @param d End of second line segment
     * @return Boolean representing whether the line segments intersect.
     */
	static boolean intersect(Point a, Point b, Point c, Point d) { 
	        //Find the four orientations needed for  
	        //general and special cases 
	        int o1 = orientation(a, b, c); 
	        int o2 = orientation(a, b, d); 
	        int o3 = orientation(c, d, a); 
	        int o4 = orientation(c, d, b); 
	        
	        
	  
	        //General case 
	        if (o1 != o2 && o3 != o4) { 
	            return true; 
	        } 
	  
	        //Special Cases 
	        //a, b and c are colinear and 
	        //c lies on segment ab 
	        if (o1 == 0 && onSegment(a, c, b)) { 
	            return true; 
	        } 
	  
	        //a, b and c are colinear and 
	        //d lies on segment ab 
	        if (o2 == 0 && onSegment(a, d, b)) { 
	            return true; 
	        } 
	  
	        //c, d and a are colinear and 
	        //a lies on segment cd 
	        if (o3 == 0 && onSegment(c, a, d)) { 
	            return true; 
	        } 
	  
	        //c, d and b are colinear and 
	        //b lies on segment cd 
	        if (o4 == 0 && onSegment(c, b, d)) { 
	            return true; 
	        } 
	  
	        //Doesn't fall in any of the above cases 
	        return false;  
    } 
	
	/**
	 * 
	 * Finds all of the positions around the current Node that the Drone can reach in one step.<br>
	 * Excludes points inside no fly zones and that would require the Drone to fly through a no fly zone.<br>
	 * Also excludes points outside the designated area.
	 * 
	 * 
	 * @param visited ArrayList of Node (or node subclass) that have already been visited and thus we don't want to be too close to.
	 * @return mapping of possible next step from the current node to their angle relative to the current node.
	 */
	public HashMap<Location, Integer> Reachable( ArrayList<Location> visited) {
		HashMap<Location, Integer> nextPoints = new HashMap<>();
		//for all possible angles around the current point (the drone can only move at angles divisible by 5)
		outerloop:
		for (Integer i = 0 ; i < 360; i += 5) {
			//find the new longitude and latitude and gives up on the Point at this angle if the Point will be outside the designated flying area
			Double lon = this.location.longitude() + Drone.STEPSIZE * Math.cos(Math.toRadians(i));
			if (lon > Drone.RIGHTBOUND || lon < Drone.LEFTBOUND) {
				continue;
			}
			Double lat = this.location.latitude() + Drone.STEPSIZE * Math.sin(Math.toRadians(i));
			if (lat > Drone.UPPERBOUND || lat < Drone.LOWERBOUND) {
				continue;
			}
			
			Point loc = Point.fromLngLat(lon,lat);
			
			//If the new point is less than a step away from any of the visited points then give up on the Point at this angle
			for (Location p: visited) {
				if (findDistance(p.getLocation(), loc) < 0.0003) {
					continue outerloop;
				}
			}
			
			//If the point will be in the designated area then we check if the point is inside or would intersect the sides of any of the no fly zones bounding boxes
			//If either of those is the case we then check if the line from the current point to the point being created intersects any of the sides of the no fly zone
			//If the line does intersect we give up on the Point at this angle
			for (ArrayList<Point> b: Drone.boundingBoxes) {
				if ((lon < b.get(2).longitude() && lon > b.get(0).longitude() && lat < b.get(0).latitude() && lat > b.get(2).latitude()) || intersect(location, loc, b.get(0), b.get(1)) || intersect(location, loc, b.get(1), b.get(2)) || intersect(location, loc, b.get(2), b.get(3)) || intersect(location, loc, b.get(3), b.get(4))) {
					//need to do more detailed check
					ArrayList<Point> outline = Drone.noFlyZones.get(b);
					for (Integer j = 1; j < outline.size(); j++) {
						if (intersect(location, loc, outline.get(j-1), outline.get(j))) {
							continue outerloop;
						}
					}
				}
			}
			
			//If none of the checks have triggered then the current point is valid so we add it to the mapping to be returned with its angle
			nextPoints.put(new Node(loc),i);
		}
		return nextPoints;
	}
	
	/**
	 * Performs an A* search to find the shortest path (in terms of steps) to the passed in Node (or Node subclass).
	 * 
	 * @param end The desired destination point.
	 * @return A list of pairs Nodes (or Node subclass) and angles representing the locations visited at each step and the angle between them.
	 */
	public ArrayList<Pair<Location, Integer>> path(Location end, Double tolerance) {
		//Create a Comparator that will compare possible paths and return an order based on the total expected cost to get to the end Point
		AStarNodeComparison comparator = new AStarNodeComparison(end);
		//List of all current possible paths (called branches due to the tree like nature of the search)
		ArrayList<ArrayList<Pair<Location, Integer>>> branches = new ArrayList<>();
		//Create the first node (tree node) in the search tree containing just the current Node (or Node subclass)
		ArrayList<Pair<Location,Integer>> base = new ArrayList<Pair<Location,Integer>>();
		base.add(new Pair<Location,Integer>((Location) this, -1));
		//Add the first (tree) node to the branches list
		branches.add(base);
		//initialise the visited list to and empty ArrayList
		ArrayList<Location> visited = new ArrayList<>();
		//Iteratively expand upon the current best paths, then ordering the new branches and repeating until the destination is reached
		while (true) {
			//get the current best path
			ArrayList<Pair<Location, Integer>> current = branches.get(0);
			//if the current best path is within 0.0002 degrees of the destination we've reached the destination so return the path
			if (findDistance(current.get(current.size()-1).getValue0().getLocation(), end.getLocation()) < tolerance ) {
				//if the current path consists of only one node then the drone hasn't moved, this would be an invalid step
				if (current.size() == 1) {
					//find the next nodes and check if we can move to any of them while remaining in range of the sensor
					HashMap<Location,Integer> deviation = current.get(current.size()-1).getValue0().Reachable(new ArrayList<Location>());
					ArrayList<Location> deviationNodes = new ArrayList<Location>(deviation.keySet());
					Boolean oneStep = false;
					for (Location n: deviationNodes) {
						if (findDistance(n.getLocation(), end.getLocation()) < tolerance) {
							current.add(new Pair<Location, Integer>(n, deviation.get(n)));
							oneStep = true;
							break;
						}
					}
					//if we can't then choose any next point, move there and move back
					if (!oneStep) {
						current.add(new Pair<Location, Integer>(deviationNodes.get(0), deviation.get(deviationNodes.get(0))));
						current.add(current.get(0));
					}
				}
				return current;
			}
			//otherwise get the list of next possible points, use them to create the next possible branches and add them all to the branches list
			HashMap<Location, Integer> available = current.get(current.size()-1).getValue0().Reachable(visited);
			ArrayList<Location> nodes = new ArrayList<Location>(available.keySet());
			//remember to remove the branch we just expanded
			branches.remove(current);
			for (Location n: nodes) {
				if (visited.contains(n)) {
					continue;
				}
				ArrayList<Pair<Location,Integer>> temp = (ArrayList<Pair<Location, Integer>>) current.clone();
				temp.add(new Pair<Location,Integer>(n, available.get(n)));
				branches.add(temp);
			}
			//sort the new branches based on their expected cost
			branches.sort(comparator);
			//add the node we just expanded to the visited list
			visited.add(current.get(current.size()-1).getValue0());
		}
		
	}
	
	/**
	 * 
	 * @param goal The desired destination Node
	 * @return The exact number of expected steps if the Drone could move to the goal in a straight line as a Double
	 */
	public Double getHeuristic(Location goal) {
		return findDistance(this.location, goal.getLocation())/Drone.STEPSIZE;
	}
	
}



