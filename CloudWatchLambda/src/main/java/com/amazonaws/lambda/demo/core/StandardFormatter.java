package com.amazonaws.lambda.demo.core;

public class StandardFormatter implements Formatter {

	private static Formatter formatter = new StandardFormatter();
	
	private StandardFormatter() {
	}

	public static Formatter getInstance() {
		return formatter;
	}

	@Override
	public String format(String template, String... params)  {
		synchronized (template) {
			String removedType = template.replace("\"".concat(Formatter.STANDARD).concat("\"").concat(","), "");
			return String.format(removedType, params[0], params[1]);
		}
	}
}
