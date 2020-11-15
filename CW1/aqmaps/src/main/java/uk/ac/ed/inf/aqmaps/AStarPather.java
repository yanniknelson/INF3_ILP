package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import org.javatuples.Pair;

import com.mapbox.geojson.Point;

public class AStarPather implements Pather {

	public AStarPather() {
		// TODO Auto-generated constructor stub
	}

	@Override
	/**
	 * Performs an A* search to find the shortest path (in terms of steps) to the passed in Node (or Node subclass).
	 * 
	 * @param end The desired destination point.
	 * @return A list of pairs Nodes (or Node subclass) and angles representing the locations visited at each step and the angle between them.
	 */
	public ArrayList<Pair<Location, Integer>> path(Location start, Location end, Double tolerance) {
		//Create a Comparator that will compare possible paths and return an order based on the total expected cost to get to the end Point
		AStarNodeComparison comparator = new AStarNodeComparison(end, this);
		//List of all current possible paths (called branches due to the tree like nature of the search)
		ArrayList<ArrayList<Pair<Location, Integer>>> branches = new ArrayList<>();
		//Create the first node (tree node) in the search tree containing just the current Node (or Node subclass)
		ArrayList<Pair<Location,Integer>> base = new ArrayList<Pair<Location,Integer>>();
		base.add(new Pair<Location,Integer>(start, -1));
		//Add the first (tree) node to the branches list
		branches.add(base);
		//initialise the visited list to and empty ArrayList
		ArrayList<Location> visited = new ArrayList<>();
		//Iteratively expand upon the current best paths, then ordering the new branches and repeating until the destination is reached
		while (true) {
			//get the current best path
			ArrayList<Pair<Location, Integer>> current = branches.get(0);
			//if the current best path is within 0.0002 degrees of the destination we've reached the destination so return the path
			if (start.findDistance(current.get(current.size()-1).getValue0().getLocation(), end.getLocation()) < tolerance ) {
				//if the current path consists of only one node then the drone hasn't moved, this would be an invalid step
				if (current.size() == 1) {
					//find the next nodes and check if we can move to any of them while remaining in range of the sensor
					HashMap<Location,Integer> deviation = current.get(current.size()-1).getValue0().Reachable(new ArrayList<Location>());
					ArrayList<Location> deviationNodes = new ArrayList<Location>(deviation.keySet());
					Boolean oneStep = false;
					for (Location n: deviationNodes) {
						if (start.findDistance(n.getLocation(), end.getLocation()) < tolerance) {
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

	@Override
	public Double findDistance(Point p1, Point p2) {
		// TODO Auto-generated method stub
		return Math.sqrt(Math.pow(p1.longitude() - p2.longitude(),2) + Math.pow(p1.latitude() - p2.latitude(),2));
	}

}

/**
 * Custom comparator for comparing paths based on the total expected cost of said paths
 * @author Yannik Nelson
 *
 * @param <T>
 */
class AStarNodeComparison implements Comparator<ArrayList<Pair<Location, Integer>>>  {
	
	Location goal;
	Pather p;
	/**
	 * 
	 * @param goal The desired destination Node. This is required to calculate the cost and cannot be passed in at time of comparison
	 */
	AStarNodeComparison(Location goal, Pather p){
		this.goal = goal;
		this.p = p;
	}
	
	/**
	 * Returns the ordering of the paths based off of their total expected costs
	 */
	public int compare(ArrayList<Pair<Location, Integer>> a, ArrayList<Pair<Location, Integer>> b) {
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
	
	Double getHeuristic(Location a){
		return p.findDistance(a.getLocation(), this.goal.getLocation())/Drone.STEPSIZE;
	}
}
