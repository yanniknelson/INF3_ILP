package uk.ac.ed.inf.aqmaps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.javatuples.Pair;

/**
 * 
 * Implements the logger interface for the format specified in the documentation
 * 
 * @author Yannik Nelson
 * @see Logger
 */
class StepLogger implements Logger {
	
	//Initialise the steps text variable and line count
	String outputLog = "";
	Integer linenum = 1;
	
	Pather pather;
	
	StepLogger (Pather p){
		this.pather = p;
	}
	
	public Integer LogSteps(ArrayList<Pair<Location, Integer>> route, Sensor sensor) {
		//run through the list of steps (ignoring the first Location as we care about moving from one Location to the next and the first Location has no previous one
		for (Integer j = 1; j < route.size(); j++) {
			//get the current Location and the locateion we were at prior
			Location from = route.get(j-1).getValue0();
			Location to = route.get(j).getValue0();
			//add the location data and angle travelled to the the outputlog
			String ln = Integer.toString(linenum) + "," + Double.toString(from.longitude()) + "," + Double.toString(from.latitude()) + "," + Integer.toString(route.get(j).getValue1()) + "," + Double.toString(to.longitude()) + "," + Double.toString(to.latitude()) + ",";
			//if we're not at the end of the route then we don't need to ceck for a Sensor location and simply add 'null'
			if (j == route.size()-1) {
				//if we're at the end by the sensor has no what3words value then that sensor is the start position and we have returned, add 'null' to the line, add the line to the outputLog and return success
				//note this also means we don't have a linebreak on the last line of the log
				if (sensor.getWhat3words().equals("")) {
					ln += "null";
					outputLog += ln;
					linenum++;
					return -1;
				}
				//if we're at the end and we're in range of the desired sensor then add its what3words to the line otherwise add 'null'
				if (pather.findDistance(route.get(route.size()-1).getValue0(), sensor) < 0.0002) {
					ln += sensor.getWhat3words();
				} else {
					ln += "null";
				}
			} else {
				ln += "null";
			}
			//add a line break to the line and add it the the full log, increment the line count
			ln += "\n";
			outputLog += ln;
			linenum++;
			// if the next line would be step 151 we've reached our step limit, return the index of the last valid step in this route
			if (linenum == 151) {
				return j;
			}
		}
		//otherwise return success
		return -1;
	}

	public void OutputLogFile(String filePath) {
		//print the number total number of steps
		System.out.println(String.format("Final Step Count: %s", linenum-1));
		//setup the path to save the file relative to where the jar is being run
		Path pathToOutput = Paths.get(System.getProperty("user.dir"), filePath);
		try {
			//attempt to write the file
			Files.write(pathToOutput, outputLog.getBytes());
			System.out.println("Log saved Successfully");
		} catch (Exception e) {
			System.out.println("Failed to write Log to the files");
		}
	}

}
