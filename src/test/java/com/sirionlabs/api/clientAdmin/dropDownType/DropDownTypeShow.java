package com.sirionlabs.api.clientAdmin.dropDownType;

import com.sirionlabs.helper.api.ApiHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class DropDownTypeShow {

	public static String getAPIPath(String fieldId) {
		return "/dropdowntype/show/" + fieldId;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static String showFromResponse(String responseShow)
	{
		Document html = Jsoup.parse(responseShow);
		String fieldType = html.getElementsByClass("form-container").get(0).child(0).child(1).child(1).childNodes().get(0).toString();

		return fieldType;

	}
	public static String EnableViewFromResponse(String responseShow)
	{
		Document html =Jsoup.parse(responseShow);
		String EnableListView = html.getElementsByClass("form-container").get(0).child(0).child(1).child(4).childNodes().get(0).toString();

		return EnableListView;
	}

	public static String SizeFromResponse(String responseShow)
	{
		Document html = Jsoup.parse(responseShow);
		String Size =  html.getElementsByClass("form-container").get(0).child(0).child(0).child(4).childNodes().get(0).toString();

		return Size;
	}

}