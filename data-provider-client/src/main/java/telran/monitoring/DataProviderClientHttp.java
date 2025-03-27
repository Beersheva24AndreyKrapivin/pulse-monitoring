package telran.monitoring;

import java.beans.DefaultPersistenceDelegate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyStore.Entry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import telran.monitoring.logging.Logger;

record DataTimestanp(String data, long timestamp) {
}

public class DataProviderClientHttp implements DataProviderClient {

    private static final int DEFAULT_CACHE_CAPACITY = 200;
    private static final long DEFAULT_CACHE_REFRESH_TIME = 24 * 3600 * 1000;
    Logger logger = loggers[0];
    private int cacheCapacity = getCacheCapacity();
    LinkedHashMap<Long, DataTimestanp> cache = new LinkedHashMap<>(cacheCapacity + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, DataTimestanp> eldestEntry) {
            return size() >= cacheCapacity;
        }
    };
    HttpClient httpClient = HttpClient.newHttpClient();
    long refreshTime = getRefreshTime();
    String baseUrl;

    public DataProviderClientHttp(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private long getRefreshTime() {
        String refreshTimeStr = System.getenv("CACHE_REFRESH_TIME");
        long res = DEFAULT_CACHE_REFRESH_TIME;
        if (refreshTimeStr != null) {
            try {
                res = Long.parseLong(refreshTimeStr);
                logger.log("fine", "Configured refresh time set " + res);
            } catch (NumberFormatException e) {
                logger.log("sever", "Wrong env.variable value for refresh time, default set " + res);
                throw new RuntimeException(e);
            }
        } else {
            logger.log("fine", "no new configured refresh time, default set " + res);
        }
        return res;
    }

    private int getCacheCapacity() {
        String cacheCapacityStr = System.getenv("CACHE_CAPACITY");
        int res = DEFAULT_CACHE_CAPACITY;
        if (cacheCapacityStr != null) {
            try {
                res = Integer.parseInt(cacheCapacityStr);
                logger.log("config", res + " configured value from env.variable");
            } catch (Exception e) {
                logger.log("severe", "wrong cache capacity env.variable, default value has been set " + res);
            }
        } else {
            logger.log("config", "default value of cache capacity has been set " + res);
        }
        return res;
    }

    @Override
    public String getDataForPatient(long patientId) {
        String res = getDataFromCache(patientId);
        if (res == null) {
            res = httpRequest(patientId);
            setDataToCache(res, patientId);
            logger.log("fine", String.format("new value %s for patient %d added to cache", res, patientId));
        } else {
            logger.log("fine", String.format("existing value %s from cache received for patient %d", res, patientId));
        }
        return res;
    }

    private void setDataToCache(String res, long patientId) {
        cache.put(patientId, new DataTimestanp(res, System.currentTimeMillis()));
    }

    private String getDataFromCache(long patientId) {
        DataTimestanp dt = cache.get(patientId);
        String res = null;

        if (dt != null && System.currentTimeMillis() - dt.timestamp() < refreshTime) {
            res = dt.data();
        }
        return res;
    }

    private String getURI(long patientId) {
        String uri = baseUrl + "?id=" + patientId;
        logger.log("fine", "URI is " + uri);
        return uri;
    }

    private String httpRequest(long patientId) {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(getURI(patientId))).build();
        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() > 399) {
                throw new Exception(response.body());
            }
            logger.log("fine", "Range received from Range Provider API service is " + range);
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
