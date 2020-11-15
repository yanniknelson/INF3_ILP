package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import org.javatuples.Pair;

interface Logger {
	Integer LogSteps(ArrayList<Pair<Location, Integer>> route, Sensor sensor);
	void OutputLogFile(String filePath);
}
