package hoopoe.extensions.webview.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlet.ServletHolder;

public class JsonRpcServletHolder extends ServletHolder {

    public JsonRpcServletHolder(ObjectMapper objectMapper, Object serviceImpl, Class serviceInterface) {
        JsonRpcServer jsonRpcServer = new JsonRpcServer(objectMapper, serviceImpl, serviceInterface);
        setServlet(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                jsonRpcServer.handle(req, resp);
            }
        });
    }

}
