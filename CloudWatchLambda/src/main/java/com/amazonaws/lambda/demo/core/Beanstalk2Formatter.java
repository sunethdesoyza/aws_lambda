package com.amazonaws.lambda.demo.core;

public class Beanstalk2Formatter implements Formatter {

	private static Formatter formatter = new Beanstalk2Formatter();
	
	private Beanstalk2Formatter() {
	}
	
	public static Formatter getInstance() {
		return formatter;
	}

	@Override
	public String format(String template, String... params)  {
		synchronized (template) {
			String removedType = template.replace("\"".concat(Formatter.BEANSTALK2).concat("\"").concat(","), "");
			return String.format(removedType, params[2], params[1]);
		}
	}

}
