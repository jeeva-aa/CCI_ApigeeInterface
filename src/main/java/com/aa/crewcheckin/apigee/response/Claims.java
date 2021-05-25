package com.aa.crewcheckin.apigee.response;

public class Claims {

	private String access_token;
	
	private String exp;
	
	private String client_id;
	
	private String iss;

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public String getExp() {
		return exp;
	}

	public void setExp(String exp) {
		this.exp = exp;
	}

	public String getClient_id() {
		return client_id;
	}

	public void setClient_id(String client_id) {
		this.client_id = client_id;
	}

	public String getIss() {
		return iss;
	}

	public void setIss(String iss) {
		this.iss = iss;
	}

	@Override
	public String toString() {
		return "Claims [access_token=" + access_token + ", exp=" + exp + ", client_id=" + client_id + ", iss=" + iss
				+ "]";
	}
}
