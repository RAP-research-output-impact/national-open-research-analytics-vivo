package dk.deffopera.nora.vivo.etl.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class HttpUtils {

    private HttpClient httpClient;
    private static final Log log = LogFactory.getLog(HttpUtils.class);
    
    public HttpUtils() {
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
        defaultHttpClient.setRedirectStrategy(new LaxRedirectStrategy());
        this.httpClient = defaultHttpClient;
        // TODO refactor and modernize HttpClient creation
        int timeout = 600; // seconds
        HttpParams httpParams = this.httpClient.getParams();
        httpParams.setParameter(
          CoreConnectionPNames.CONNECTION_TIMEOUT, timeout * 1000);
        httpParams.setParameter(
          CoreConnectionPNames.SO_TIMEOUT, timeout * 1000);
        httpParams.setParameter(
          ClientPNames.CONN_MANAGER_TIMEOUT, new Long(timeout * 1000));
    }
    
    public HttpClient getHttpClient() {
        return this.httpClient;
    }
    
    public String getHttpResponse(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept-charset", "utf-8");
        HttpResponse response;
        try {
            response = httpClient.execute(get);
        } catch (Exception e) {
            try {
                Thread.sleep(2000);
                response = httpClient.execute(get);
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            } catch (Exception e2) {
                try {
                    Thread.sleep(4000);
                    response = httpClient.execute(get);
                } catch (InterruptedException e3) {
                    throw new RuntimeException(e3);
                }
            }
        }
        try {
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } finally {
            EntityUtils.consume(response.getEntity());
        }   
    }
    
    public String getHttpPostResponse(String url, String payload,
            String contentType) {
        return getHttpPostResponse(url, payload, contentType, null);
    }
    
    public String getHttpPostResponse(String url, String payload, 
            String contentType, String authToken) {
        HttpPost post = new HttpPost(url);
        post.addHeader("content-type", contentType);
        if(authToken != null) {
            post.setHeader("Authorization", "JWT " + authToken);
        }
        try {
            ContentType ctype = ContentType.create(contentType, "UTF-8");
            post.setEntity(new StringEntity(payload, ctype));
        } catch (Exception e) {
            log.warn("Unable to use content type " + contentType +
                    ".  Using default UTF-8 StringEntity");
            post.setEntity(new StringEntity(payload, "UTF-8"));
        }
        try {
            HttpResponse response = httpClient.execute(post);
            try {
                return EntityUtils.toString(response.getEntity());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                EntityUtils.consume(response.getEntity());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 

    }
    
    public Model getRDFLinkedDataResponse(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "application/rdf+xml");
        HttpResponse response = httpClient.execute(get);
        try {
            byte[] entity = EntityUtils.toByteArray(response.getEntity());
            Model model = ModelFactory.createDefaultModel();
            model.read(new ByteArrayInputStream(entity), null, "RDF/XML");
            return model;
        } finally {
            EntityUtils.consume(response.getEntity());
        }
    }
    
    
}
