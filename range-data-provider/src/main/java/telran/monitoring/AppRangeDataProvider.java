package telran.monitoring;

import java.util.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import telran.monitoring.logging.*;

public class AppRangeDataProvider implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected static final String DEFAULT_DATA_SOURCE_CLASS_NAME = "telran.monitoring.DataSourceSqlRange";
    Map<String, String> env = System.getenv();
    
    String dataSourceClassName = getDataSourceClassName();
    Logger logger = new LoggerStandard("range-data-provider");
    DataSource dataSource;
    

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        dataSource = DataSource.getDataSource(dataSourceClassName, logger);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Map<String, String> path = input.getQueryStringParameters();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            String patientIdStr = "";
            if (path == null || (patientIdStr = path.get("id")) == null) {
                throw new IllegalArgumentException("id parameter must exist");
            }
            long patientId = 0;
            try {
                patientId = Long.parseLong(patientIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("patient id must be number");
            }
           String range = dataSource.getData(patientId);
           
            response
                    .withStatusCode(200)
                    .withBody(range);
        } catch (NoSuchElementException e) {
            response
                    .withBody(e.toString())
                    .withStatusCode(404);
        } catch (IllegalArgumentException e) {
            response
                    .withBody(e.toString())
                    .withStatusCode(400);
        } catch (Exception e) {
            response
                    .withBody(e.toString())
                    .withStatusCode(500);
        }
        return response;
    }

    
    private String getDataSourceClassName() {
        return env.getOrDefault("DATA_SOURCE_CLASS_NAME", DEFAULT_DATA_SOURCE_CLASS_NAME);
}

}