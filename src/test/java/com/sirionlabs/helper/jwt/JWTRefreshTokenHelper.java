package com.sirionlabs.helper.jwt;

import com.sirionlabs.api.commonAPI.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.TimerTask;

public class JWTRefreshTokenHelper extends TimerTask {

	private final static Logger logger = LoggerFactory.getLogger(JWTRefreshTokenHelper.class);
	private Check checkObj = new Check();

	@Override
	public void run() {
		logger.info("Hitting Check API at Time: [{}] using Credentials of Last Logged in User. [{}/{}]", new Date(), Check.lastLoggedInUserName, Check.lastLoggedInUserPassword);
		checkObj.hitCheck(Check.lastLoggedInUserName, Check.lastLoggedInUserPassword);
	}
}