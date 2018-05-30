package com.amazonaws.lambda.demo.core;

public class BeanstalkFormatter implements Formatter {

	private static Formatter formatter = new BeanstalkFormatter();
	
	private BeanstalkFormatter() {
	}
	
	public static Formatter getInstance() {
		return formatter;
	}

	@Override
	public String format(String template, String... params)  {
		synchronized (template) {
			String removedType = template.replace("\"".concat(Formatter.BEANSTALK).concat("\"").concat(","), "");
			return String.format(removedType, params[0], params[2], params[1]);
		}
	}

}
