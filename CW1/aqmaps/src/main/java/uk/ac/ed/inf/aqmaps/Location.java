package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;

import org.javatuples.Pair;

import com.mapbox.geojson.Point;

interface Location {
	HashMap<Location, Integer> Reachable( ArrayList<Location> visited);
	ArrayList<Pair<Location, Integer>> path(Location end, Double tollerance);
	Double getHeuristic(Location goal);
	Double findDistance(Point p1, Point p2);
	Point getLocation();
}
