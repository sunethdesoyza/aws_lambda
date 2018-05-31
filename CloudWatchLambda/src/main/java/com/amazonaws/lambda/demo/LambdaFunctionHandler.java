package com.amazonaws.lambda.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.lambda.demo.core.Ec2Instances;
import com.amazonaws.lambda.demo.core.MetricsUtil;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.PutDashboardRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {

	private static String TEMPLATE = "cloud_watch_template.json";
	private static String RESOURCES_FILE = "dashboard.properties";
	private Properties properties = new Properties();
	private static AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
	private static AmazonCloudWatch acw = AmazonCloudWatchClientBuilder.defaultClient();
	private ClassLoader classLoader = null;
	
	//Constants for Template
	public static final String AWS_CW_TAG_MATRICS="metrics";
	public static final String AWS_CW_TAG_PROPERTIES="properties";
	public static final String AWS_CW_TAG_WIDGETS="widgets";

	@Override
	public String handleRequest(Object input, Context context) {
		classLoader = getClass().getClassLoader();
		
		JsonNode reloadedDashbord = null;
		try {
			loadProperties();
			reloadedDashbord = getUpdatedDashBoard();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// Need to handle using AWS Step functions
			e.printStackTrace();
		}
		PutDashboardRequest putDashboardRequest = new PutDashboardRequest();
		putDashboardRequest.setDashboardName(properties.getProperty("aws.cloudwatch.dashboard.name"));
		putDashboardRequest.setDashboardBody(reloadedDashbord.toString());
		acw.putDashboard(putDashboardRequest);

		return "Success";
	}

	private Ec2Instances getLatestEc2InstanceIds() {
		boolean done = false;
		DescribeInstancesRequest request = new DescribeInstancesRequest();

		Ec2Instances ec2Instances = new Ec2Instances();
		List<String> filters = new ArrayList<String>();
		filters.add("true");
		Filter filter = new Filter("tag:".concat(properties.getProperty("concert.monitoring.tag")), filters);

		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request.withFilters(Arrays.asList(filter)));

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					if (instance.getState().getName().equalsIgnoreCase("running")) {
						String name = "";
						if (instance.getTags() != null) {
							for (Tag tag : instance.getTags()) {
								if (tag.getKey().equalsIgnoreCase("name")) {
									name = tag.getValue();
								}
							}
						}
						ec2Instances.addToList(name, instance.getInstanceId());
					}
				}
			}

			request.setNextToken(response.getNextToken());
			if (response.getNextToken() == null) {
				done = true;
			}
		}
		return ec2Instances;
	}

	private JsonNode getUpdatedDashBoard() throws IOException {
		Ec2Instances instances = getLatestEc2InstanceIds();
		JsonNode rootNode = loadTemplate();
		ArrayNode widgets = (ArrayNode) rootNode.get(AWS_CW_TAG_WIDGETS);
		
		MetricsUtil metricsUtil = new MetricsUtil();
		Map<String, String> templates = metricsUtil.getHeaderTemplates(widgets);
		Map<String, ArrayNode> updatedMetricList = new HashMap<String, ArrayNode>();
		
		for (String heading : templates.keySet()) {
				updatedMetricList.put(heading, metricsUtil.getUpdatedMetric(templates.get(heading), instances));
		}
		
		for (JsonNode widget : widgets) {
			ObjectNode properties = (ObjectNode) widget.get(AWS_CW_TAG_PROPERTIES);

			if (properties != null) {
				ArrayNode metrics = (ArrayNode) properties.get(AWS_CW_TAG_MATRICS);
				if (metrics != null) {
					ArrayNode metricLine = (ArrayNode) metrics.get(0);
					if (metricLine != null && metricLine.size() > MetricsUtil.MATRIC_TABLE_HEADER_INDEX) {
						TextNode heading = (TextNode) metricLine.get(MetricsUtil.MATRIC_TABLE_HEADER_INDEX);
						if (heading != null) {
							properties.remove(AWS_CW_TAG_MATRICS);
							properties.set(AWS_CW_TAG_MATRICS, updatedMetricList.get(heading.asText()));
						}
					}
				}
			}
		}
		return rootNode;
	}

	private JsonNode loadTemplate() throws IOException {
		JsonNode rootNode = null;
		InputStream stream = classLoader.getResourceAsStream(TEMPLATE);
		rootNode = new ObjectMapper().readTree(stream);
		return rootNode;
	}

	private void loadProperties() throws IOException {
		InputStream stream = classLoader.getResourceAsStream(RESOURCES_FILE);
		properties.load(stream);
	}

}
