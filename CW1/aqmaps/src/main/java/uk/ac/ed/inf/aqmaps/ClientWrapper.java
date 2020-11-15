package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.javatuples.Pair;

interface ClientWrapper {
	Pair<ArrayList<ArrayList<Location>>, HashMap<ArrayList<Location>, ArrayList<Location>>> getNoFly() throws IOException, InterruptedException;
	ArrayList<Sensor> getDestinations(String day,String month, String year) throws IOException, InterruptedException;
	Location LocationFromWords(String Input) throws IOException, InterruptedException;
	Sensor getSensorData(Sensor sensor);
}
