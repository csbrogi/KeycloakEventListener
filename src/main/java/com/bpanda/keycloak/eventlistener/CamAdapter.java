package com.bpanda.keycloak.eventlistener;

import com.bpanda.keycloak.model.CamUser;
import com.bpanda.keycloak.model.KeycloakData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CamAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
    private final String campServer;
    private final String accountId;
    private final String clientSecret;
    private final String realm;
    private final String clientId;//  = "camp";
    private final String keycloakServer;//  = "https://pc62.mid.de:9443";

    public CamAdapter(String campServer, String accountId, KeycloakData keycloakData) {
        this.campServer = campServer;
        this.clientSecret = keycloakData.getClientSecret();
        this.clientId = keycloakData.getClientId();
        this.keycloakServer = keycloakData.getKeycloakServer();
        this.accountId = accountId;
        this.realm = keycloakData.getRealmName();
    }

    public void createUser(String email) throws CampException, IOException {
        String token = getAccessToken();
        String createUrl = String.format("%s/subapi/api/accounts/%s/users/create", campServer, accountId);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(createUrl);
        post.setHeader("Authorization", "Bearer " + token);
        String userAsJson = String.format("{\n" +
                "\"Emails\": [\"%s\"]," +
                "\"LicenceNameId\": \"\"\n" +
                "}", email);

        StringEntity requestEntity = new StringEntity(
                userAsJson,
                ContentType.APPLICATION_JSON);
        post.setEntity(requestEntity);

        logger.debug("**POST** request Url: " + post.getURI());

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(post);
            this.handleResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateUser(CamUser updateUser) throws IOException, CampException {
        String updateUrl = String.format("%s/subapi/api/accounts/%s/users/%s", campServer, accountId, updateUser.getId());

        String jsonString = updateUser.toJsonString();
        String token = getAccessToken();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(updateUrl);
        httpPut.setHeader("Authorization", "Bearer " + token);
        httpPut.setHeader("Content-Type", "application/json");
        StringEntity requestEntity = new StringEntity(
                jsonString,
                ContentType.APPLICATION_JSON);
        httpPut.setEntity(requestEntity);

        logger.debug("**PUT** request Url: " + httpPut.getURI());

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpPut);
            this.handleResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startUserSync() throws CampException, IOException {
        String token = getAccessToken();
        String syncUrl = String.format("%s/subapi/api/accounts/%s/identity/sync/all", campServer, accountId);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(syncUrl);
        post.setHeader("Authorization", "Bearer " + token);
        String syncJson = "{]";

        StringEntity requestEntity = new StringEntity(
                syncJson,
                ContentType.APPLICATION_JSON);
        post.setEntity(requestEntity);

        logger.debug("**POST** request Url: " + post.getURI());

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(post);
            this.handleResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getAccessToken() throws IOException, CampException {
        String tokenUrl = keycloakServer + "/auth/realms/" + realm + "/protocol/openid-connect/token";

        String encodedString = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("grant_type", "client_credentials"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(tokenUrl);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("Authorization", "Basic " + encodedString);
        post.setEntity(entity);
        HttpResponse response = httpClient.execute(post);
        int responseCode = response.getStatusLine().getStatusCode();
        logger.debug("Response Code: " + responseCode);
        logger.debug("Content:");

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        String line;
        String token = null;
        ObjectMapper mapper = new ObjectMapper();
        while ((line = rd.readLine()) != null && token == null) {
            JsonNode json = mapper.readTree(line);
            List<String> vals = json.findValuesAsText("access_token");
            if (vals.size() == 1) {
                token = vals.get(0);
            }

            logger.trace(token);
        }
        if (responseCode >= 400) {
            String errrorMsg = String.format("Cannot get Access Token for clientId %s and secret %s ", clientId, clientSecret);
            throw new CampException(errrorMsg);
        }
        return token;
    }

    private void handleResponse(CloseableHttpResponse response) throws IOException, CampException {
        int responseCode = response.getStatusLine().getStatusCode();

        logger.debug("Response Code: " + responseCode);
        if (response.getEntity() != null) {
            logger.debug("Response getContentType: " + response.getEntity().getContentType());
            logger.debug("Content:");

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line;
            StringBuilder jsonLine = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                logger.debug("Raw: " + line);
                jsonLine.append(line);
            }
            if (responseCode >= 400) {
                logger.error("Throwing ScimException " + jsonLine);
                throw new CampException(jsonLine.toString(), responseCode);
            }

            ByteBuffer buffer = StandardCharsets.ISO_8859_1.encode(jsonLine.toString());
            String result = StandardCharsets.UTF_8.decode(buffer).toString();
            logger.debug(result);
        }
    }
}
