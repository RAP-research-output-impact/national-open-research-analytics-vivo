package dk.deffopera.nora.vivo.etl.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.resultset.ResultSetException;

public class SparqlEndpoint {

    // writing too many triples at once to VIVO seems to result in 403 errors
    private static final int CHUNK_SIZE = 25000;
    
    private static final Log log = LogFactory.getLog(SparqlEndpoint.class);
    
    private SparqlEndpointParams endpointParams;
    private String authorizationHeaderValue;
    
    private static final DefaultHttpClient httpClient;

    static {
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        cm.setDefaultMaxPerRoute(50);
        cm.setMaxTotal(300);
        httpClient = new DefaultHttpClient(cm);
        // TODO refactor and modernize HttpClient creation
        int timeout = 900; // seconds
        HttpParams httpParams = httpClient.getParams();
        httpParams.setParameter(
          CoreConnectionPNames.CONNECTION_TIMEOUT, timeout * 1000);
        httpParams.setParameter(
          CoreConnectionPNames.SO_TIMEOUT, timeout * 1000);
        httpParams.setParameter(
          ClientPNames.CONN_MANAGER_TIMEOUT, new Long(timeout * 1000));
    }
    
    public SparqlEndpoint(SparqlEndpointParams params) {
        this.endpointParams = params; 
        if(params.getHttpBasicAuthUsername() != null 
                && params.getHttpBasicAuthPassword() != null) {
            String authenticateHeaderValue = params.getHttpBasicAuthUsername() 
                    + ":" + params.getHttpBasicAuthPassword();        
            this.authorizationHeaderValue = DatatypeConverter.printBase64Binary(
                    authenticateHeaderValue.getBytes());
        }
    }
       
    public String getURI() {
        return this.endpointParams.getEndpointURI();
    }
    
    public ResultSet getResultSet(String queryStr) {
        try {
            //System.out.println(this.endpointParams.getEndpointURI());
            //QueryExecution qe = QueryExecutionFactory.sparqlService(
            //        this.endpointParams.getEndpointURI(), queryStr);
            //return qe.execSelect();
            // avoid strange Jena bugs:
            return getResultSetWithoutJena(queryStr);
        } catch (QueryParseException qpe) {
            throw new RuntimeException("Unable to parse query:\n" + queryStr, qpe);
        }
    }
    
