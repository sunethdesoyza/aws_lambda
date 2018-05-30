package com.amazonaws.lambda.demo.core;

public interface Formatter {
	
	public static String STANDARD = "{STANDARD}";
	public static String BEANSTALK = "{BEANSTALK}";
	public static String BEANSTALK2 = "{BEANSTALK2}";

	public String format(String template, String ...params);
}
