package com.sirionlabs.helper.api;

import com.sirionlabs.helper.testRail.TestRailBase;
import org.testng.annotations.BeforeSuite;

public class TestAPIBase  extends  TestRailBase {

	public static APIExecutor executor;

	public static void setUp() {
		executor = new APIExecutor();

	}
}