package hoopoe.tests;

import hoopoe.core.HoopoeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.testcontainers.containers.Container;

/**
 * Utility to execute HTTP requests to the container.
 */
public class HttpEndpoint {

    private final String url;

    private HttpClient httpClient = HttpClients.createDefault();

    private HttpEndpoint(String url) {
        this.url = url;
    }

    /**
     * Creates an instance for the exposed port in the provided container.
     *
     * @param container   docker container to send requests to.
     * @param exposedPort port exposed by the container. Is used to find mapped port to send requests via.
     * @param path        path to the endpoint within the container.
     *
     * @return new instance of endpoint, ready to execute any number of requests.
     */
    public static HttpEndpoint forContainer(Container container, int exposedPort, String path) {
        return new HttpEndpoint("http://" + container.getContainerIpAddress()
                + ":" + container.getMappedPort(exposedPort)
                + path
        );
    }

    /**
     * Executes GET request to the endpoint and returns plain string response.
     *
     * @return plain string response, retrieved with UTF-8 encoding.
     */
    public String executeGetForString() {
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(request);
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new HoopoeException(e);
        }
    }

}