    private String getSparqlQueryResponse(String queryStr, String contentType) {
        try {
            String uri = endpointParams.getEndpointURI();
            //queryStr = URLEncoder.encode(queryStr, "UTF-8");
            URIBuilder uriB = new URIBuilder(uri);
            uriB.addParameter("query", queryStr);
            uriB.addParameter("email", endpointParams.getUsername());
            uriB.addParameter("password", endpointParams.getPassword());
            String uriWithParams = uriB.build().toString();
            HttpGet get = new HttpGet(uriWithParams);            
            log.debug("Request URI " + uriWithParams);
            get.addHeader("Accept", contentType);
            if(this.authorizationHeaderValue != null) {
                get.addHeader("Authorization", "Basic " + this.authorizationHeaderValue);
            }
            long start = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(get);
            try {
                String content = stringFromInputStream(
                        response.getEntity().getContent());
                int status = response.getStatusLine().getStatusCode();
                if(status >= 300 && status < 400) {
                    throw new UnsupportedOperationException("SPARQL endpoint " +
                            "returned " + status + " redirect status. " +
                            "Redirection is not supported.");
                } else if (status >= 400) {
		    log.error("Response 400: \n" + content);
                    throw new RuntimeException("Status " + status + 
                            " from SPARQL endpoint: \n" + content);
                } else {
                    return content;
                }
            } finally {
                EntityUtils.consume(response.getEntity());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private ResultSet getResultSetWithoutJena(String queryStr) {
        String content = getSparqlQueryResponse(queryStr, 
                "application/sparql-results+xml");
        try {
            ResultSet rs = ResultSetFactory.fromXML(content);
            return rs;
        } catch (ResultSetException e) {
            throw new RuntimeException("Unable to create result " +
                    "set from response content: " + content);
        }
    }
    
    /**
     * Uses the SPARQL UPDATE endpoint to write a model to VIVO.
     * The model is split into individual requests of size CHUNK_SIZE to 
     * avoid error responses from VIVO.
     * @param model
     */
    public void writeModel(Model model, String graphURI) {
        List<Model> modelChunks = new ArrayList<Model>();
        StmtIterator sit = model.listStatements();
        int i = 0;
        Model currentChunk = null;
        while(sit.hasNext()) {
            if(i % CHUNK_SIZE == 0) {
                if (currentChunk != null) {
                    modelChunks.add(currentChunk);
                }
                currentChunk = ModelFactory.createDefaultModel();
            }
            currentChunk.add(sit.nextStatement());
            i++;
        }
        if (currentChunk != null) {
            modelChunks.add(currentChunk);
        }
        long total = model.size();
        long written = 0;
        log.info("Writing " + total + " new statements to " + graphURI);
        for (Model chunk : modelChunks) {
            writeChunk(chunk, graphURI);
            written += chunk.size();
            if(log.isDebugEnabled()) {
                int percentage = Math.round(
                        ((float)written/ (float)total) * 100);
                log.debug("\t" + written + " out of " + total + 
                        " triples written (" + percentage + "%)");
            }
        }
    }
    
    /**
     * Uses the SPARQL UPDATE endpoint to remove a model to VIVO.
     * The model is split into individual requests of size CHUNK_SIZE to 
     * avoid error responses from VIVO.
     * @param model
     */
    public void deleteModel(Model model, String graphURI) {
        List<Model> modelChunks = new ArrayList<Model>();
        StmtIterator sit = model.listStatements();
        int i = 0;
        Model currentChunk = null;
        while(sit.hasNext()) {
            if(i % CHUNK_SIZE == 0) {
                if (currentChunk != null) {
                    modelChunks.add(currentChunk);
                }
                currentChunk = ModelFactory.createDefaultModel();
            }
            currentChunk.add(sit.nextStatement());
            i++;
        }
        if (currentChunk != null) {
            modelChunks.add(currentChunk);
        }
        long total = model.size();
        long written = 0;
        log.debug("Deleting " + total + " statements from VIVO");
        for (Model chunk : modelChunks) {
            deleteChunk(chunk, graphURI);
            written += chunk.size();
            if(log.isDebugEnabled()) {
                int percentage = Math.round(
                        ((float)written/ (float)total) * 100);
                log.debug("\t" + written + " out of " + total + 
                        " triples deleted (" + percentage + "%)");
            }
        }
    }

    public void update(String updateString) {
        HttpPost post = new HttpPost(endpointParams.getEndpointUpdateURI());
        post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair(
                    "email", endpointParams.getUsername()));
            nameValuePairs.add(new BasicNameValuePair(
                    "password", endpointParams.getPassword()));
            nameValuePairs.add(new BasicNameValuePair(
                    "update", updateString));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(
                    nameValuePairs, Charsets.UTF_8);
            post.setEntity(entity);
            if(this.authorizationHeaderValue != null) {
                post.setHeader("Authorization", "Basic " + this.authorizationHeaderValue);
            }
            HttpResponse response = httpClient.execute(post);
            try {
                String content = stringFromInputStream(
                        response.getEntity().getContent());
                int result = response.getStatusLine().getStatusCode();
                if (result == 403) {
                    throw new RuntimeException(
                            "VIVO forbade update with the supplied " +
                            "username and password.  Update unsuccessful.");
                } else if (result > 200) {
                    log.error("Error code " + result + " processing update \n" + updateString);
		    log.error(content);
                    throw new RuntimeException("VIVO responded with error code " + 
                            result + ". Update unsuccessful. ");
                }
            } finally {
                EntityUtils.consume(response.getEntity());
            }
        } catch (ClientProtocolException e) {
            throw new RuntimeException("Unable to connect to VIVO via HTTP " +
                    "to perform update", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to VIVO to perform " +
                    "update", e);
        }
    }
    
    /**
     * Uses the SPARQL UPDATE endpoint to write a model to VIVO
     * @param chunk
     */
    private void writeChunk(Model chunk, String graphURI) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        chunk.write(out, "N-TRIPLE");
        StringBuffer reqBuff = new StringBuffer();
        reqBuff.append("INSERT DATA { GRAPH <" + graphURI + "> { \n");
        try {
            reqBuff.append(out.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        reqBuff.append(" } } \n");
        String reqStr = reqBuff.toString();     
        long startTime = System.currentTimeMillis();
        update(reqStr);
        log.debug("\t" + (System.currentTimeMillis() - startTime) / 1000 + 
                " seconds to insert " + chunk.size() + " triples");
    }
    
    /**
     * Uses the SPARQL UPDATE endpoint to delete a model from VIVO
     * @param chunk
     */
    private void deleteChunk(Model chunk, String graphURI) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        chunk.write(out, "N-TRIPLE");
        StringBuffer reqBuff = new StringBuffer();
        reqBuff.append("DELETE DATA { GRAPH <" + graphURI + "> { \n");
        reqBuff.append(out);
        reqBuff.append(" } } \n");
        String reqStr = reqBuff.toString();     
        long startTime = System.currentTimeMillis();
        update(reqStr);
        log.debug("\t" + (System.currentTimeMillis() - startTime) / 1000 + 
                " seconds to insert " + chunk.size() + " triples");
    }
    
    private String stringFromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    public Model construct(String query) {
        Model m = ModelFactory.createDefaultModel();
        try {
            String ttl = getSparqlQueryResponse(query, "text/turtle");
            m.read(new StringReader(ttl), null, "TURTLE");
        } catch (Exception e) {
            log.warn(e, e);
            String rdfxml = getSparqlQueryResponse(query, "application/rdf+xml");
            m.read(new StringReader(rdfxml), null, "RDF/XML");
        }
        return m;
    }
    
    /**
     * An alternative to CLEAR, which is very inefficient with VIVO-based 
     * endpoints
     * @param graphURI
     */
    public void clearGraph(String graphURI) {
        int batchSize = 10000;
        log.info("Clearing graph " + graphURI + " in batches of " + batchSize + 
                " triples");
        // Get triple count
//        String countSelect = "SELECT (COUNT(*) AS ?count) WHERE { \n"
//                + "  GRAPH <" + graphURI + "> { ?s ?p ?o } \n"
//                + "}";
//        ResultSet countRs = getResultSet(countSelect);
//        QuerySolution qsoln = countRs.next();
//        int count = qsoln.getLiteral("count").getInt();
//        int iterations = (count / batchSize) + 1;
        boolean done = false;
//	for(int i = 0; i < iterations; i++) {
        while(!done) {
	    log.info("Acquiring chunk");
            String chunkConstruct = "CONSTRUCT { ?s ?p ?o } WHERE { \n"
                    + "  GRAPH <" + graphURI + "> { ?s ?p ?o } \n" 
                    + "} LIMIT " + batchSize;
            Model chunk = this.construct(chunkConstruct);
	    log.info("Chunk acquired");
            this.deleteModel(chunk, graphURI);
	    log.info("Chunk deleted");
	    if(chunk.size() < batchSize) {
                done = true;
	    }
        }
        log.info("Calling final clear");
        update("CLEAR GRAPH <" + graphURI + ">");
    }
    
//    /**
//     * An alternative to CLEAR, which is very inefficient with VIVO-based 
//     * endpoints
//     * @param graphURI
//     */
//    public void clearGraph(String graphURI) {
//        // retrieve individual URIs in batches of 1000
//        int batchSize = 20000;
//        log.info("Clearing graph " + graphURI + " in batches of " + batchSize + 
//                " individuals");
//        boolean getNextBatch = true;
//        do {
//            String individualsBatch = "SELECT DISTINCT ?s WHERE { \n" +
//                    "    GRAPH <" + graphURI + "> { \n" +
//                    "        ?s ?p ?o \n" +
//                    "    } \n" +
//                    "} LIMIT " + batchSize;
//            log.debug(individualsBatch);
//            ResultSet rs = getResultSet(individualsBatch);
//            getNextBatch = rs.hasNext();
//            StringBuilder deletion = new StringBuilder();
//            while(rs.hasNext()) {
//                QuerySolution sol = rs.next();
//                Resource s = sol.getResource("s");
//                if(s.isURIResource()) {
//                    deletion.append("DELETE { \n")
//                    .append("    GRAPH<").append(graphURI).append(">")
//                    .append(" { <").append(s.getURI()).append("> ?p ?o } \n")
//                    .append("} WHERE { \n")
//                    .append("    GRAPH<").append(graphURI).append(">")
//                    .append(" { <").append(s.getURI()).append("> ?p ?o } \n")
//                    .append("}; \n");
//                }
//            }
//            String deletionStr = deletion.toString();
//            log.debug(deletionStr);
//            if(deletionStr.isEmpty()) {
//                getNextBatch = false;
//            } else {
//                try {
//                    update(deletionStr);    
//                } catch (Exception e) {
//                    log.info("Failed to delete batch of triples", e);
//                }
//            }            
//        } while (getNextBatch);
//        // TODO check that count is decreasing after each N batches, otherwise 
//        // terminate loop
//        // Finally, issue the regular CLEAR to flush out anything remaining
//        // (e.g. blank nodes)
//        //log.info("Clearing graph " + graphURI);
//        log.info("Calling final clear");
//        update("CLEAR GRAPH <" + graphURI + ">");
//    }
    
    /**
     * An alternative to CLEAR, which is very inefficient with VIVO-based 
     * endpoints
     * @param graphURI
     */
    public void clear(String graphURI) {
        log.info("Deleting contents from graph " + graphURI);
        Model toDelete = this.construct("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <" 
                + graphURI + "> { ?s ?p ?o } }");
        deleteModel(toDelete, graphURI);
        log.info("Calling final clear");
        update("CLEAR GRAPH <" + graphURI + ">");
    }
    
}
