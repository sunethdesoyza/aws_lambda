package com.amazonaws.lambda.demo.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class MetricsUtilTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetHeaderTemplateFromProperty() throws JsonProcessingException, IOException {
		String propertyString = "{" + 
				"                \"view\": \"singleValue\"," + 
				"                \"metrics\": [" + 
				"                    [ \"AWS/EC2\", \"CPUUtilization\", \"InstanceId\", \"%s\", { \"label\": \"%s\" } ]" + 
				"                ]," + 
				"                \"region\": \"eu-west-1\"," + 
				"                \"period\": 300," + 
				"                \"title\": \"\"," + 
				"                \"stacked\": true" + 
				"            }";
		
		JsonNode rooNode = loadTemplate();
		Map<String, String> tempates = new MetricsUtil().getHeaderTemplates((ArrayNode)rooNode.get("widgets"));
		assertTrue(tempates.containsKey("CPUUtilization"));
		assertFalse(tempates.containsKey("AWS/EC2"));
	}
	
	@Test
	public void testGetUpdatedMetric() throws JsonProcessingException, IOException {
		Ec2Instances instanceList  = new Ec2Instances();

		instanceList.addToList("pool-Aler-1I8D0FU3MQ6CR", "i-08cd233294171d486");
		instanceList.addToList("pool-Repo-6JKE1XB0J1AA", "i-07e14f649f855a675");
		instanceList.addToList("pool-VsdE-KH9J6KWEGL2S", "i-038abb42c7fb864e7");
		instanceList.addToList("pool-Regi-1RS9CGGN0E646", "i-0f8dc161584ea522d");
		instanceList.addToList("pool-Aler-1I8D0FU3MQ6CR", "i-06758a21be951ba8a");
		instanceList.addToList("pool-Aler-1I8D0FU3MQ6CR", "i-05131ba5aaa03c421");
		instanceList.addToList("pool-Auth-118U1OC6MHIYW", "i-08b52bcc675d24d2e");
		instanceList.addToList("pool-Pref-1E0M5CYNFBHIB", "i-05581f3293ff196f6");
		instanceList.addToList("pool-LBAp-12GHK0P93DK63", "i-0273f62d55099da7d");
		
		String propertyString = "{" + 
				"                \"view\": \"singleValue\"," + 
				"                \"metrics\": [" + 
				"                    [ \"AWS/EC2\", \"CPUUtilization\", \"InstanceId\", \"%s\", { \"label\": \"%s\" } ]" + 
				"                ]," + 
				"                \"region\": \"eu-west-1\"," + 
				"                \"period\": 300," + 
				"                \"title\": \"\"," + 
				"                \"stacked\": true" + 
				"            }";
		
		JsonNode rooNode = loadTemplate();
		MetricsUtil util = new MetricsUtil();
		Map<String, String> headers  = util.getHeaderTemplates((ArrayNode)rooNode.get("widgets"));
		Map<String, ArrayNode> updatedMetricList = new HashMap<String, ArrayNode>();
		for(String header: headers.keySet()) {
			updatedMetricList.put(header,util.getUpdatedMetric(headers.get(header), instanceList));
		}
		
		assertEquals(16, updatedMetricList.size());
		
		for (String header : updatedMetricList.keySet()) {
			Iterator<JsonNode> metricsItr = updatedMetricList.get(header).iterator();
			Map<String, String> processedMetric = new HashMap<>();
			while (metricsItr.hasNext() ) {
				ArrayNode metricLine = (ArrayNode)metricsItr.next();
				//ObjectNode label = (ObjectNode)metricLine.get(metricLine.size()-1);
				//processedMetric.put(((TextNode)metricLine.get(3)).asText(), label.get("label").asText());
				
			}
//			
//			assertEquals(6, processedMetric.size());
//			assertEquals("Registry", processedMetric.get("323452"));
//			assertEquals("Alert", processedMetric.get("123456"));
//			assertEquals("Swagger", processedMetric.get("623455"));
//			assertEquals("Alert", processedMetric.get("223451"));
//			assertEquals("Registry", processedMetric.get("423453"));
//			assertEquals("Alert", processedMetric.get("523454"));
		}
		
	}

	private JsonNode loadTemplate() {
		byte[] jsonData;
		JsonNode rootNode = null;
		try {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			InputStream stream = classloader.getResourceAsStream("cloud_watch_template.json");
			rootNode = new ObjectMapper().readTree(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rootNode;
	}
}
