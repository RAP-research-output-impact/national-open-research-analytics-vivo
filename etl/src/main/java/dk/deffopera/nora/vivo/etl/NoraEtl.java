package dk.deffopera.nora.vivo.etl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;

import dk.deffopera.nora.vivo.etl.datasource.DataSource;
import dk.deffopera.nora.vivo.etl.datasource.connector.GraphClearer;
import dk.deffopera.nora.vivo.etl.datasource.connector.dimensions.DimensionsByDoiConnector;
import dk.deffopera.nora.vivo.etl.datasource.connector.dimensions.DimensionsConnector;
import dk.deffopera.nora.vivo.etl.util.SparqlEndpoint;
import dk.deffopera.nora.vivo.etl.util.SparqlEndpointParams;

public class NoraEtl {

    private static final Log log = LogFactory.getLog(NoraEtl.class);
    
    public static void main(String[] args) {
        if(args.length < 3) {
            System.out.println("Usage: " 
                    + "dimensions|dimensionsByDoi|clearGraph outputfile dimensionsUsername=<username> dimensionsPassword=<password> " 
                    + "[sourceEndpointURI= sourceEndpointUsername= sourceEndpointPassword=] "
                    + "[dataDir=] [endpointURI= endpointUpdateURI= username= password=] [authUsername= authPassword=] [graphURI=] "
                    + "[limit]");
            return;
        }
        List<String> queryTerms = new LinkedList<String>(
                Arrays.asList(args));
        String connectorName  = queryTerms.remove(0);
        String outputFileName = queryTerms.remove(0);
        int limit = getLimit(queryTerms);
        if(limit < Integer.MAX_VALUE) {
            log.info("Retrieving a limit of " + limit + " records");
        }
        SparqlEndpointParams endpointParameters = getEndpointParams(queryTerms);
        DataSource connector = getConnector(connectorName, queryTerms);
        connector.getConfiguration().getParameterMap().put("dataDir", getParameter(queryTerms, "dataDir"));
        //connector.getConfiguration().setQueryTerms(queryTerms);
        connector.getConfiguration().setEndpointParameters(endpointParameters);
        connector.getConfiguration().setLimit(limit);
        connector.getConfiguration().setResultsGraphURI(getParameter(queryTerms, "graphURI"));
        if(endpointParameters == null) {
            log.info("EndpointParameters is null");
        } 
        log.info("GraphURI is " + connector.getConfiguration().getResultsGraphURI());
        long start = System.currentTimeMillis();
        connector.run();
        Model result = connector.getResult();
        log.info(((System.currentTimeMillis() - start) / 1000) + " s to run");
        if(result == null) {
            log.warn("result is null");
        } else {
            File outputFile = new File(outputFileName);
            FileOutputStream fos;
            try {
                log.info("Writing output to " + outputFile);
                fos = new FileOutputStream(outputFile);
                result.write(fos, "N3");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static DataSource getConnector(String connectorName, List<String> queryTerms) {
        DataSource connector = null;
        if("clearGraph".equals(connectorName)) {
            connector = new GraphClearer();
        } else if ("dimensions".equals(connectorName)) {
            connector = new DimensionsConnector(
                    getParameter(queryTerms, "dimensionsUsername"), 
                    getParameter(queryTerms, "dimensionsPassword"),
                    getParameter(queryTerms, "mongoServer"),
                    getParameter(queryTerms, "mongoPort"),
                    getParameter(queryTerms, "mongoCollection"),
                    getParameter(queryTerms, "mongoUsername"),
                    getParameter(queryTerms, "mongoPassword"));
        } else if("dimensionsByDoi".equals(connectorName)) {
            String dimensionsUsername = getParameter(queryTerms, "dimensionsUsername"); 
            String dimensionsPassword = getParameter(queryTerms, "dimensionsPassword");
            SparqlEndpointParams params = new SparqlEndpointParams();
            params.setEndpointURI(getParameter(queryTerms, "sourceEndpointURI"));
            params.setUsername(getParameter(queryTerms, "sourceEndpointUsername"));
            params.setPassword(getParameter(queryTerms, "sourceEndpointPassword"));
            connector = new DimensionsByDoiConnector(
                    dimensionsUsername, dimensionsPassword, new SparqlEndpoint(params));
        } else {
            throw new RuntimeException("Connector not found: " 
                    + connectorName);
        }        
        return connector;
    }
    
    private static int getLimit(List<String> queryTerms) {
        if(!queryTerms.isEmpty()) {
            String possibleLimit = queryTerms.get(queryTerms.size() - 1);            
            try {
                int limit = Integer.parseInt(possibleLimit, 10);
                queryTerms.remove(queryTerms.size() - 1);
                return limit;
            } catch (NumberFormatException e) {
                // no limit argument present; move on
            }
        }
        return Integer.MAX_VALUE;
    }
    
    private static String getDataDir(List<String> queryTerms) {
        if(!queryTerms.isEmpty() && queryTerms.get(0).startsWith("dataDir=")) {
            return queryTerms.get(0).substring("dataDir=".length());            
        } else {
            return null;
        }
    }
    
    private static SparqlEndpointParams getEndpointParams(List<String> queryTerms) {
        SparqlEndpointParams params = new SparqlEndpointParams();
        int toRemove = 0;
        for (String queryTerm : queryTerms) {
            if(queryTerm.startsWith("endpointURI=")) {
                params.setEndpointURI(queryTerm.substring("endpointURI=".length()));
                toRemove++;
            } else if (queryTerm.startsWith("endpointUpdateURI=")) {
                params.setEndpointUpdateURI(queryTerm.substring("endpointUpdateURI=".length()));
                toRemove++;
            } else if (queryTerm.startsWith("username=")) {
                params.setUsername(queryTerm.substring("username=".length()));
                toRemove++;
            } else if (queryTerm.startsWith("password=")) {
                params.setPassword(queryTerm.substring("password=".length()));
                toRemove++;
            } else if (queryTerm.startsWith("authUsername=")) {
                params.setHttpBasicAuthUsername(queryTerm.substring("authUsername=".length()));
                toRemove++;
            } else if (queryTerm.startsWith("authPassword=")) {
                params.setHttpBasicAuthPassword(queryTerm.substring("authPassword=".length()));
                toRemove++;
            } 
        }
        for(int i = 0; i < toRemove; i++) {
            queryTerms.remove(0);
        }
        if(toRemove > 0) {
            return params;    
        } else {
            return null;
        }        
    }
    
    private static String getParameter(List<String> queryTerms, String paramName) {
        if(!queryTerms.isEmpty() && queryTerms.get(0).startsWith(paramName + "=")) {
            String graphParam = queryTerms.remove(0);
            return graphParam.substring((paramName + "=").length());
        } else {
            return null;
        }
    }
    
}
