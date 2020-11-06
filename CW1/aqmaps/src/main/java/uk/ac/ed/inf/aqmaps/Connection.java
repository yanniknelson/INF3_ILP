package uk.ac.ed.inf.aqmaps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.javatuples.Pair;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

class Connection <T extends Node> {

	private T start;
	private T end;
	private Integer cost;
	private ArrayList<Pair<T, Integer>> path;
	
	Connection(T start, T end) {
		super();
		this.start = start;
		this.end = end;
		this.calculateCost();
	}
	
	private void calculateCost() {
		Point startLoc = this.start.getLocation();
		Point endLoc = this.end.getLocation();
		Double distance = AQMapper.findDistance(startLoc, endLoc);
		this.path = this.start.AStar(this.end);
		this.cost = this.path.size() - 1;
	}
	
//	public Integer AStar() {
//		Boolean print = false;
//		AStarNodeComparison comparator = new AStarNodeComparison(this.end);
//		ArrayList<ArrayList<Pair<T, Integer>>> branches = new ArrayList<>();
//		ArrayList<Pair<T,Integer>> base = new ArrayList<Pair<T,Integer>>();
//		base.add(new Pair<T,Integer>(this.start, -1));
//		branches.add(base);
//		if (print) {System.out.println(branches.size());}
//		ArrayList<Node> visited = new ArrayList<>();
//		Integer i = 0;
//		while (true) {
//			ArrayList<Pair<T, Integer>> current = branches.get(0);
//			if (print) {System.out.println(String.format("g(n) = %d, f(n) = %f", current.size()-1, current.size()-1 + current.get(current.size()-1).getValue0().getHeuristic(this.end)));
//			System.out.println(AQMapper.findDistance(current.get(current.size()-1).getValue0().getLocation(), this.end.getLocation()));}
//			this.path = current;
//			if (AQMapper.findDistance(current.get(current.size()-1).getValue0().getLocation(), this.end.getLocation()) < 0.0002) {
//				this.path = current;
////				if (print) {System.out.println("found");
////				System.out.println(AQMapper.findDistance(current.get(current.size()-1).getValue0().getLocation(), this.end.getLocation()));
////				System.out.println(0.0002);}
//				return current.size();
//			}
//			HashMap<T, Integer> available = current.get(current.size()-1).getValue0().Reachable(AQMapper.nearestAngle(current.get(current.size()-1).getValue0().getLocation(), this.end.getLocation()), visited);
//			ArrayList<T> nodes = new ArrayList<T>(available.keySet());
////			System.out.println(nodes);
//			if (print) {System.out.println(String.format("%d available", nodes.size()));}
//			branches.remove(current);
//			for (T n: nodes) {
//				if (visited.contains(n)) {
//					continue;
//				}
//				if (print) {System.out.print(String.format("%d, ", available.get(n)));
//				System.out.print(AQMapper.findDistance(n.getLocation(), this.start.getLocation()));
//				System.out.print(", ");}
//				ArrayList<Pair<T,Integer>> temp = (ArrayList<Pair<T, Integer>>) current.clone();
//				temp.add(new Pair<T,Integer>(n, available.get(n)));
//				branches.add(temp);
//			}
//			if (print) {System.out.print("\n");
//	        System.out.println(AQMapper.findDistance(Point.fromLngLat(-3.1877479,55.9442954), this.start.getLocation()));}
//	            
//			branches.sort(comparator);
//			visited.add(current.get(current.size()-1).getValue0());
//			if (print) {System.out.println(String.format("Visited: %d", visited.size()));
//			System.out.println(String.format("Branches: %d", branches.size()));}
//			
//			ArrayList<Feature> testList = (ArrayList<Feature>) AQMapper.connectionToFeatures(this);
//			for (ArrayList<Point> corners: AQMapper.boundingBoxes) {
//				List<List<Point>> outerCorners = new ArrayList<>();
//				outerCorners.add(corners);
//				testList.add(Feature.fromGeometry(Polygon.fromLngLats(outerCorners)));
//			}
////			FeatureCollection testlin = FeatureCollection.fromFeatures(testList);
////			Path pathToOutput = Paths.get(System.getProperty("user.dir"), "test.geojson");
////			try {
////				Files.write(pathToOutput, testlin.toJson().getBytes());
//////				System.out.println("File saved Successfully");
////			} catch (Exception e) {
//////				System.out.println("Failed to write to the file heatmap.geojson");
////			}
//
//		}
//		
//	}
	
	ArrayList<Pair<T, Integer>> getPath() {
		return path;
	}
	
	/**
	 * @return the start node
	 */
	T getStart() {
		return start;
	}
	/**
	 * @return the end node
	 */
	T getEnd() {
		return end;
	}
	
	/**
	 * @return the number of Moves required to move between the start and end nodes
	 */
	Integer getCost() {
		return cost;
	}

}

class AStarNodeComparison <T extends Node> implements Comparator<ArrayList<Pair<T, Integer>>>  {
	
	T goal;
	
	AStarNodeComparison(T goal){
		this.goal = goal;
	}
	
	public int compare(ArrayList<Pair<T, Integer>> a, ArrayList<Pair<T, Integer>> b) {
		//I scale the heuristics here to emphasise their difference
		//I do this as the restriction of only moving at angles that divide by 5 means taking a step sometimes adds more to the f value than is lost in the heuristic from the new end node
		//This means it has to go back and check the old values that are now better, to fix this I scale the heuristic by the biggest number i can while keeping the heuristic mostly consistent
		Double ah = a.get(a.size()-1).getValue0().getHeuristic(this.goal);
		Double bh = b.get(b.size()-1).getValue0().getHeuristic(this.goal);
		//To try and keep the heuristic consistent I assume the realistic length will be the ceiling of the heuristic (the distance to the point divided by the step size)
		//I then divide those by their actual heuristic and take the minimum of the two values to find the amount i can scale the heuristics to keep them consistent (based on the above assumption) 
		Double scale = Math.min(Math.ceil(ah)/ah, Math.ceil(bh)/bh);
		Double fa = a.size() - 1 + ah*scale;
		return fa.compareTo(b.size() - 1 + bh*scale);
	}
}
