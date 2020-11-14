package uk.ac.ed.inf.aqmaps;

interface Sensor extends Location{
	String getWhat3words();
	Double getBattery();
	String getReading();
}
