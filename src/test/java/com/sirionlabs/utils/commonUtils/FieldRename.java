package com.sirionlabs.utils.commonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FieldRename {

	private final static Logger logger = LoggerFactory.getLogger(FieldRename.class);

	public static boolean isWordInRussian(String word) {


		//logger.info("Word Verifying is : [{}]",word);
		String specialChars = "/*!@#$%^&*()\"{}_[]|\\?/<>,.- ";
		Boolean flag = false;
		List<Character> nonRussianChar = new ArrayList<>();
		for (int i = 0; i < word.trim().length(); i++) {
			if (!Character.UnicodeBlock.of(word.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
				flag = true;
				nonRussianChar.add(word.charAt(i));
				if (specialChars.contains(Character.toString(word.charAt(i)))) {
					logger.info("Special character present in the string field");
					flag = false;
					continue;
				} else {
					continue;
				}
			}
			if (flag) {
				logger.error("Given [String {} , contains non-russian characters : {} in it ]", word, nonRussianChar);
				return false;
			}
		}
		return true;
	}
}
