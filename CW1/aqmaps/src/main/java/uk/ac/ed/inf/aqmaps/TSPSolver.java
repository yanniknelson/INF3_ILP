package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;

interface TSPSolver {
	ArrayList<Sensor> solve(ArrayList<Sensor> sensors, Location start);
}
