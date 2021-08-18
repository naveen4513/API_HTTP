package com.sirionlabs.helper.search;

import com.sirionlabs.api.search.SearchAttachment;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class SearchAttachmentHelper {

	private final static Logger logger = LoggerFactory.getLogger(SearchAttachmentHelper.class);
	private SearchAttachment searchObj = new SearchAttachment();

	public String getSearchAttachmentResponse(String queryText, int entityTypeId, int limit, int offset) {
		logger.info("Hitting Search Attachment API for EntityTypeId {}, QueryText [{}], Limit {} and Offset {}.", entityTypeId, queryText, limit, offset);
		searchObj.hitAttachment(queryText, entityTypeId, limit, offset);
		return searchObj.getAttachmentJsonStr();
	}

	public Integer getFilteredCount(String searchAttachmentResponse) {
		try {
			JSONObject jsonObj = new JSONObject(searchAttachmentResponse);
			return jsonObj.getInt("filteredCount");
		} catch (Exception e) {
			logger.error("Exception while Getting Filtered Count from Search Attachment Response. {}", e.getMessage());
			return null;
		}
	}

	public String getVersionNoFromDocumentNameValue(String documentNameValue) {
		try {
			if (documentNameValue.trim().contains("Version ")) {
				String[] temp = documentNameValue.trim().split(Pattern.quote("Version "));
				return temp[1].trim().substring(0, temp[1].trim().length() - 1);
			}

			return null;
		} catch (Exception e) {
			logger.error("Exception while Getting Version No from DocumentName Value: {}. {}", documentNameValue, e.getStackTrace());
			return null;
		}
	}
}