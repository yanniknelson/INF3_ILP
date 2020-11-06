package uk.ac.ed.inf.heatmap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


/**
 * 
 * @author Yannik Nelson
 * @version 1.0
 * 
 */
public class heatmap {
	
	/**
	 * The number of expected integers per row
	 */
	private static final Integer GRIDWIDTH = 10;
	
	/**
	 * The number of expected rows of integers
	 */
	private static final Integer GRIDHEIGHT = 10;
	
	/**
	 * The predetermined highest latitude value
	 */
	private static final Double UPPERBOUND = 55.946233;
	
	/**
	 * The predetermined lowest latitude value
	 */
	private static final Double LOWERBOUND = 55.942617;
	
	/**
	 * The predetermined leftmost longitude value
	 */
	private static final Double LEFTBOUND = -3.192473;
	
	/**
	 * The predetermined rightmost longitude value
	 */
	private static final Double RIGHTBOUND = -3.184319;
	
	/**
	 * Turns a comma and newline delimited string of numbers into a 2D array of integers
	 * <p>
	 * This function takes in a string consisting of integers separated by commas and new lines that
	 * represents a 2 dimensional array of integers with each newline separating a row in the array
	 * and each comma separating an entry (integer) in that row. The resulting array of integers is 
	 * then returned.
	 * <p>
	 * The size of this array must be known beforehand and provided. It is assumed that the array 
	 * represented in the string has the same number of integers in each row. 
	 * 
	 * @param data command and newline delimited string of numbers
	 * @param height number of lines in the string
	 * @param width number of integers in each line of the string
	 * @return 2D array of Integers corresponding to the data
	 */
	private static Integer[][] breakDownData(String data, Integer height, Integer width) {
		//Create the array and two counter variables for iterating through the data
		Integer returnArray[][] = new Integer[height][width];
		Integer i = 0;
		Integer j = 0;
		//split the input string on newline or carriage return and throw away empty elements
		//run through the resulting array of string representing each row of the array
		for (String row: data.split("\\r?\\n",-1)) {
			//resent the inner counter, split the current row on commas (again throwing away empty elements)
			//run through the resulting array of strings representing the elements, parsing them to Integers
			//and storing them in the returnArray
			j = 0;
			for (String elem: row.split(",",-1)) {
				try {
					returnArray[i][j] = Integer.parseInt(elem);
				} catch (Exception e) {
					System.out.println("Oops, Check your file is formatted correctly, remember to separate integers with commans and rows with newlines. Make sure there're no commas at the end of rows or newline at the end of the file.");
					System.exit(0);
				}
				j++;
			}
			i++;
		}
		return returnArray;
	}
	
	/**
	 * Convenience method to quickly create a Polygon in the shape of a rectangle.
	 * 
	 * @param topLeft A point corresponding to the latitude and longitude of the top left corner of the desired rectangle
	 * @param horizontalSidelength The longitude difference of the two sides of the rectangle
	 * @param verticalSidelength The latitude difference of the top and bottom of the rectangle
	 * @return GeoJSON Polygon in the shape of the desired rectangle
	 */
	private static Polygon generateRectangle(Point topLeft, Double horizontalSidelength, Double verticalSidelength) {
		//the list of Points corners stores the points to be used in the Polygon
		List<Point> corners = new ArrayList<>();
		//the list of lists of Points outerCorners is used as the Polygon.fromLngLats function expects a list of lists of Points
		List<List<Point>> outerCorners = new ArrayList<>();
		corners.add(topLeft);
		corners.add(Point.fromLngLat(topLeft.longitude() - horizontalSidelength, topLeft.latitude()));
		corners.add(Point.fromLngLat(topLeft.longitude() - horizontalSidelength, topLeft.latitude() - verticalSidelength));
		corners.add(Point.fromLngLat(topLeft.longitude(), topLeft.latitude() - verticalSidelength));
		//Must end with the same point as it started with
		corners.add(topLeft);
		outerCorners.add(corners);
		return Polygon.fromLngLats(outerCorners);
	}
	
