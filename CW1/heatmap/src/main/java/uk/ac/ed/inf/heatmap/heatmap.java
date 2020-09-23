package uk.ac.ed.inf.heatmap;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class heatmap {
	
	//Required constants
	private static Integer gridWidth = 10;
	private static Integer gridHeight = 10;
	//latitude Bounds
	private static Double upperBound = 55.946233;
	private static Double lowerBound = 55.942617;
	//longitude Bounds
	private static Double leftBound = -3.192473;
	private static Double rightBound = -3.184319;
	
	private static Integer[][] breakDownData(String data, Integer height, Integer width){
		Integer returnArray[][] = new Integer[height][width];
		Integer i = 0;
		for (String row: data.split("\\r?\\n",-1)) {
			Integer j = 0;
			for (String elem: row.split(",",-1)) {
				returnArray[i][j] = Integer.parseInt(elem);
				j++;
			}
			i++;
		}
		return returnArray;
	}
	
	private static Polygon generateSquare(Point topLeft, Double horizontalSidelength, Double verticalSidelength) {
		List<Point> corners = new ArrayList<>();
		List<List<Point>> outerCorners = new ArrayList<>();
		corners.add(topLeft);
		corners.add(Point.fromLngLat(topLeft.longitude() - horizontalSidelength, topLeft.latitude()));
		corners.add(Point.fromLngLat(topLeft.longitude() - horizontalSidelength, topLeft.latitude() - verticalSidelength));
		corners.add(Point.fromLngLat(topLeft.longitude(), topLeft.latitude() - verticalSidelength));
		corners.add(topLeft);
		outerCorners.add(corners);
		return Polygon.fromLngLats(outerCorners);
	}
	
	private static String getColour(Integer value) {
		if (value < 32) {
			return "#00ff00";
		} else if (value < 64) {
			return "#40ff00";
		} else if (value < 96) {
			return "#40ff00";
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
			return "#000000";
		}
	}
	
	public static void main( String[] args ) {
		String fileData;
		Integer predictions[][];
		Path filepath;
		try {
			filepath = Paths.get(System.getProperty("user.dir"), args[0]);
	        System.out.println(filepath.toString());
		} catch (Exception e) {
			System.out.println("Please pass in the name of the predictions file");
	        return;
		}
	    try {
	    	fileData = Files.readString(filepath);
    		System.out.println("Read File succesfully");
    		fileData = fileData.replaceAll(" ", "");
	    } catch (Exception e) {
	    	System.out.println("Oops couldn't load that file please make sure the file is in the:");
	        System.out.print(System.getProperty("user.dir"));
	        System.out.println(" directory.");
	        return ;
	    }
	    
	    //Breakdown the data from the predicted data file into a 2D array of integers we can work with
	    predictions = breakDownData(fileData, gridWidth, gridHeight);
	    
	    List<Feature> gridList = new ArrayList<>();
	    
	    Double verticalStep = (upperBound - lowerBound)/gridHeight;
	    Double horizontalStep = (leftBound - rightBound)/gridWidth;
		
		for (Integer i = 0; i < gridHeight; i++) {
			for (Integer j = 0; j < gridWidth; j++) {
				String colour = getColour(predictions[i][j]);
				Feature Square = Feature.fromGeometry(generateSquare(Point.fromLngLat(leftBound - (horizontalStep * j), upperBound - (verticalStep * i)),horizontalStep, verticalStep));
				Square.addStringProperty("rgb-string", colour);
				Square.addStringProperty("fill", colour);
				Square.addNumberProperty("fill-opacity", 0.75);
				gridList.add(Square);
			}
		}
		
		FeatureCollection grid = FeatureCollection.fromFeatures(gridList);
		Path pathToOutput = Paths.get(System.getProperty("user.dir"), "heatmap.geojson");
		try {
			Files.write(pathToOutput, grid.toJson().getBytes());
		} catch (Exception e) {
			
		}
    }
}
