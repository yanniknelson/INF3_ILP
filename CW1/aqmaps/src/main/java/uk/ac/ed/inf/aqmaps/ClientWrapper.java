package uk.ac.ed.inf.aqmaps;

import java.io.IOException;

import com.mapbox.geojson.Point;

interface ClientWrapper {
	String getNoFly() throws IOException, InterruptedException;
	String getData(String day,String month, String year) throws IOException, InterruptedException;
	Point PointFromWords(String Input) throws IOException, InterruptedException;
}
