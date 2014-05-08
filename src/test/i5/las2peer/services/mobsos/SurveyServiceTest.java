/*
Copyright (c) 2014 Dominik Renzel, Advanced Community Information Systems (ACIS) Group, 
Chair of Computer Science 5 (Databases & Information Systems), RWTH Aachen University, Germany
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

 * Neither the name of the {organization} nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package i5.las2peer.services.mobsos;

import static org.junit.Assert.*;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit Test Class for MobSOS Survey Service
 * 
 * @author Dominik Renzel
 *
 */
public class SurveyServiceTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static MiniClient c;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static final String testPass = "adamspass";

	private static final String testServiceClass = "i5.las2peer.services.mobsos.SurveyService";


	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void startServer() throws Exception {

		//start node
		node = LocalNode.newNode();

		testAgent = MockAgentFactory.getAdam();
		node.storeAgent(testAgent);
		node.launch();

		ServiceAgent testService = ServiceAgent.generateNewAgent(testServiceClass, "a pass");
		testService.unlockPrivateKey("a pass");

		node.registerReceiver(testService);

		//start connector
		logStream = new ByteArrayOutputStream ();

		//connector = new WebConnector(true,HTTP_PORT,false,1000,"./etc/xmlc");
		connector = new WebConnector(true,HTTP_PORT,false,1000);

		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream (logStream));


		connector.start ( node );
		Thread.sleep(1000); //wait a second for the connector to become ready

		connector.updateServiceList();

		c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		c.setLogin(Long.toString(testAgent.getId()), testPass);

		//avoid timing errors: wait for the repository manager to get all services before continuing
		/*
		try
		{
			System.out.println("waiting..");
			Thread.sleep(10000);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}*/

	}

	/**
	 * Called after the tests have finished.
	 * Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void shutDownServer () throws Exception {

		connector.stop();
		node.shutDown();

		connector = null;
		node = null;

		LocalNode.reset();

		System.out.println("Connector-Log:");
		System.out.println("--------------");

		System.out.println(logStream.toString());

	}

	/**
	 * Test the creation of new surveys.
	 */
	@Test
	public void testSurveyCreation()
	{
		try {

			ClientResponse result=c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());

			assertEquals(201, result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject jo = (JSONObject) o;
			assertTrue(jo.keySet().contains("url"));
			String urlStr = (String) jo.get("url");
			URL url = new URL(urlStr);
			//System.out.println(jo.toJSONString());

		} catch (ParseException e){
			e.printStackTrace();
			fail("Could not parse service response to JSON Object!");
		} catch (MalformedURLException e){
			e.printStackTrace();
			fail("Service should return a valid URL to the new survey's resource");
		}
	}
	
	/**
	 * Test the creation of new questionnaires.
	 */
	@Test
	public void testQuestionnaireCreation()
	{
		try {

			ClientResponse result=c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());

			assertEquals(201, result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject jo = (JSONObject) o;
			assertTrue(jo.keySet().contains("url"));
			String urlStr = (String) jo.get("url");
			URL url = new URL(urlStr);
			//System.out.println(jo.toJSONString());

		} catch (ParseException e){
			e.printStackTrace();
			fail("Could not parse service response to JSON Object!");
		} catch (MalformedURLException e){
			e.printStackTrace();
			fail("Service should return a valid URL to the new questionnaire's resource");
		}
	}

	/**
	 * Test the creation of new surveys with invalid data.
	 */
	@Test
	public void testBadRequestSurveyCreation()
	{
		JSONObject invalidSurvey = generateSurveyJSON(); // until now, survey JSON is ok. Introduce problems now...
		invalidSurvey.put("name", new Integer(2)); // name must be string

		ClientResponse result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidSurvey.put("name", "Alcoholics Standard Survey"); //make valid again and introduce other problem
		invalidSurvey.put("start", "20144-33-44T1000"); //introduce wrong time

		result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidSurvey.put("start", "2014-05-08T12:00:00Z"); //make valid again and introduce other problem
		invalidSurvey.put("end", "2014-04-01T00:00:00Z"); // end time before start time

		result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidSurvey.put("end", "2014-06-08T00:00:00Z"); // make valid again and introduce other problem
		invalidSurvey.put("logo","dbis"); // malformed logo URL

		result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidSurvey.put("logo","http://dbis.rwth-aachen.de/nonexistingimage"); // non-existing logo resource

		result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidSurvey.put("logo","http://dbis.rwth-aachen.de/gadgets"); // existing non-image resource

		result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());
		
		invalidSurvey.put("logo","http://dbis.rwth-aachen.de/cms/images/logo.jpg"); // make valid again and introduce other problem
		invalidSurvey.put("resource","shitonashingle"); // malformed resource URL
		
		result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());
		
		invalidSurvey.put("resource","http://dbis.rwth-aachen.de/nonexistingresource"); // non-existing resource URL
		
		result=c.sendRequest("POST", "mobsos/surveys",invalidSurvey.toJSONString());
		assertEquals(400, result.getHttpCode());

	}
	
	/**
	 * Test the creation of new surveys with invalid data.
	 */
	@Test
	public void testBadRequestQuestionnaireCreation()
	{
		JSONObject invalidQuestionnaire = generateQuestionnaireJSON(); // until now, questionnaire JSON is ok. Introduce problems now...
		invalidQuestionnaire.put("name", new Integer(2)); // name must be string

		ClientResponse result=c.sendRequest("POST", "mobsos/questionnaires",invalidQuestionnaire.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidQuestionnaire.put("name", "Alcoholics Standard Questionnaire"); //make valid again and introduce other problem
		invalidQuestionnaire.put("logo","dbis"); // malformed logo URL

		result=c.sendRequest("POST", "mobsos/questionnaires",invalidQuestionnaire.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidQuestionnaire.put("logo","http://dbis.rwth-aachen.de/nonexistingimage"); // non-existing logo resource

		result=c.sendRequest("POST", "mobsos/questionnaires",invalidQuestionnaire.toJSONString());
		assertEquals(400, result.getHttpCode());

		invalidQuestionnaire.put("logo","http://dbis.rwth-aachen.de/gadgets"); // existing non-image resource

		result=c.sendRequest("POST", "mobsos/questionnaires",invalidQuestionnaire.toJSONString());
		assertEquals(400, result.getHttpCode());

	}

	/**
	 * Test the retrieval of survey lists.
	 */
	@Test
	public void testSurveyListRetrieval(){
		try {
			ClientResponse result=c.sendRequest("GET", "mobsos/surveys","");
			assertEquals(200,result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject jo = (JSONObject) o;
			assertTrue(jo.get("surveys") != null);
			o = jo.get("surveys");
			assertTrue(o instanceof JSONArray);
			//System.out.println(jo.toJSONString());

		} catch (ParseException e) {
			fail("Could not parse service response to JSON Object!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Test the retrieval of questionnaire lists.
	 */
	@Test
	public void testQuestionnaireListRetrieval(){
		try {
			ClientResponse result=c.sendRequest("GET", "mobsos/questionnaires","");
			assertEquals(200,result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject jo = (JSONObject) o;
			assertTrue(jo.get("questionnaires") != null);
			o = jo.get("questionnaires");
			assertTrue(o instanceof JSONArray);
			//System.out.println(jo.toJSONString());

		} catch (ParseException e) {
			fail("Could not parse service response to JSON Object!");
			e.printStackTrace();
		}
	}

	/**
	 * Test the retrieval of a single survey.
	 */
	@Test
	public void testSurveyRetrieval(){
		try {
			// add a couple of surveys
			c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());
			c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());

			// then get complete list and pick the first survey URL for subsequent testing
			ClientResponse list = c.sendRequest("GET", "mobsos/surveys","");
			JSONObject jo = (JSONObject) JSONValue.parseWithException(list.getResponse().trim());
			String fullurl = (String) ((JSONArray) jo.get("surveys")).get(0);

			// check if first survey URL is a valid URL, then extract path
			URL u = new URL(fullurl);
			u.getPath();
			String pathonly = u.getPath();

			// now check if survey retrieval works properly
			ClientResponse result=c.sendRequest("GET", pathonly,"");
			assertEquals(200,result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject rjo = (JSONObject) o;

			// check if all fields are contained in result
			assertTrue(rjo.keySet().contains("id"));
			assertTrue(rjo.keySet().contains("name"));
			assertTrue(rjo.keySet().contains("organization"));
			assertTrue(rjo.keySet().contains("logo"));
			assertTrue(rjo.keySet().contains("description"));
			assertTrue(rjo.keySet().contains("owner"));
			assertTrue(rjo.keySet().contains("resource"));
			assertTrue(rjo.keySet().contains("start"));
			assertTrue(rjo.keySet().contains("end"));

		
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Could not parse service response to JSON Object!");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Detected invalid survey URL! " + e.getMessage());
		}
	}

	/**
	 * Test the retrieval of a single questionnaire.
	 */
	@Test
	public void testQuestionnaireRetrieval(){
		try {
			// add a couple of surveys
			c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());
			c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());

			// then get complete list and pick the first questionnaire URL for subsequent testing
			ClientResponse list = c.sendRequest("GET", "mobsos/questionnaires","");
			JSONObject jo = (JSONObject) JSONValue.parseWithException(list.getResponse().trim());
			String fullurl = (String) ((JSONArray) jo.get("questionnaires")).get(0);

			// check if first survey URL is a valid URL, then extract path
			URL u = new URL(fullurl);
			u.getPath();
			String pathonly = u.getPath();

			// now check if survey retrieval works properly
			ClientResponse result=c.sendRequest("GET", pathonly,"");
			assertEquals(200,result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject rjo = (JSONObject) o;

			// check if all fields are contained in result
			assertTrue(rjo.keySet().contains("id"));
			assertTrue(rjo.keySet().contains("name"));
			assertTrue(rjo.keySet().contains("organization"));
			assertTrue(rjo.keySet().contains("logo"));
			assertTrue(rjo.keySet().contains("description"));
			assertTrue(rjo.keySet().contains("owner"));
		
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Could not parse service response to JSON Object!");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Detected invalid questionnaire URL! " + e.getMessage());
		}
	}
	
	/**
	 * Test the deletion of all surveys at once.
	 */
	@Test
	public void testDeleteAllSurveys(){
		try {


			// first add a couple of surveys
			c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());
			c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());
			c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());

			// check if deletion of all surveys works
			ClientResponse delete=c.sendRequest("DELETE", "mobsos/surveys","");
			assertEquals(200,delete.getHttpCode());

			// then check if survey list retrieval retrieves an empty list.
			ClientResponse result=c.sendRequest("GET", "mobsos/surveys","");
			assertEquals(200,result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject jo = (JSONObject) o;
			assertTrue(jo.get("surveys") != null);
			o = jo.get("surveys");
			assertTrue(o instanceof JSONArray);
			JSONArray ja = (JSONArray) o;
			assertTrue(ja.isEmpty());

		}  catch (ParseException e) {
			fail("Could not parse service response to JSON Object!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Test the deletion of all questionnaires at once.
	 */
	@Test
	public void testDeleteAllQuestionnaires(){
		try {

			// first add a couple of questionnaires
			c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());
			c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());
			c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());

			// check if deletion of all questionnaires works
			ClientResponse delete=c.sendRequest("DELETE", "mobsos/questionnaires","");
			assertEquals(200,delete.getHttpCode());

			// then check if questionnaire list retrieval retrieves an empty list.
			ClientResponse result=c.sendRequest("GET", "mobsos/questionnaires","");
			assertEquals(200,result.getHttpCode());
			Object o = JSONValue.parseWithException(result.getResponse().trim());
			assertTrue(o instanceof JSONObject);
			JSONObject jo = (JSONObject) o;
			assertTrue(jo.get("questionnaires") != null);
			o = jo.get("questionnaires");
			assertTrue(o instanceof JSONArray);
			JSONArray ja = (JSONArray) o;
			assertTrue(ja.isEmpty());

		}  catch (ParseException e) {
			fail("Could not parse service response to JSON Object!");
			e.printStackTrace();
		}
	}

	/**
	 * Test the deletion of an individual existing survey.
	 */
	@Test
	public void testDeleteExistingSurvey(){
		try {

			// first add a survey
			c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());

			// then get complete list and pick the first survey URL for subsequent testing
			ClientResponse list = c.sendRequest("GET", "mobsos/surveys","");
			JSONObject jo = (JSONObject) JSONValue.parseWithException(list.getResponse().trim());
			String fullurl = (String) ((JSONArray) jo.get("surveys")).get(0);

			// check if first survey URL is a valid URL, then extract path
			URL u = new URL(fullurl);

			// check if deletion of particular surveys works
			ClientResponse delete=c.sendRequest("DELETE", u.getPath(),"");
			assertEquals(200,delete.getHttpCode());

			// then check if previously deleted survey still exists.
			ClientResponse result=c.sendRequest("GET", u.getPath(),"");
			assertEquals(404,result.getHttpCode());

		}  catch (ParseException e) {
			e.printStackTrace();
			fail("Could not parse service response to JSON Object!");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Detected invalid survey URL! " + e.getMessage());
		}
	}
	
	/**
	 * Test the deletion of an individual existing questionnaire.
	 */
	@Test
	public void testDeleteExistingQuestionnaire(){
		try {

			// first add a survey
			c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());

			// then get complete list and pick the first questionnaire URL for subsequent testing
			ClientResponse list = c.sendRequest("GET", "mobsos/questionnaires","");
			JSONObject jo = (JSONObject) JSONValue.parseWithException(list.getResponse().trim());
			String fullurl = (String) ((JSONArray) jo.get("questionnaires")).get(0);

			// check if first questionnaire URL is a valid URL, then extract path
			URL u = new URL(fullurl);
			
			System.out.println("Path: " + u.getPath());

			// check if deletion of particular questionnaires works
			ClientResponse delete=c.sendRequest("DELETE", u.getPath(),"");
			assertEquals(200,delete.getHttpCode());

			// then check if previously deleted questionnaire still exists.
			ClientResponse result=c.sendRequest("GET", u.getPath(),"");
			assertEquals(404,result.getHttpCode());

		}  catch (ParseException e) {
			e.printStackTrace();
			fail("Could not parse service response to JSON Object!");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Detected invalid survey URL! " + e.getMessage());
		}
	}

	/**
	 * Test the deletion of a non-existing survey.
	 */
	@Test
	public void testDeleteNonExistingSurvey(){
		// check if deletion of non-existing surveys works.
		ClientResponse delete=c.sendRequest("DELETE","/mobsos/surveys/-1","");
		assertEquals(404,delete.getHttpCode());
	}
	
	/**
	 * Test the deletion of a non-existing questionnaire.
	 */
	@Test
	public void testDeleteNonExistingQuestionnaire(){
		// check if deletion of non-existing questionnaires works.
		ClientResponse delete=c.sendRequest("DELETE","/mobsos/questionnaires/-1","");
		assertEquals(404,delete.getHttpCode());
	}

	/**
	 * Test the updating of an existing survey.
	 */
	@Test
	public void testUpdateExistingSurvey(){
		// first add a new survey
		ClientResponse r = c.sendRequest("POST", "mobsos/surveys",generateSurveyJSON().toJSONString());
		assertTrue(r.getResponse()!=null);
		assertTrue(r.getResponse().length()>0);
		try{
			JSONObject o = (JSONObject) JSONValue.parseWithException(r.getResponse().trim());
			assertTrue(o.keySet().contains("url"));

			String fullurl = (String) o.get("url");

			// check if first survey URL is a valid URL, then extract path
			URL u = new URL(fullurl);
			String pathonly = u.getPath();

			// use path to get the survey
			ClientResponse result=c.sendRequest("GET", u.getPath(),"");
			assertEquals(200,result.getHttpCode()); // survey should exist
			JSONObject survey = (JSONObject) JSONValue.parse(result.getResponse());

			// change some fields in survey
			survey.put("name","Beerdrinker Survey");
			survey.put("description", "This survey is for all those who like to drink beer.");

			// then call service to update existing survey
			ClientResponse updateresult=c.sendRequest("POST", u.getPath(),survey.toJSONString());
			assertEquals(200, updateresult.getHttpCode());

			ClientResponse updated=c.sendRequest("GET", u.getPath(),"");
			assertEquals(200,updated.getHttpCode()); // survey should exist
			JSONObject updatedSurvey = (JSONObject) JSONValue.parse(updated.getResponse());

			assertEquals(survey,updatedSurvey);

		} catch (ParseException e) {
			e.printStackTrace();
			fail("Service returned invalid JSON! " + e.getMessage());
		} catch (MalformedURLException e){
			e.printStackTrace();
			fail("Service returned malformed URL!");
		}
	}
	
	/**
	 * Test the updating of an existing questionnaire.
	 */
	@Test
	public void testUpdateExistingQuestionnaire(){
		// first add a new questionnaire
		ClientResponse r = c.sendRequest("POST", "mobsos/questionnaires",generateQuestionnaireJSON().toJSONString());
		assertTrue(r.getResponse()!=null);
		assertTrue(r.getResponse().length()>0);
		try{
			JSONObject o = (JSONObject) JSONValue.parseWithException(r.getResponse().trim());
			assertTrue(o.keySet().contains("url"));

			String fullurl = (String) o.get("url");

			// check if first questionnaire URL is a valid URL, then extract path
			URL u = new URL(fullurl);
			String pathonly = u.getPath();

			// use path to get the questionnaire
			ClientResponse result=c.sendRequest("GET", u.getPath(),"");
			assertEquals(200,result.getHttpCode()); // questionnaire should exist
			
			JSONObject questionnaire = (JSONObject) JSONValue.parse(result.getResponse());

			// change some fields in questionnaire
			questionnaire.put("name","Beerdrinker questionnaire");
			questionnaire.put("description", "This questionnaire is for all those who like to drink beer.");

			// then call service to update existing questionnaire
			ClientResponse updateresult=c.sendRequest("POST", u.getPath(),questionnaire.toJSONString());
			assertEquals(200, updateresult.getHttpCode());

			ClientResponse updated=c.sendRequest("GET", u.getPath(),"");
			assertEquals(200,updated.getHttpCode()); // questionnaire should exist
			JSONObject updatedQuestionnaire = (JSONObject) JSONValue.parse(updated.getResponse());

			assertEquals(questionnaire,updatedQuestionnaire);

		} catch (ParseException e) {
			e.printStackTrace();
			fail("Service returned invalid JSON! " + e.getMessage());
		} catch (MalformedURLException e){
			e.printStackTrace();
			fail("Service returned malformed URL!");
		}
	}

	/**
	 * Generates a valid survey JSON representation.
	 */
	@SuppressWarnings("unchecked")
	private static JSONObject generateSurveyJSON(){

		JSONObject obj = new JSONObject(); 
		obj.put("name","Wikipedia Survey " + (new Date()).getTime());
		obj.put("organization", "Advanced Community Information Systems Group, RWTH Aachen University");
		obj.put("logo","http://dbis.rwth-aachen.de/cms/images/logo.jpg");
		obj.put("description","A sample survey on Wikipedia.");
		obj.put("resource", "http://wikipedia.org"); 
		obj.put("start","2014-06-06T00:00:00Z");
		obj.put("end", "2014-08-06T23:59:59Z");

		return obj;
	}
	
	/**
	 * Generates a valid questionnaire JSON representation.
	 */
	@SuppressWarnings("unchecked")
	private static JSONObject generateQuestionnaireJSON(){

		JSONObject obj = new JSONObject(); 
		obj.put("name","Quality Questionnaire " + (new Date()).getTime());
		obj.put("organization", "Advanced Community Information Systems Group, RWTH Aachen University");
		obj.put("logo","http://dbis.rwth-aachen.de/cms/images/logo.jpg");
		obj.put("description","A questionnaire designed to ask for quality");

		return obj;
	}
}