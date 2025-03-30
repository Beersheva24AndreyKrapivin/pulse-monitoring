package telran.monitoring;

import java.util.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import telran.monitoring.logging.*;

public class AppEmailProvider implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected static final String DEFAULT_DATA_SOURCE_CLASS_NAME = "telran.monitoring.DatSourceSqlEmail";
        Map<String, String> env = System.getenv();
        String dataSourceClassName = getDataSourceClassName();
    
        Logger logger = new LoggerStandard("range-data-provider");
        DataSource dataSource;
    
        public AppEmailProvider() {
            logger.log("config", "Data Source Class Name is " + dataSourceClassName);
            dataSource = DataSource.getDataSource(dataSourceClassName, logger);
        }
    
        private String getDataSourceClassName() {
            return env.getOrDefault("DATA_SOURCE_CLASS_NAME", DEFAULT_DATA_SOURCE_CLASS_NAME);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

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
            String email = dataSource.getData(patientId);

            response
                    .withStatusCode(200)
                    .withBody(email);
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

    

    

   

}