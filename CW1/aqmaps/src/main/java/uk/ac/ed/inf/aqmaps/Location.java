package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;

import org.javatuples.Pair;

import com.mapbox.geojson.Point;

interface Location {
	Point getLocation();
}
