package org.ekstep.searchindex.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.ekstep.learning.util.ControllerUtil;
import org.ekstep.searchindex.util.HTTPUtil;
import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.graph.dac.model.Node;
import com.ilimi.kafka.util.PropertiesUtil;

/**
 * The Class LanguageEnrichmentMessageProcessor is a kafka consumer which
 * provides implementations of the core language feature extraction operations
 * defined in the IMessageProcessor along with the methods to implement content
 * enrichment from language model with additional metadata
 * 
 * @author Rashmi
 *
 */
public class LanguageEnrichmentMessageProcessor extends BaseProcessor implements IMessageProcessor {

	/** The logger. */
	private static Logger LOGGER = LogManager.getLogger(LanguageEnrichmentMessageProcessor.class.getName());

	/** The ObjectMapper */
	private static ObjectMapper mapper = new ObjectMapper();

	/** The Controller Utility */
	private static ControllerUtil util = new ControllerUtil();

	/** The constructor */
	public LanguageEnrichmentMessageProcessor() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ekstep.searchindex.processor #processMessage(java.lang.String,
	 * java.lang.String, java.io.File, java.lang.String)
	 */
	@Override
	public void processMessage(String messageData) {
		try {
			LOGGER.info("Reading from kafka consumer" + messageData);
			Map<String, Object> message = new HashMap<String, Object>();
			if (StringUtils.isNotBlank(messageData)) {
				LOGGER.debug("checking if kafka message is blank or not" + messageData);
				message = mapper.readValue(messageData, new TypeReference<Map<String, Object>>() {
				});
			}
			if (null != message) {
				LOGGER.info("checking if kafka message is null" + message);
				processMessage(message);
			}
		} catch (Exception e) {
			LOGGER.error("Error while processing kafka message", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ekstep.searchindex.processor #processMessage(java.lang.String
	 * java.lang.String, java.io.File, java.lang.String)
	 */
	@SuppressWarnings("unused")
	@Override
	public void processMessage(Map<String, Object> message) throws Exception {
		
		LOGGER.info("filtering out the kafka message" + message);
		Node node = filterMessage(message);
		
		String languageId = null;
		LOGGER.info("Checking if node contains languageCode");
		String language = getLanguage(node);
		processData(node, language);
	}

	private void processData(Node node, String languageId) throws Exception {

		LOGGER.info("Checking if languageId is null" + languageId);
		if (StringUtils.isNotBlank(languageId)) {

			LOGGER.info("checking if node contains text tag" + node.getMetadata().containsKey("text"));
			if (null != node.getMetadata().get("text")) {
				Object object = node.getMetadata().get("text");

				if (object instanceof String[]) {
					LOGGER.info("fetched object is an string array");
					String[] textArray = (String[]) object;
					String text = Arrays.toString(textArray);
					updateData(text, languageId, node);

				} else if (object instanceof String) {
					LOGGER.info("fetched object is a string");
					String text = object.toString();
					updateData(text, languageId, node);
				}
			}
		}
	}

	/**
	 * This method holds logic to make a rest API call to language actor to get
	 * complexity measures for a given text
	 * 
	 * @param languageId
	 *            The languageId
	 * 
	 * @param Text
	 *            The text to be analysed
	 * 
	 * @return result The complexity measures result
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, Object> getComplexityMeasures(String languageId, String text) throws Exception {

		LOGGER.info("getting language_api_url from properties to make rest call");
		String api_url = PropertiesUtil.getProperty("language-api-url") + "/v1/language/tools/complexityMeasures/text";
		LOGGER.info("api url to make rest api call to complexity measures api" + api_url);

		Map<String, Object> request_map = new HashMap<String, Object>();
		request_map.put("languageId", languageId);
		request_map.put("text", text);

		Request request = new Request();
		request.setRequest(request_map);
		LOGGER.info("creating request map to make rest post call" + request);

		LOGGER.info("making a post call to get complexity measures for a given text");
		String result = HTTPUtil.makePostRequest(api_url, request.toString());

		LOGGER.info("response from complexity measures api" + result);
		Map<String, Object> responseObject = mapper.readValue(result, new TypeReference<Map<String, Object>>() {
		});
		LOGGER.info("converting result string to response map" + responseObject);

		if (responseObject == null) {
			throw new Exception("Unable to get complexity measures");
		}

		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap = (Map) responseObject.get("result");

		LOGGER.info("getting resultMap from response Object" + resultMap);
		if (resultMap == null) {
			throw new Exception("Result in response is empty");
		}
		return resultMap;
	}

	/**
	 * This method holds logic to extract required fields from complexity
	 * measures response and update it as node metadata
	 * 
	 * @param node
	 *            The node
	 * 
	 * @param result
	 *            The result map
	 */
	@SuppressWarnings({ "rawtypes" })
	private void updateComplexityMeasures(Node node, Map result) {

		try {
			LOGGER.info("extracting required fields from result map" + result);
			if (null != result.get("text_complexity")) {

				LOGGER.info("Getting text_complexity map from result object" + result.containsKey("text_complexity"));
				Map text_complexity = (Map) result.get("text_complexity");

				if (null != text_complexity && !text_complexity.isEmpty()) {

					LOGGER.info("checking if text complexity map contains wordCount");
					if (text_complexity.containsKey("wordCount")) {
						if (null == text_complexity.get("wordCount")) {
							node.getMetadata().put("wordCount", null);
						} else {
							Object wordCount = text_complexity.get("wordCount");
							LOGGER.info("updating node with wordCount" + wordCount);
							updateNodeMetadata("wordCount", wordCount, node);
						}
					}

					LOGGER.info("checking if text complexity map contains syllableCount");
					if (text_complexity.containsKey("syllableCount")) {
						if (null == text_complexity.get("syllableCount")) {
							node.getMetadata().put("syllableCount", null);
						} else {
							Object syllableCount = text_complexity.get("syllableCount");
							LOGGER.info("updating node with syllableCount" + syllableCount);
							updateNodeMetadata("syllableCount", syllableCount, node);
						}
					}

					LOGGER.info("checking if text complexity map contains totalOrthoComplexity");
					if (text_complexity.containsKey("totalOrthoComplexity")) {
						if (null == text_complexity.get("totalOrthoComplexity")) {
							node.getMetadata().put("totalOrthoComplexity", null);
						} else {
							Object totalOrthoComplexity = text_complexity.get("totalOrthoComplexity");
							LOGGER.info("updating node with totalOrthoComplexity"
									+ text_complexity.get("totalOrthoComplexity"));
							updateNodeMetadata("totalOrthoComplexity", totalOrthoComplexity, node);
						}
					}

					LOGGER.info("checking if text complexity map contains totalPhonicComplexity");
					if (text_complexity.containsKey("totalPhonicComplexity")) {
						if (null == text_complexity.get("totalPhonicComplexity")) {
							node.getMetadata().put("totalPhonicComplexity", null);
						} else {
							Object totalPhonicComplexity = text_complexity.get("totalPhonicComplexity");
							LOGGER.info("updating node with totalPhonicComplexity"
									+ text_complexity.get("totalPhonicComplexity"));
							updateNodeMetadata("totalPhonicComplexity", totalPhonicComplexity, node);
						}
					}

					LOGGER.info("checking if text complexity map contains totalWordComplexity");
					if (text_complexity.containsKey("totalWordComplexity")) {
						if (null == text_complexity.get("totalWordComplexity")) {
							node.getMetadata().put("totalWordComplexity", null);
						} else {
							Object totalWordComplexity = text_complexity.get("totalWordComplexity");
							LOGGER.info("updating node with totalWordComplexity"
									+ text_complexity.get("totalWordComplexity"));
							updateNodeMetadata("totalWordComplexity", totalWordComplexity, node);
						}
					}

					LOGGER.info("checking if text complexity map contains gradeLevel");
					if (text_complexity.containsKey("gradeLevel")) {
						updateGradeMap(node, text_complexity);
					}

					LOGGER.info("checking if text complexity map contains Themes");
					if (text_complexity.containsKey("themes")) {
						if (null == text_complexity.get("themes")) {
							node.getMetadata().put("themes", null);
						} else {
							LOGGER.info("checking if complexity map contains themes");
							Object themes = text_complexity.get("themes");
							node.getMetadata().put("themes", themes);
						}
					}
					LOGGER.info("updating node with extracted language metadata" + node);
					Response response = util.updateNode(node);
					LOGGER.info("response of content update" + response.getResponseCode());
				}
			}
		} catch (Exception e) {
			LOGGER.error("exception occured while setting language metadata" + e);
		}
	}

	private void updateData(String text, String languageId, Node node) {

		LOGGER.info("calling get complexity measures to get text_complexity" + text + languageId);
		if (StringUtils.isNotBlank(text) && StringUtils.isNotBlank(languageId)) {

			Map<String, Object> result = new HashMap<String, Object>();
			try {
				result = getComplexityMeasures(languageId, text);
				LOGGER.info("complexity measures result" + result);

				if (null != result && !result.isEmpty()) {
					LOGGER.info("mapping complexity measures with node metadata and updating the node");
					updateComplexityMeasures(node, result);
				}

			} catch (Exception e) {
				LOGGER.error("Exception occured while getting complexity measures", e);
			}
		}
	}

	private void updateNodeMetadata(String key, Object value, Node node) {

		LOGGER.info("Checking if Object is instance of Integer");
		if (value instanceof Integer) {
			Integer data = (Integer) value;
			LOGGER.info("setting node metadata with integer value" + key + value);
			node.getMetadata().put(key, data);
		}
		LOGGER.info("Checking if Object is instance of Double");
		if (value instanceof Double) {
			double data = (double) value;
			LOGGER.info("setting node metadata with double value" + key + value);
			node.getMetadata().put(key, data);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateGradeMap(Node node, Map text_complexity) {

		List<String> gradeLevel = new ArrayList<String>();
		List<String> grades = new ArrayList<String>();

		LOGGER.info("Checking if gradeLevel is null and fetched gradeLevel from node");
		if (null != node.getMetadata().get("gradeLevel")) {
			gradeLevel = (List) node.getMetadata().get("gradeLevel");
			LOGGER.info("gradeLevel fetched from node" + gradeLevel);
		}

		LOGGER.info("Checking if text_complexity result has gradeLevel");
		if (text_complexity.containsKey("gradeLevel")) {
			grades = (List) text_complexity.get("gradeLevel");
			LOGGER.info("grades fetched from complexity map" + grades);
		}

		LOGGER.info("Checking if gradeLevel from node is empty");
		if (!gradeLevel.isEmpty()) {

			LOGGER.info("Checking if grades from complexity map is empty");
			if (!grades.isEmpty()) {

				LOGGER.info("Iterating through the grades from complexity map");
				for (Object obj : grades) {
					Map gradeMap = (Map) obj;

					LOGGER.info("Iterating through the grades from gradeMap" + gradeMap);
					if (null != gradeMap && !gradeMap.isEmpty()) {

						LOGGER.info("Checking if gradeMap contains grade in it");
						if (gradeMap.containsKey("grade")) {
							String grade = gradeMap.get("grade").toString();

							LOGGER.info("Checking if grade is not there in gradeLevel from node and adding it" + grade);
							if (!gradeLevel.contains(grade) && StringUtils.isNotBlank(grade)) {
								gradeLevel.add(grade);

								LOGGER.info("Updating node with gradeLevel" + gradeLevel);
								node.getMetadata().put("gradeLevel", gradeLevel);
							}
						}
					}
				}
			}
		}
	}
}