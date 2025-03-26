package telran.monitoring;

import telran.monitoring.api.Range;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;

import org.json.JSONObject;

import telran.monitoring.logging.Logger;

public class EmailProviderClientHttp extends AbstractEmailProviderClient {
    private String baseURL = getBaseUrl();
    HttpClient httpClient = HttpClient.newHttpClient();

    public EmailProviderClientHttp(Logger logger) {
        super(logger);
        logger.log("info", "HTTP client for communicating with Email Provider Service");
        logger.log("config", "baseURL is " + baseURL);
    }

    private String getBaseUrl() {
        String baseUrl = System.getenv("EMAIL_PROVIDER_URL");
        if (baseUrl == null) {
            throw new RuntimeException("No value for EMAIL_PROVIDER_URL provided");
        }
        return baseUrl;
    }

    @Override
    public String getEmail(long patientId) {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(getURI(patientId))).build();
        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() > 399) {
                throw new Exception(response.body());
            }
            JSONObject jsonObject = new JSONObject(response.body());
            String res = jsonObject.getString("email");
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String getURI(long patientId) {
        String uri = baseURL + "?id=" + patientId;
        logger.log("fine", "URI is " + uri);
        return uri;
    }

}
