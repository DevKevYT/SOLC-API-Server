package com.devkev.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Gateway {
	
	public static String USER_AGENT = "Mozilla/5.0";
	
	private static Response<JsonObject> lastResponse;
	
	public static Response<JsonObject> POST(String url, String body, Property ... header) {
		URL obj;
		HttpURLConnection httpURLConnection;
		try {
			obj = new URL(url);
			httpURLConnection = (HttpURLConnection) obj.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
			httpURLConnection.setRequestProperty("Content-Type", "application/json; utf-8");
			for(Property p : header) 
				httpURLConnection.setRequestProperty(p.key, p.value);
			
			httpURLConnection.setDoOutput(true);
		} catch (Exception e) {
			return new Response<JsonObject>(null, 404, "Failed to connect to url " + url + "(" + e.toString() + ")", 0);
		}

		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpURLConnection.getOutputStream()));
			writer.write(body);
			writer.flush();
			writer.close();
			httpURLConnection.connect();
		} catch(Exception e) {
			return new Response<JsonObject>(null, 404, "Failed to send POST request to " + url, 0);
		}

		String inputLine;
		StringBuilder responseCollector = new StringBuilder();
		int responseCode = 500;
		
		try {
			responseCode = httpURLConnection.getResponseCode();
			InputStream stream;
			if(responseCode >= 200 && responseCode < 400) stream = httpURLConnection.getInputStream();
			else stream = httpURLConnection.getErrorStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			while ((inputLine = in.readLine()) != null) responseCollector.append(inputLine);
			in.close();
			
			JsonObject response;
			try {
				response = (JsonObject) JsonParser.parseString(responseCollector.toString());
			} catch(Exception e) {
				return new Response<JsonObject>(null, 500, "Unable to create JsonData with content: " + responseCollector.toString(), 0);
			}
			
			String errorMessage = "";
			int errorCode = 0;
			if(response.getAsJsonObject("error") != null) {
				JsonObject errorStuff = response.getAsJsonObject("error");
				errorCode = errorStuff.getAsJsonPrimitive("code").getAsInt();
				errorMessage = errorStuff.getAsJsonPrimitive("message").getAsString();
			}
			
			lastResponse = new Response<JsonObject>(response, responseCode, errorMessage, errorCode);
			
			return lastResponse;
		} catch(Exception e) {
			return new Response<JsonObject>(null, responseCode, "Failed to recieve response from " + url, 0);
		}
	}
	
	public static Response<JsonObject> GET(String fullURL, Property ... header) {
		URL obj;
		HttpURLConnection con;
		try {
			obj = new URL(fullURL);
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			for(Property p : header) 
				con.setRequestProperty(p.key, p.value);
			con.connect();
		} catch (Exception e) {
			lastResponse = new Response<JsonObject>(null, 404, "", 0);
			lastResponse.setUrl(fullURL);
			return lastResponse;
		}
		
		int responseCode = 404;
		String inputLine;
		StringBuilder responseCollector = new StringBuilder();
		
		try {
			responseCode = con.getResponseCode();
			InputStream stream;
			if(responseCode >= 200 && responseCode < 400) stream = con.getInputStream();
			else stream = con.getErrorStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			while ((inputLine = in.readLine()) != null) responseCollector.append(inputLine);
			in.close();
		} catch (IOException e) {
			lastResponse = new Response<JsonObject>(null, responseCode, (responseCode == 404 ? "Endpoint not found: " : ""), 0);
			lastResponse.setUrl(fullURL);
			return lastResponse;
		}
		JsonObject response;
		try {
			response = (JsonObject) JsonParser.parseString(responseCollector.toString());
		} catch(Exception e) {
			return new Response<JsonObject>(null, 500, "Unable to create JsonData", 0);
		}
		
		String errorMessage = "";
		int errorCode = 0;
		if(response.getAsJsonObject("error") != null) {
			JsonObject errorStuff = response.getAsJsonObject("error");
			errorCode = errorStuff.getAsJsonPrimitive("code").getAsInt();
			errorMessage = errorStuff.getAsJsonPrimitive("message").getAsString();
		}
		
		lastResponse = new Response<JsonObject>(response, responseCode, errorMessage, errorCode);
		lastResponse.setUrl(fullURL);
		
		return lastResponse;
	}
	
	public static Response<?> lastResponse() {
		return lastResponse;
	}
}