	/**
	 * Convenience method to get desired colour string from predicted value
	 * 
	 * @param value predicted value from Integer Array
	 * @return String containing the hexadecimal code of the desired colour
	 */
	private static String getColour(Integer value) {
		if (value < 32) {
			return "#00ff00";
		} else if (value < 64) {
			return "#40ff00";
		} else if (value < 96) {
			return "#80ff00";
		} else if (value < 128) {
			return "#c0ff00";
		} else if (value < 160) {
			return "#ffc000";
		} else if (value < 192) {
			return "#ff8000";
		} else if (value < 224) {
			return "#ff4000";
		} else if (value < 256) {
			return "#ff0000";
		} else {
			//this case should never occur
			//default to black if it does.
			//the provided documentation did not include a case for the value 256 only ever for less than 256
			return "#000000";
		}
	}
	
	public static void main( String[] args ) {
		String fileData;
		Integer predictions[][];
		Path filepath;
		//attempt to create the path to the predictions file
		//this should only fail if args[0] doesn't exit at which point it will request the name and the program will exit
		try {
			filepath = Paths.get(System.getProperty("user.dir"), args[0]);
	        System.out.println(filepath.toString());
		} catch (Exception e) {
			System.out.println("Please pass in the name of the predictions file");
	        return;
		}
		//attempt to read the file
		//if it fails then the desired file is not in the expected directory and so the path is incorrect
		//an error message will be displayed and the program will exit
	    try {
	    	fileData = Files.readString(filepath);
    		System.out.println("Read File succesfully");
    		//removes all spaces for convenience
    		fileData = fileData.replaceAll(" ", "");
	    } catch (Exception e) {
	    	System.out.println("Oops couldn't load that file please make sure the file is in the:");
	        System.out.print(System.getProperty("user.dir"));
	        System.out.println(" directory.");
	        return;
	    }
	    
	    //Breakdown the data from the predicted data file into a 2D array of integers we can work with
	    predictions = breakDownData(fileData, GRIDWIDTH, GRIDHEIGHT);
	    for (Integer[] r: predictions) {
	    	for (Integer v: r) {
	    		System.out.print(String.format("%d, ", v));
	    	}
	    	System.out.println();
	    }
	    
	    List<Feature> gridList = new ArrayList<>();
	    
	    //calculate the desired height and width of the rectangle
	    Double verticalStep = (UPPERBOUND - LOWERBOUND)/GRIDHEIGHT;
	    Double horizontalStep = (LEFTBOUND - RIGHTBOUND)/GRIDWIDTH;
		
	    //run through the array of predictions creating a Feature containing a rectangle Polygon for each and setting it's properties to the desired values
	    //then add the Feature to the gridList
		for (Integer i = 0; i < GRIDHEIGHT; i++) {
			for (Integer j = 0; j < GRIDWIDTH; j++) {
				String colour = getColour(predictions[i][j]);
				//the top left corner of each rectangle is given by (leftBound - (horizontalStep * j), upperBound - (verticalStep * i)) and the height and width of the rectangles stay constant
				Feature rectangle = Feature.fromGeometry(generateRectangle(Point.fromLngLat(LEFTBOUND - (horizontalStep * j), UPPERBOUND - (verticalStep * i)), horizontalStep, verticalStep));
				rectangle.addStringProperty("rgb-string", colour);
				rectangle.addStringProperty("fill", colour);
				rectangle.addNumberProperty("fill-opacity", 0.75);
				gridList.add(rectangle);
			}
		}
		
		//convert the gridList to a FeatureCollection, create the path to the heatmap.geojson and write to it the JSON string of grid
		//in the case of an exception output an error message and exit the program
		FeatureCollection grid = FeatureCollection.fromFeatures(gridList);
		Path pathToOutput = Paths.get(System.getProperty("user.dir"), "heatmap.geojson");
		try {
			Files.write(pathToOutput, grid.toJson().getBytes());
			System.out.println("File saved Successfully");
		} catch (Exception e) {
			System.out.println("Failed to write to the file heatmap.geojson");
		}
    }
}
