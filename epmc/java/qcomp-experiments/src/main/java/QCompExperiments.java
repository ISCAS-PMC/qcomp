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
public class QCompExperiments {
	private static void parseIndexFile(String indexFile, StringBuilder sbJani, StringBuilder sbPrism) throws Exception {
		System.out.println("Processing " + indexFile);
		List<String> constants = new LinkedList<String>();
		JsonReader reader = Json.createReader(new FileReader(indexFile));
		JsonObject json = reader.readObject();
		reader.close();
		
		JsonArray jaParameters = json.getJsonArray("parameters");
		for (JsonValue jvParam : jaParameters) {
			JsonObject joParam = (JsonObject) jvParam;
			if ("open".equals(joParam.getString("kind")))
				constants.add(joParam.getString("name"));
		}

		JsonArray jaFiles = json.getJsonArray("files");
		for (JsonValue jvFile : jaFiles) {
			String file = null;
			JsonObject joFile = (JsonObject) jvFile;
			String sFile = null;
			String sOrigFileModel = null;
			String sOrigFileProperty = null;
			file = joFile.getString("file");
			if (file.endsWith(".jani"))
				sFile = file;
			JsonValue jvOrigFile = joFile.get("original-file");
			if (jvOrigFile instanceof JsonString) {
				file = ((JsonString) jvOrigFile).getString();
				if (file.endsWith(".prism") || file.endsWith(".nm") || file.endsWith(".pm"))
					sOrigFileModel = file;
			} else if (jvOrigFile instanceof JsonArray) {
				for (JsonValue jvOrigFileIt : (JsonArray) jvOrigFile) {
					if (jvOrigFileIt instanceof JsonString) {
						file = ((JsonString) jvOrigFileIt).getString();
						if (file.endsWith(".prism") || file.endsWith(".nm") || file.endsWith(".pm"))
							sOrigFileModel = file;
						if (file.endsWith(".props") || file.endsWith(".prctl"))
							sOrigFileProperty = file;
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
					if (sFile != null)
						sFile = sFile.replaceAll("\"", "\"\"");
					String sConstants = sbConstants.toString().replaceAll("\"", "\"\"");
					if (sFile != null) {
						sFile = sFile.replaceAll("\"", "\"\"");
						sbJani.append("\n\"")
							.append(sFile)
							.append("\",\"")
							.append(sConstants)
							.append("\"");
					}
					if (sOrigFileModel != null) {
						sOrigFileModel = sOrigFileModel.replaceAll("\"", "\"\"");
						if (sOrigFileProperty != null)
							sOrigFileProperty = sOrigFileProperty.replaceAll("\"", "\"\"");
						sbPrism.append("\n\"")
							.append(sOrigFileModel)
							.append("\",\"")
							.append(sOrigFileProperty)
							.append("\",\"")
							.append(sConstants)
							.append("\"");
					}
				}
			}
		}
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
		StringBuilder sbJani = new StringBuilder();
		StringBuilder sbPrism = new StringBuilder();
		for (String index : args) {
			parseIndexFile(index, sbJani, sbPrism);
		}
		try(BufferedWriter jani = new BufferedWriter(new FileWriter("benchmarks_jani"))) {
			jani.write("model,const");
			jani.write(sbJani.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try(BufferedWriter prism = new BufferedWriter(new FileWriter("benchmarks_prism"))) {
			prism.write("model,property,const");
			prism.write(sbPrism.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
