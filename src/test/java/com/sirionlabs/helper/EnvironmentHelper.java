package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentHelper {
	private final static Logger logger = LoggerFactory.getLogger(EnvironmentHelper.class);

	static String oldEnvironmentFileName;
	static String newEnvironmentFileName;

	public static void loggingOnEnvironment(String environment) {
		try {
			 newEnvironmentFileName = environment;
			oldEnvironmentFileName = ConfigureEnvironment.environment;

			ConfigureEnvironment.configureProperties(newEnvironmentFileName, true);

			Check checkObj = new Check();
			//Login on different env.
			checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));

		} catch (Exception e) {
			logger.error("Exception occurred while logging into alternate environment :{}. Error = {}", environment,e.getMessage());
			e.printStackTrace();
		}
	}

	public static void setEnvironmentProperties(String environment) {
		try {
			newEnvironmentFileName = environment;
			oldEnvironmentFileName = ConfigureEnvironment.environment;

			ConfigureEnvironment.configureProperties(newEnvironmentFileName, true);

		} catch (Exception e) {
			logger.error("Exception occurred while setting alternate environment :{}. Error = {}", environment,e.getMessage());
			e.printStackTrace();
		}
	}
}
