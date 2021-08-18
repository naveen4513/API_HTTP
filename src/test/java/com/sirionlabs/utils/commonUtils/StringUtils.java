package com.sirionlabs.utils.commonUtils;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    private final static Logger logger = LoggerFactory.getLogger(StringUtils.class);

    //Removes substring starting from beginChar till endChar
    public static String removeSubString(String originalString, char beginChar, char endChar) {
        return removeSubString(originalString, originalString.indexOf(beginChar), originalString.indexOf(endChar));
    }

    public static String removeSubString(String originalString, int beginIndex, int endIndex) {
        StringBuffer outputString = new StringBuffer(originalString);
        outputString = outputString.replace(beginIndex, endIndex + 1, "");
        return outputString.toString();
    }

    public static String removeAllSubString(String originalString, char beginChar, char endChar) {
        String allSubStrings[] = originalString.split(String.valueOf(endChar));
        String outputString = "";

        for (String subString : allSubStrings) {
            outputString += StringUtils.removeSubString(subString, subString.indexOf(beginChar), subString.length() - 1);
        }
        return outputString;
    }

    public static boolean isNumericRangeValue(String originalValue) {
        boolean rangeValue = false;
        String rangeDelimiter = StringUtils.getNumericRangeDelimiter(originalValue);

        if (rangeDelimiter != null) {
            String values[] = originalValue.split(Pattern.quote(rangeDelimiter));

            if (rangeDelimiter.equalsIgnoreCase("-")) {
                if (!values[0].trim().equalsIgnoreCase("") && NumberUtils.isParsable(values[0].trim()) && !values[1].trim().equalsIgnoreCase("")
                        && NumberUtils.isParsable(values[1].trim()))
                    rangeValue = true;
            } else {
                if (values[0].trim().equalsIgnoreCase("") && !values[1].trim().equalsIgnoreCase("")
                        && NumberUtils.isParsable(values[1]))
                    rangeValue = true;
            }
        }
        return rangeValue;
    }

    public static String getNumericRangeDelimiter(String value) {
        String rangeDelimiter = null;

        if (value.contains("-"))
            rangeDelimiter = "-";
        else if (value.contains(">"))
            rangeDelimiter = ">";
        else if (value.contains("<"))
            rangeDelimiter = "<";

        return rangeDelimiter;
    }

    public static String strSubstitutor(String templateString, Map<String, ?> valuemap) {
        String resolvedString = "";
        StrSubstitutor sub = new StrSubstitutor(valuemap);
        resolvedString = sub.replace(templateString);
        return resolvedString;
    }

    public static boolean matchRussianCharacters(String valueOne, String valueTwo) {
        Pattern pattern = Pattern.compile("(?iu)\\b(" + Pattern.quote(valueOne) + ")\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(valueTwo);

        return matcher.find();
    }
}
