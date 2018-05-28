package com.amazonaws.lambda.demo.core;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Ec2InstancesTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetInstances() {
		Ec2Instances ins = new Ec2Instances();
		String inputs[] = {"a","b","c","d","e","f"};
		
		ins.addToList(inputs[0], "1");
		SortedMap<String,List<String>> map = (SortedMap)ins.getInstances();
		for (String key : map.keySet()) {
			assertEquals(inputs[0], key);
		}
		
		ins.addToList(inputs[3], "1");
		ins.addToList(inputs[1], "1");
		ins.addToList(inputs[2], "1");
		ins.addToList(inputs[1], "1");
		ins.addToList(inputs[2], "1");
		
		int i = 0;
		for (String key : map.keySet()) {
			assertEquals(inputs[i], key);
			i++;
		}
		assertEquals(2, ins.getInstances().get(inputs[2]).size());
		assertEquals(1, ins.getInstances().get(inputs[0]).size());
		assertEquals(2, ins.getInstances().get(inputs[1]).size());
		assertEquals(1, ins.getInstances().get(inputs[3]).size());
	}

}
