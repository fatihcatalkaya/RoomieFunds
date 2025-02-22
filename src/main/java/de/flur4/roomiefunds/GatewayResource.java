package de.flur4.roomiefunds;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

@Path("/")
public class GatewayResource {
    private static final String FALLBACK_RESOURCE = "/frontend/fallback.html";

    @GET
    @Path("/")
    public Response getFrontendRoot() throws IOException {
        return getFrontendStaticFile("index.html");
    }

    @GET
    @Path("/{fileName:.+}")
    public Response getFrontendStaticFile(@PathParam("fileName") String fileName) throws IOException {
        InputStream requestedFileStream;
        if(fileName.contains(".")) {
            requestedFileStream = GatewayResource.class.getResourceAsStream("/frontend/" + fileName);
        } else {
            requestedFileStream = GatewayResource.class.getResourceAsStream("/frontend/" + fileName + ".html");
        }

        final InputStream inputStream = requestedFileStream != null ?
                requestedFileStream :
                GatewayResource.class.getResourceAsStream(FALLBACK_RESOURCE);

        final StreamingOutput streamingOutput = outputStream -> inputStream.transferTo(outputStream);

        String contentType;
        if(fileName.contains("js")) {
            contentType = "application/javascript";
        } else {
            contentType = URLConnection.guessContentTypeFromName(fileName);
            // contentType = URLConnection.guessContentTypeFromStream(inputStream);
        }

        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(900);

        return Response
                .ok(streamingOutput)
                .cacheControl(cacheControl)
                .type(contentType)
                .build();
    }
}
