package com.amazonaws.lambda.demo.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class MetricsUtil {

	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Get Header Templates from a widget array
	 * 
	 * @param widgets
	 * @return
	 */
	public Map<String, String> getHeaderTemplates(ArrayNode widgets) {
		Map<String, String> headerList = new HashMap<>();

		Iterator<JsonNode> widgetsItr = widgets.iterator();
		while (widgetsItr.hasNext()) {
			ObjectNode properties = (ObjectNode) ((JsonNode) widgetsItr.next()).get("properties");
			ArrayNode metrics = (ArrayNode) properties.get("metrics");
			if (metrics != null) {
				ArrayNode metricLine = (ArrayNode) metrics.get(0);
				TextNode heading = (TextNode) metricLine.get(1);
				String metricLineString = "", headingString = "";
				if (metricLine != null) {
					metricLineString = metricLine.toString();
				}
				if (heading != null) {
					headingString = heading.asText();
				}

				headerList.put(headingString, metricLineString);
			}
		}

		return headerList;
	}

	/**
	 * Create a new Metric for a given widget template
	 * 
	 * @param template
	 * @param instances
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public ArrayNode getUpdatedMetric(String template, Ec2Instances instances)
			throws JsonProcessingException, IOException {
		ArrayNode returnMetric = mapper.createArrayNode();
		Map<String, List<String>> instancesMap = instances.getInstances();
		for (String label : instancesMap.keySet()) {
			String labelCode = label.split("-")[1];
			List<String> instanceList = instancesMap.get(label);
			for (String id : instanceList) {
				JsonNode metric = mapper.readTree(String.format(template, id, labelCode));
				returnMetric.add(metric);
			}
		}

		return returnMetric;
	}

}
