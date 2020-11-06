package uk.ac.ed.inf.aqmaps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Precision;
import org.javatuples.Pair;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Node <T extends Node> {
	
	protected Point location;
	HashMap<T,Connection> connections = new HashMap<T,Connection>();

	Node(Point location) {
		this.location = location;
	}

	/**
	 * @return the location
	 */
	Point getLocation() {
		return location;
	}
	
	 static boolean onSegment(Point a, Point b, Point c)  
	    { 
	        if (b.longitude() <= Math.max(a.longitude(), c.longitude()) && 
	            b.longitude() >= Math.min(a.longitude(), c.longitude()) && 
	            b.latitude() <= Math.max(a.latitude(), c.latitude()) && 
	            b.latitude() >= Math.min(a.latitude(), c.latitude())) 
	        { 
	            return true; 
	        } 
	        return false; 
	    } 
	  

	    static int orientation(Point a, Point b, Point c)  
	    { 
	        Double val = (b.latitude() - a.latitude()) * (c.longitude() - b.longitude()) - (c.latitude() - b.latitude()) * (b.longitude() - a.longitude()); 
	        return val.compareTo(0.0);
	    } 
	  
	    // The function that returns true if  
	    // line segment 'ab' and 'cd' intersect. 
	    static boolean intersect(Point a, Point b, Point c, Point d)  
	    { 
	        // Find the four orientations needed for  
	        // general and special cases 
	        int o1 = orientation(a, b, c); 
	        int o2 = orientation(a, b, d); 
	        int o3 = orientation(c, d, a); 
	        int o4 = orientation(c, d, b); 
	        
	        
	  
	        // General case 
	        if (o1 != o2 && o3 != o4) 
	        { 
	            return true; 
	        } 
	  
	        // Special Cases 
	        // a, b and c are colinear and 
	        // c lies on segment ab 
	        if (o1 == 0 && onSegment(a, c, b))  
	        { 
	            return true; 
	        } 
	  
	        // a, b and c are colinear and 
	        // d lies on segment ab 
	        if (o2 == 0 && onSegment(a, d, b))  
	        { 
	            return true; 
	        } 
	  
	        // c, d and a are colinear and 
	        // a lies on segment cd 
	        if (o3 == 0 && onSegment(c, a, d)) 
	        { 
	            return true; 
	        } 
	  
	        // c, d and b are colinear and 
	        // b lies on segment cd 
	        if (o4 == 0 && onSegment(c, b, d)) 
	        { 
	            return true; 
	        } 
	  
	        // Doesn't fall in any of the above cases 
	        return false;  
	    } 
	
//	static Boolean intersect(Point a, Point b, Point c, Point d) {
//		
//		return false;
//	}    
	    
	void createConnections(ArrayList<T> nodes) {
		Integer i = 0;
		for (T dest: nodes) {
			if (dest != this){
				System.out.print(String.format("To Sensor %d at ", i));
				System.out.println(dest.getLocation());
				this.connections.put(dest, new Connection(this, dest));
				System.out.println("connection made");
			}
			i++;
		}
		System.out.println("done");
	}
	
	HashMap<Node, Integer> Reachable(Integer roundedAngle, ArrayList<T> visited){
		HashMap<Node, Integer> nextPoints = new HashMap<>();
		outerloop:
		for (Integer i = 0 ; i < 360; i += 5) {
			Double lon = this.location.longitude() + AQMapper.STEPSIZE * Math.cos(Math.toRadians(i));
			if (lon > AQMapper.RIGHTBOUND || lon < AQMapper.LEFTBOUND) {
				continue;
			}
			Double lat = this.location.latitude() + AQMapper.STEPSIZE * Math.sin(Math.toRadians(i));
			if (lat > AQMapper.UPPERBOUND || lat < AQMapper.LOWERBOUND) {
				continue;
			}
			
			Point loc = Point.fromLngLat(lon,lat);
			
			
			
			for (ArrayList<Point> b: AQMapper.boundingBoxes) {
				if ((lon < b.get(2).longitude() && lon > b.get(0).longitude() && lat < b.get(0).latitude() && lat > b.get(2).latitude()) || intersect(location, loc, b.get(0), b.get(1)) || intersect(location, loc, b.get(1), b.get(2)) || intersect(location, loc, b.get(2), b.get(3)) || intersect(location, loc, b.get(3), b.get(4))) {
					//need to do more detailed check
					ArrayList<Point> outline = AQMapper.noFlyZones.get(b);
					for (Integer j = 1; j < outline.size(); j++) {
						if (intersect(location, loc, outline.get(j-1), outline.get(j))) {
							continue outerloop;
						}
					}
				}
			}
			
			for (T p: visited) {
				if (AQMapper.findDistance(p.getLocation(), loc) < 0.0003) {
					continue outerloop;
				}
			}
			
			nextPoints.put(new Node(loc),i);
		}
		return nextPoints;
	}
	
	public ArrayList<Pair<T, Integer>> AStar(T end) {
		Boolean print = false;
		AStarNodeComparison comparator = new AStarNodeComparison(end);
		ArrayList<ArrayList<Pair<T, Integer>>> branches = new ArrayList<>();
		ArrayList<Pair<T,Integer>> base = new ArrayList<Pair<T,Integer>>();
		base.add(new Pair<T,Integer>((T) this, -1));
		branches.add(base);
		if (print) {System.out.println(branches.size());}
		ArrayList<Node> visited = new ArrayList<>();
		Integer i = 0;
		while (true) {
			ArrayList<Pair<T, Integer>> current = branches.get(0);
			if (print) {System.out.println(String.format("g(n) = %d, f(n) = %f", current.size()-1, current.size()-1 + current.get(current.size()-1).getValue0().getHeuristic(end)));
			System.out.println(AQMapper.findDistance(current.get(current.size()-1).getValue0().getLocation(), end.getLocation()));}
//			this.path = current;
			if (AQMapper.findDistance(current.get(current.size()-1).getValue0().getLocation(), end.getLocation()) < 0.0002) {
//				this.path = current;
//				if (print) {System.out.println("found");
//				System.out.println(AQMapper.findDistance(current.get(current.size()-1).getValue0().getLocation(), this.end.getLocation()));
//				System.out.println(0.0002);}
				return current;
			}
			HashMap<T, Integer> available = current.get(current.size()-1).getValue0().Reachable(AQMapper.nearestAngle(current.get(current.size()-1).getValue0().getLocation(), end.getLocation()), visited);
			ArrayList<T> nodes = new ArrayList<T>(available.keySet());
//			System.out.println(nodes);
			if (print) {System.out.println(String.format("%d available", nodes.size()));}
			branches.remove(current);
			for (T n: nodes) {
				if (visited.contains(n)) {
					continue;
				}
				if (print) {System.out.print(String.format("%d, ", available.get(n)));
				System.out.print(AQMapper.findDistance(n.getLocation(), this.getLocation()));
				System.out.print(", ");}
				ArrayList<Pair<T,Integer>> temp = (ArrayList<Pair<T, Integer>>) current.clone();
				temp.add(new Pair<T,Integer>(n, available.get(n)));
				branches.add(temp);
			}
			if (print) {System.out.print("\n");
	        System.out.println(AQMapper.findDistance(Point.fromLngLat(-3.1877479,55.9442954), this.getLocation()));}
	            
			branches.sort(comparator);
			visited.add(current.get(current.size()-1).getValue0());
			if (print) {System.out.println(String.format("Visited: %d", visited.size()));
			System.out.println(String.format("Branches: %d", branches.size()));}
			
//			ArrayList<Feature> testList = (ArrayList<Feature>) AQMapper.connectionToFeatures(this);
//			for (ArrayList<Point> corners: AQMapper.boundingBoxes) {
//				List<List<Point>> outerCorners = new ArrayList<>();
//				outerCorners.add(corners);
//				testList.add(Feature.fromGeometry(Polygon.fromLngLats(outerCorners)));
//			}
//			FeatureCollection testlin = FeatureCollection.fromFeatures(testList);
//			Path pathToOutput = Paths.get(System.getProperty("user.dir"), "test.geojson");
//			try {
//				Files.write(pathToOutput, testlin.toJson().getBytes());
////				System.out.println("File saved Successfully");
//			} catch (Exception e) {
////				System.out.println("Failed to write to the file heatmap.geojson");
//			}

		}
		
	}
	
	Double getHeuristic(T goal) {
		return AQMapper.findDistance(this.location, goal.getLocation())/AQMapper.STEPSIZE;
	}
	
	void addConnection(T node) {
		if (node != this) {
			this.connections.put(node, new Connection(this, node));
		}
	}
	
	public String toString() {
		return this.location.toString();
	}
}
