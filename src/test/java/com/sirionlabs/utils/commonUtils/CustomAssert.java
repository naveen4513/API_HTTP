package com.sirionlabs.utils.commonUtils;

import org.testng.asserts.Assertion;
import org.testng.asserts.IAssert;
import org.testng.collections.Maps;

import java.util.Map;

public class CustomAssert extends Assertion {

	private Map<AssertionError, IAssert> m_errors = Maps.newLinkedHashMap();

	@Override
	public void executeAssert(IAssert a) {
		try {
			a.doAssert();
		} catch (AssertionError ex) {
			m_errors.put(ex, a);
		}
	}

	public void assertAll() {
		if (!m_errors.isEmpty()) {
			StringBuilder sb = new StringBuilder("The following asserts failed:\n");
			boolean first = true;
			for (Map.Entry<AssertionError, IAssert> ae : m_errors.entrySet()) {
				if (first) {
					first = false;
				} else {
					sb.append("\n");
				}
				sb.append(ae.getValue().getMessage());
			}
			throw new AssertionError(sb.toString());
		}
	}

	public String getAllAssertionMessages() {
		if (!m_errors.isEmpty()) {
			StringBuilder sb = new StringBuilder("The following asserts failed:\n");
			boolean first = true;
			for (Map.Entry<AssertionError, IAssert> ae : m_errors.entrySet()) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(ae.getValue().getMessage());
			}

			String str = sb.toString();
			str = str.replaceAll("\n", "\\r\\n");

			return str;
		}

		return "";
	}


}