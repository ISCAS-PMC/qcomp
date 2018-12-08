import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 *
 * @author ori
 */
public class QCompReferences {
	private static String parseIndexFile(String indexFile) throws Exception {
		System.out.println("Processing " + indexFile);
		List<String> constants = new LinkedList<String>();
		List<String> properties = new LinkedList<String>();
		Map<String, String> propertyType = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		JsonReader reader = Json.createReader(new FileReader(indexFile));
		JsonObject json = reader.readObject();
		reader.close();
		
		JsonArray jaParameters = json.getJsonArray("parameters");
		for (JsonValue jvParam : jaParameters) {
			JsonObject joParam = (JsonObject) jvParam;
			if ("open".equals(joParam.getString("kind")))
				constants.add(joParam.getString("name"));
		}

		JsonArray jaProps = json.getJsonArray("properties");
		for (JsonValue jvProp : jaProps) {
			JsonObject joProp = (JsonObject) jvProp;
			String prop = joProp.getString("name");
			properties.add(prop);
			propertyType.put(prop, joProp.getString("type", "unknown"));
		}

		JsonArray jaFiles = json.getJsonArray("files");
		for (JsonValue jvFile : jaFiles) {
			JsonObject joFile = (JsonObject) jvFile;
			String sFile = joFile.getString("file");
			String sOrigFile = null;
			JsonValue jvOrigFile = joFile.get("original-file");
			if (jvOrigFile instanceof JsonString)
				sOrigFile = ((JsonString) jvOrigFile).getString();
			else if (jvOrigFile instanceof JsonArray) {
				for (JsonValue jvOrigFileIt : (JsonArray) jvOrigFile) {
					if (jvOrigFileIt instanceof JsonString) {
						String file = ((JsonString) jvOrigFileIt).getString();
						if (file.endsWith(".prism") || file.endsWith(".nm") || file.endsWith(".pm"))
							sOrigFile = file;
					}
				}
			}
			JsonArray jaOpenParams = joFile.getJsonArray("open-parameter-values");
			if (jaOpenParams != null) {
				for (JsonValue jvOpenParam : jaOpenParams) {
					JsonObject joOpenParam = (JsonObject) jvOpenParam;
					Map<String, String> mOpenParams = new HashMap<String, String>();
					JsonArray jaValues = joOpenParam.getJsonArray("values");
					if (jaValues != null) {
						for (JsonValue jvValue : jaValues) {
							JsonObject joValue = (JsonObject) jvValue;
							mOpenParams.put(joValue.getString("name"), getAsString(joValue.get("value")));
						}
					}
					Map<String, String> mResults = new HashMap<String, String>();
					JsonArray jaResults = joOpenParam.getJsonArray("results");
					if (jaResults != null) {
						for (JsonValue jvResult : jaResults) {
							JsonObject joResult = (JsonObject) jvResult;
							JsonValue jvValue = joResult.get("value");
							if (jvValue == null)
								mResults.put(joResult.getString("property"), "");
							else if (jvValue instanceof JsonObject) {
								JsonObject joValue = (JsonObject) jvValue;
								JsonValue jvApprox = joValue.get("approx");
								if (jvApprox != null) {
									mResults.put(joResult.getString("property"), getAsString(jvApprox));
								} else {
									String sLower = getAsString(joValue.get("lower"));
									String sUpper = getAsString(joValue.get("upper"));
									mResults.put(joResult.getString("property"), "[" + sLower + "," + sUpper + "]");
								}
							}
							else 
								mResults.put(joResult.getString("property"), getAsString(jvValue));
						}
					}
					StringBuilder sbConstants = new StringBuilder();
					boolean notFirst = false;
					for (String constant : constants) {
						if (notFirst)
							sbConstants.append("#");
						sbConstants
							.append(constant)
							.append("=")
							.append(mOpenParams.get(constant));
						notFirst = true;
					}
					sFile = sFile.replaceAll("\"", "\"\"");
					if (sOrigFile != null) {
						sOrigFile = sOrigFile.replaceAll("\"", "\"\"");
					}
					String sConstants = sbConstants.toString().replaceAll("\"", "\"\"");
					for (String sProperty : properties) {
						String sEscapedProperty = sProperty.replaceAll("\"", "\"\"");
						String sEscapedType = propertyType.get(sProperty).replaceAll("\"", "\"\"");
						String sEscapedResult = mResults.getOrDefault(sProperty, "").replaceAll("\"", "\"\"");
						sb.append("\n\"")
							.append(sFile)
							.append("\",\"")
							.append(sConstants)
							.append("\",\"")
							.append(sEscapedProperty)
							.append("\",\"")
							.append(sEscapedType)
							.append("\",\"")
							.append(sEscapedResult)
							.append("\"");
						if (sOrigFile != null) {
							sb.append("\n\"")
								.append(sOrigFile)
								.append("\",\"")
								.append(sConstants)
								.append("\",\"")
								.append(sEscapedProperty)
								.append("\",\"")
								.append(sEscapedType)
								.append("\",\"")
								.append(sEscapedResult)
								.append("\"");
						}
					}
				}
			}
		}
		return sb.toString();
	}
	
	private static String getAsString(JsonValue value) {
		if (value instanceof JsonString)
			return ((JsonString) value).getString();
		if (value instanceof JsonNumber)
			return value.toString();
		else if (value.equals(JsonValue.TRUE)) 
			return "true";
		else if (value.equals(JsonValue.FALSE)) 
			return "false";
		return null;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws Exception {
		try(BufferedWriter ref = new BufferedWriter(new FileWriter("references.csv"))) {
			ref.write("model,const,property,type,ref_value");
			for (String index : args) {
				ref.write(parseIndexFile(index));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
