package com.amazonaws.lambda.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.amazonaws.lambda.demo.core.Ec2Instances;
import com.amazonaws.lambda.demo.core.MetricsUtil;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.DeleteDashboardsRequest;
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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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
	private static AmazonAutoScaling aas = AmazonAutoScalingClientBuilder.defaultClient();
	private ClassLoader classLoader = null;

	@Override
	public String handleRequest(Object input, Context context) {
		classLoader = getClass().getClassLoader();
		loadProperties();
		PutDashboardRequest putDashboardRequest = new PutDashboardRequest();
		putDashboardRequest.setDashboardName(properties.getProperty("aws.cloudwatch.dashboard.name"));
		JsonNode reloadedDashbord = getUpdatedDashBoard();
		putDashboardRequest.setDashboardBody(reloadedDashbord.toString());

		acw.putDashboard(putDashboardRequest);

		return "Success";
	}

	private List<String> getAwsAsgs() {
		List<String> ret = new ArrayList<>();

		List<AutoScalingGroup> asGroups = new ArrayList<>();
		String nextToken = null;
		while (true) {
			DescribeAutoScalingGroupsResult result = aas
					.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withNextToken(nextToken));
			asGroups.addAll(result.getAutoScalingGroups());
			nextToken = result.getNextToken();
			if (nextToken == null)
				break;
		}
		for (AutoScalingGroup autoScalingGroup : asGroups) {
			ret.add(autoScalingGroup.getAutoScalingGroupName());
		}
		return ret;
	}

	private Ec2Instances getLatestEc2InstanceIds(List<String> asgs) {

		boolean done = false;

		DescribeInstancesRequest request = new DescribeInstancesRequest();

		// System.out.printf("ASGS : %s",asg.toString());

		Filter filters = new Filter("tag:aws:autoscaling:groupName", asgs);
		Ec2Instances ec2Instances = new Ec2Instances();
		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request.withFilters(filters));

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					System.out.printf("%n Status : %s , %s", instance.getState().getName(), instance.getStateReason());
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

			System.out.printf("%n Instances : %s", ec2Instances.getInstances().toString());
			request.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) {
				done = true;
			}
		}
		return ec2Instances;
	}

	private JsonNode getUpdatedDashBoard() {
		Ec2Instances instances = getLatestEc2InstanceIds(getAwsAsgs());
		JsonNode rootNode = loadTemplate();
		ArrayNode widgets = (ArrayNode) rootNode.get("widgets");
		MetricsUtil metricsUtil = new MetricsUtil();
		Map<String, String> templates = metricsUtil.getHeaderTemplates(widgets);
		Map<String, ArrayNode> updatedMetricList = new HashMap<String, ArrayNode>();
		for (String heading : templates.keySet()) {
			try {
				updatedMetricList.put(heading, metricsUtil.getUpdatedMetric(templates.get(heading), instances));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// System.out.printf("%n Updated Matrics : %s",updatedMetricList.toString());
		// add the new widgets to the dashboard
		for (JsonNode widget : widgets) {
			ObjectNode properties = (ObjectNode) widget.get("properties");

			if (properties != null) {
				ArrayNode metrics = (ArrayNode) properties.get("metrics");
				if (metrics != null) {
					ArrayNode metricLine = (ArrayNode) metrics.get(0);
					if (metricLine != null && metricLine.size() > MetricsUtil.MATRIC_TABLE_HEADER_INDEX) {
						TextNode heading = (TextNode) metricLine.get(MetricsUtil.MATRIC_TABLE_HEADER_INDEX);
						if (heading != null) {
							properties.remove("metrics");
							properties.set("metrics", updatedMetricList.get(heading.asText()));
						}
					}
				}
			}
		}
		return rootNode;
	}

	private JsonNode loadTemplate() {
		JsonNode rootNode = null;
		try {
			InputStream stream = classLoader.getResourceAsStream(TEMPLATE);
			rootNode = new ObjectMapper().readTree(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rootNode;
	}

	private void loadProperties() {
		try {
			InputStream stream = classLoader.getResourceAsStream(RESOURCES_FILE);
			properties.load(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
