package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import org.javatuples.Pair;

import com.mapbox.geojson.Point;

interface Pather {
	ArrayList<Pair<Location, Integer>> path(Location start, Location end, Double tollerance);
	Double findDistance(Point p1, Point p2);
//	Double getHeuristic(Location start, Location goal);
}
