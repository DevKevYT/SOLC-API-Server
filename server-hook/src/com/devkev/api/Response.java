package com.devkev.api;

/**Payload is mostly in the form of a JsonObject.
 * Sometimes as a DestinyMember or sth.
 * If the errorCode == 0, the payload will be 0.
 * Also useful for standart function responses, since the payload can be any type.
 * 
 * In the context of this program, the Response class is returned by ANY method, that uses an API request anywhere*/
public class Response<T> {
	
	private final long timestamp;
	public final int httpStatus;
	public final int errorCode;
	private final T payload;
	public final String errorMessage;
	private String url;
	
	/**Imitates an error response with specified errors*/
	public Response(T payload, int httpStatus, String errorMessage, int errorCode) {
		this.timestamp = System.currentTimeMillis();
		this.payload = payload;
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
	
	/**Imitates a success response*/
	public Response(T payload) {
		this.timestamp = System.currentTimeMillis();
		this.payload = payload;
		this.httpStatus = 200;
		this.errorCode = 1;
		this.errorMessage = "Success";
	}
	
	void setUrl(String url) {
		this.url = url;
	}
	
	public String getOriginalUri() {
		return url;
	}
	
	/**Returns the timestamp this object was created or a request was handled*/
	public long getTimeStamp() {
		return timestamp;
	}
	
	public T getResponseData() {
		return payload;
	}
	
	public boolean containsPayload() {
		return payload != null;
	}
	
	public boolean success() {
		return errorCode == 1 && httpStatus == 200;
	}
	
	public String toString() {
		return "Status:\nHTTP:" + httpStatus + ",\nBungie:" + errorCode + ",\nMessage:" + errorMessage + ",\nHasPayload?" + containsPayload();
	}
}
