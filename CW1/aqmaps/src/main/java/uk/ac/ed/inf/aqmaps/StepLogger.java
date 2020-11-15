package uk.ac.ed.inf.aqmaps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.javatuples.Pair;

class StepLogger implements Logger {
	
	String outputLog = "";
	Integer linenum = 1;
	public Integer LogSteps(ArrayList<Pair<Location, Integer>> route, Sensor sensor) {
		for (Integer j = 1; j < route.size(); j++) {
			Location from = route.get(j-1).getValue0();
			Location to = route.get(j).getValue0();
			String ln = Integer.toString(linenum) + "," + Double.toString(from.longitude()) + "," + Double.toString(from.latitude()) + "," + Integer.toString(route.get(j).getValue1()) + "," + Double.toString(to.longitude()) + "," + Double.toString(to.latitude()) + ",";
			if (j == route.size()-1) {
				if (sensor.getWhat3words().equals("")) {
					ln += "null";
					outputLog += ln;
					linenum++;
					return -1;
				}
				if (Drone.pather.findDistance(route.get(route.size()-1).getValue0(), sensor) < 0.0002) {
					ln += sensor.getWhat3words();
				} else {
					ln += "null";
				}
			} else {
				ln += "null";
			}
			ln += "\n";
			outputLog += ln;
			linenum++;
			if (linenum == 151) {
				return j;
			}
		}
		return -1;
	}

	public void OutputLogFile(String filePath) {
		System.out.println(String.format("Final Step Count: %s", linenum));
		Path pathToOutput = Paths.get(System.getProperty("user.dir"), filePath);
		try {
			Files.write(pathToOutput, outputLog.getBytes());
			System.out.println("Log saved Successfully");
		} catch (Exception e) {
			System.out.println("Failed to write Log to the files");
		}
	}

}
