package hoopoe.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import hoopoe.api.HoopoeProfiledResult;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

class ExtensionConnector {

    private String baseUrl;
    private HttpClient httpClient = HttpClients.createDefault();

    public ExtensionConnector(String ipAddress, int port) {
        this.baseUrl = "http://" + ipAddress + ":" + port + "/hoopoe-tests/";
    }

    public void startProfiling() {
        try {
            executeRequestAndGetBody("start-profiling");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public HoopoeProfiledResult stopProfiling() {
        try {
            String profiledResultJson = executeRequestAndGetBody("stop-profiling");
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectReader resultReader = objectMapper.readerFor(HoopoeProfiledResult.class);
            return resultReader.readValue(profiledResultJson);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String executeRequestAndGetBody(String action) throws IOException {
        HttpGet extensionRequest = new HttpGet(baseUrl + action);
        HttpResponse extensionResponse = httpClient.execute(extensionRequest);
        return EntityUtils.toString(extensionResponse.getEntity());
    }

}
