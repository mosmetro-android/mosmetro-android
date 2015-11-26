package ru.thedrhax.mosmetro;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.List;
import java.util.ArrayList;

public class HTMLFormParser {
	private List<String> fields;
	
	// Patterns
	private static final Pattern pInputs = Pattern.compile("<input [^>]*>");
	private static final Pattern pFieldName = Pattern.compile("name=\"(.*?)\"");
	private static final Pattern pFieldValue = Pattern.compile("value=\"(.*?)\"");
	
	// Parse form fields from HTML
	public HTMLFormParser parse (String content) {
		fields = new ArrayList<String>();
		
		Matcher mInputs = pInputs.matcher(content);
		while (mInputs.find()) {
			Matcher mFieldName = pFieldName.matcher(mInputs.group(0));
			mFieldName.find();
			
			Matcher mFieldValue = pFieldValue.matcher(mInputs.group(0));
			mFieldValue.find();

			try {		
				fields.add(mFieldName.group(1) + "=" + mFieldValue.group(1));
			} catch (IllegalStateException ex) {
				System.out.println(ex.toString());
			}
		}
				
		return this;
	}
	
	// Get parsed fields as String
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		
		for (int i = 0; i < fields.size(); i++) {
			buffer.append(fields.get(i));
			if (i != fields.size() - 1) buffer.append("&");
		}
		
		return buffer.toString();
	}
}

