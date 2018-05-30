package com.amazonaws.lambda.demo.core;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FormatterTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		String testString = "[ \"{BEANSTALK}\",\"AWS/ElasticBeanstalk\", \"InstanceHealth\", \"InstanceId\", \"%s\", \"EnvironmentName\", \"%s\", { \"label\": \"%s\" } ]";
		String targetString = "[ \"AWS/ElasticBeanstalk\", \"InstanceHealth\", \"InstanceId\", \"1\", \"EnvironmentName\", \"2\", { \"label\": \"3\" } ]";
		Formatter formatter = BeanstalkFormatter.getInstance();
		String target = formatter.format(testString, "1","3","2");
		assertEquals(targetString, target);
		
		testString = " [ \"{STANDARD}\",\"AWS/EC2\", \"CPUUtilization\", \"InstanceId\", \"%s\", { \"label\": \"%s\" } ]";
		targetString = " [ \"AWS/EC2\", \"CPUUtilization\", \"InstanceId\", \"1\", { \"label\": \"2\" } ]";
		formatter = StandardFormatter.getInstance();
		target = formatter.format(testString, "1","2","3");
		assertEquals(targetString, target);
	}

}
