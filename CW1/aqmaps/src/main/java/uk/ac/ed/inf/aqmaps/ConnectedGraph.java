package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Arrays;

class ConnectedGraph {
	
	private Node startNode;
	private ArrayList<Sensor> nodes;
	
	ConnectedGraph(Node startNode, ArrayList<Sensor> nodes) {
		this.startNode = startNode;
		this.nodes = nodes;
		this.createConnections();
	}

	private void createConnections() {
		Integer i = 0;
		for (Sensor n: nodes) {
			System.out.print("From ");
			System.out.print(i);
			System.out.println(" ");
			System.out.println(n.getLocation());
			n.createConnections(this.nodes);
			n.addConnection(startNode);
			i++;
		}
	}
}
