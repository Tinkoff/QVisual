/*
 * Copyright (c) 2010-2014 Gurock Software GmbH
 *
 * https://github.com/gurock/testrail-api
 *
 */
package ru.tinkoff;

import org.json.simple.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.json.simple.JSONValue.parse;
import static org.json.simple.JSONValue.toJSONString;

public class TestRailClient {

    private String m_user;
    private String m_password;
    private String m_url;

    public TestRailClient(String base_url, String login, String password) {
        if (isNullOrEmpty(base_url) || isNullOrEmpty(login) || isNullOrEmpty(password)) {
            throw new IllegalArgumentException(String.format(
                    "Could not start TestRailClient with parameters: URL = %s, login = %s, password = %s",
                    base_url, login, password));
        } else {
            if (!base_url.endsWith("/")) {
                base_url += "/";
            }

            m_url = base_url + "index.php?/api/v2/";
            m_user = login;
            m_password = password;
        }
    }

    /**
     * Send Get
     * <p>
     * Issues a GET request (read) against the API and returns the result
     * (as Object, see below).
     * <p>
     * Arguments:
     * <p>
     * uri                  The API method to call including parameters
     * (e.g. get_case/1)
     * <p>
     * Returns the parsed JSON response as standard object which can
     * either be an instance of JSONObject or JSONArray (depending on the
     * API method). In most cases, this returns a JSONObject instance which
     * is basically the same as java.util.Map.
     */
    public Object sendGet(String uri) throws Exception {
        return this.sendRequest("GET", uri, null);
    }

    /**
     * Send POST
     * <p>
     * Issues a POST request (write) against the API and returns the result
     * (as Object, see below).
     * <p>
     * Arguments:
     * <p>
     * uri                  The API method to call including parameters
     * (e.g. add_case/1)
     * data                 The data to submit as part of the request (e.g.,
     * a map)
     * <p>
     * Returns the parsed JSON response as standard object which can
     * either be an instance of JSONObject or JSONArray (depending on the
     * API method). In most cases, this returns a JSONObject instance which
     * is basically the same as java.util.Map.
     */
    public Object sendPost(String uri, Object data) throws Exception {
        return this.sendRequest("POST", uri, data);
    }

    private Object sendRequest(String method, String uri, Object data) throws Exception {
        URL url = new URL(this.m_url + uri);

        // Create the connection object and set the required HTTP method
        // (GET/POST) and headers (content type and basic auth).
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Content-Type", "application/json");

        String auth = getAuthorization(this.m_user, this.m_password);
        conn.addRequestProperty("Authorization", "Basic " + auth);

        if (method == "POST") {
            // Add the POST arguments, if any. We just serialize the passed
            // data object (i.e. a dictionary) and then add it to the
            // request body.
            if (data != null) {
                byte[] block = toJSONString(data).getBytes("UTF-8");

                conn.setDoOutput(true);
                OutputStream ostream = conn.getOutputStream();
                ostream.write(block);
                ostream.flush();
            }
        }

        // Execute the actual web request (if it wasn't already initiated
        // by getOutputStream above) and record any occurred errors (we use
        // the error stream in this case).
        int status = conn.getResponseCode();

        InputStream istream;
        if (status != 200) {
            istream = conn.getErrorStream();
            if (istream == null) {
                throw new Exception("TestRail API return HTTP " + status + " (No additional error message received)");
            }
        } else {
            istream = conn.getInputStream();
        }

        // Read the response body, if any, and deserialize it from JSON.
        String text = "";
        if (istream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(istream, "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                text += line;
                text += System.getProperty("line.separator");
            }

            reader.close();
        }

        Object result;
        if (text != "") {
            result = parse(text);
        } else {
            result = new JSONObject();
        }

        // Check for any occurred errors and add additional details to
        // the exception message, if any (e.g. the error message returned
        // by TestRail).
        if (status != 200) {
            String error = "No additional error message received";
            if (result != null && result instanceof JSONObject) {
                JSONObject obj = (JSONObject) result;
                if (obj.containsKey("error")) {
                    error = '"' + (String) obj.get("error") + '"';
                }
            }

            throw new Exception("TestRail API returned HTTP " + status + "(" + error + ")");
        }

        return result;
    }

    private static String getAuthorization(String user, String password) {
        try {
            return DatatypeConverter.printBase64Binary((user + ":" + password).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}