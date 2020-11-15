package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;

interface TSPSolver {
	ArrayList<Sensor> solve(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> sensors);
	Integer getCost(HashMap<Sensor, HashMap<Sensor, Integer>> connectionMatrix, ArrayList<Sensor> path);
}
