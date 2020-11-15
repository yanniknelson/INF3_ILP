package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;

interface TSPSolver {
	ArrayList<Sensor> solve(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> sensors, Location start);
	Integer getCost(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> path);
	HashMap<Sensor, HashMap<Sensor, Integer>> ConnectionMatrix(ArrayList<Sensor> data);
	ArrayList<Sensor> ACOTSP(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> sensors);
	void Two_OPT(HashMap<Sensor, HashMap<Sensor, Integer>> connectionLengths, ArrayList<Sensor> order);
}
