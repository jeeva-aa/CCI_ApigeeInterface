package com.aa.crewcheckin.apigee.helper;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.aa.crewcheckin.apigee.response.ApigeeTokenResponse;
import com.aa.crewcheckin.apigee.response.Claims;
import com.google.gson.Gson;


@Component
public class ApigeeTokenHelper {

    private final Logger logger = LoggerFactory.getLogger( ApigeeTokenHelper.class);

    public final Map<String, String> jwtMap = new HashMap<String, String>();

    private static final String APIGEE_TOKEN = "apigeeToken";

    private static final String AUTH_TOKEN_HEADER = "Authorization";

    private static final String GRANT_TYPE = "grant_type";

    private static final String CLIENT_CREDENTIALS = "client_credentials";

    private static final String EXP_TIME = "expiry_time";
    
	@Autowired
	@Qualifier("RestTemplate")
	RestTemplate restTemplate;
	
	@Autowired
	Environment environment;
    
    /*
     * Apigee token expires every certain time that we specify in the configuration.
     * We have to renew every time it is about to expire and retrieving token for every call is not good for performance.
     * So make an API call 30 seconds before expiration of the token.
     */
    public String getApigeeJwtToken() {

    	logger.info(" Apigee Token URL " + environment.getProperty("apigee.tokenURL"));
    	
        if (jwtMap.get(APIGEE_TOKEN) != null) {
            final Instant expirationTime = Instant.ofEpochSecond( getTokenExpirationTime(jwtMap.get(APIGEE_TOKEN)));
            final Instant currentTime = Instant.now();
            final Duration res = Duration.between(currentTime, expirationTime);
            if (res.getSeconds() > 30) {
                return jwtMap.get(APIGEE_TOKEN);
            }
        }

        jwtMap.clear();

        try {
	        final HttpHeaders headers = getHTTPHeaders();
	        final MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
	        map.add(GRANT_TYPE, CLIENT_CREDENTIALS);
	
	        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
	        
	        final ResponseEntity<ApigeeTokenResponse> response =
	            restTemplate.exchange( environment.getProperty("apigee.tokenURL"), HttpMethod.POST, request, ApigeeTokenResponse.class);
	     
	        jwtMap.put(APIGEE_TOKEN, response.getBody().getAccessToken());
	        
        } catch( Exception e) {
        	logger.error(" There was an error in retrieving apigee token " + e.getMessage(), e.getLocalizedMessage());
        }
        return jwtMap.get(APIGEE_TOKEN);
    }

    public HttpHeaders getHTTPHeaders() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add(AUTH_TOKEN_HEADER, "Basic " + Base64Utils.encodeToString( ( environment.getProperty("apigee.username") + ":" + environment.getProperty("apigee.password")).getBytes()));
        return httpHeaders;
    }

    public long getTokenExpirationTime(final String jwtToken) {
    	
        if (jwtMap.get(EXP_TIME) != null) {
            return Long.valueOf(jwtMap.get(EXP_TIME));
        }
        
        final Base64.Decoder decoder = Base64.getUrlDecoder();
        
        final String[] jwtTokenparts = jwtToken.split("\\.");
        
        final String payload = new String(decoder.decode(jwtTokenparts[1]));

        Gson gson = new Gson();
        Claims jwtClaims = gson.fromJson( payload, Claims.class);
        logger.info(" JWT Expiration Time " + jwtClaims.getExp());
        
        jwtMap.put(EXP_TIME, jwtClaims.getExp());
        
        return Long.valueOf(jwtMap.get(EXP_TIME));
    }
}
