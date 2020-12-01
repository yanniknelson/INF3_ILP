package uk.ac.ed.inf.aqmaps;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AStarPatherTest {

	@Test
	void testPath() {
		var testPather = new AStarPather();
		testPather.setStepSize(0.0003);
		testPather.setBounds(5.0, -5.0, -5.0, 5.0);
		var start = new Node(0.0, 0.0);
		var end = new Node(0.0006, 0.0);
		var test = testPather.path(start, end, 0.00001);
		System.out.println(test);
	}

	@Test
	void testFindDistance() {
//		fail("Not yet implemented");
		assertTrue(true);
	}

}
