package dk.deffopera.osrap.vivo.etl.datasource.connector.dimensions;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dk.deffopera.osrap.vivo.etl.datasource.DataSource;
import dk.deffopera.osrap.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.osrap.vivo.etl.datasource.connector.ConnectorDataSource;
import dk.deffopera.osrap.vivo.etl.util.HttpUtils;
import dk.deffopera.osrap.vivo.etl.util.JsonToXMLConverter;
import dk.deffopera.osrap.vivo.etl.util.RdfUtils;
import dk.deffopera.osrap.vivo.etl.util.XmlToRdf;

public class DimensionsConnector extends ConnectorDataSource 
        implements DataSource {

    private static final String DIMENSIONS_API = "https://app.dimensions.ai/api/";
    private static final String ABOX = "http://vivo.deffopera.dk/individual/";
    private static final String SPARQL_RESOURCE_DIR = "/dimensions/sparql/";
    private static final long REQUEST_INTERVAL = 2000; // ms
       
    private static final Log log = LogFactory.getLog(DimensionsConnector.class);
    private HttpUtils httpUtils = new HttpUtils();
    private String token;
        
    public DimensionsConnector(String username, String password) {
        this.token = getToken(username, password);    
    }
    
    private String getToken(String username, String password) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("username", username);
        json.put("password", password);
        String tokenJson = httpUtils.getHttpPostResponse(
                DIMENSIONS_API + "auth.json", json.toString(), "application/json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tokenObj;
        try {
            tokenObj = mapper.readTree(tokenJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tokenObj.get("token").textValue();
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new DimensionsIterator(this.token);
    }
    
    private class DimensionsIterator implements IteratorWithSize<Model> {

        private static final int RESULTS_PER_REQUEST = 200;
        private String token;
        private final String[] sources = {
                "publications", "grants", "patents", "clinical_trials"};
        // number of requests to be made for each data source (pubs, grants, etc.)
        private int[] totals = new int[4];
        private int requestCount;
        private Model firstIteration;
        
        public DimensionsIterator(String token) {
            this.token = token;
            for(int i = 0; i < totals.length; i++) {
                // run a request for each data source at least once, then set
                // the actual total results based on the number returned
                totals[i] = 1;
            } 
            try {
                firstIteration = getResults();
                setTotals();
            } catch (InterruptedException e) {
                log.error(e, e);
                throw new RuntimeException(e);
            }
        }
        
        private void setTotals() {
            for(int i = 0; i < sources.length; i++) {
                StmtIterator sit = firstIteration.listStatements(
                        null, firstIteration.getProperty(
                                XmlToRdf.GENERIC_NS + sources[i]), (RDFNode) null);
                if(sit.hasNext()) {
                    Statement stmt = sit.next();
                    Resource r = stmt.getSubject();
                    Statement stats = r.getProperty(firstIteration.getProperty(
                            XmlToRdf.GENERIC_NS + "_stats"));
                    if(stats != null) {
                        RDFNode statsNode = stats.getObject();
                        if(statsNode.isResource()) {
                            Resource statsRes = statsNode.asResource();
                            Statement totalCount = statsRes.getProperty(
                                    firstIteration.getProperty(
                                            XmlToRdf.GENERIC_NS + "total_count"));
                            if(totalCount != null) {
                                RDFNode totalCountNode = totalCount.getObject();
                                if(totalCountNode.isLiteral()) {
                                    try {
                                        int totalCountInt = totalCountNode.asLiteral().getInt();
                                        log.info(totalCountInt + " total " + sources[i]);
                                        totals[i] = totalCountInt / RESULTS_PER_REQUEST + 1;
                                    } catch (Exception e) {
                                        log.error(e, e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        @Override
        public boolean hasNext() {
            for(int i = 0; i < totals.length; i++) {
               if(requestCount < totals[i]) {
                   return true;
               }
            }
            return false;
        }

        @Override
        public Model next() {
            Model results;
            if(requestCount == 0) {                
                results = firstIteration;
                firstIteration = null;
            } else {
                try {
                    results = getResults();
                } catch (InterruptedException e) {
                    log.error(e, e);
                    throw new RuntimeException(e);
                }
            }
            requestCount++;
            return results;
        }

        @Override
        public Integer size() {
            int size = 0;
            for(int i = 0; i < totals.length; i++) {
                if(totals[i] > size) {
                    size = totals[i];
                }
            }
            return size;
        }

        @Override
        public void close() {
            // no API method for logging out; nothing to do for now
        }
        
        private Model getResults() throws InterruptedException {
            Model model = ModelFactory.createDefaultModel();
            if(requestCount < totals[0]) {
                model.add(toRdf(getPubs(this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            if(requestCount < totals[1]) {
                model.add(toRdf(getGrants(this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            if(requestCount < totals[2]) {
                model.add(toRdf(getPatents(this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            if(requestCount < totals[3]) {
                model.add(toRdf(getClinicalTrials(this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            return model;
        }
        
        long lastRequest = 0;
        
        private String getDslResponse(String dslQuery, String token) 
                throws InterruptedException {
            long now = System.currentTimeMillis();
            long toWait = REQUEST_INTERVAL - (now - lastRequest);        
            if(toWait > 0) {
                Thread.sleep(toWait);
            }
            lastRequest = System.currentTimeMillis();
            return httpUtils.getHttpPostResponse(DIMENSIONS_API + "dsl.json",
                    dslQuery, "application/json", token);        
        }
        
        private Model toRdf(String data) {                
            try {
                //ObjectMapper mapper = new ObjectMapper();
                //JsonNode dataObj;
                //dataObj = mapper.readTree(data);
                //System.out.println(mapper.writerWithDefaultPrettyPrinter()
                //        .writeValueAsString(dataObj));
                JsonToXMLConverter json2xml = new JsonToXMLConverter();
                XmlToRdf xml2rdf = new XmlToRdf();
                RdfUtils rdfUtils = new RdfUtils();
                String xml = json2xml.convertJsonToXml(data);
                Model rdf = xml2rdf.toRDF(xml);
                rdf = rdfUtils.renameBNodes(rdf, ABOX + "n", rdf);
                rdf = renameByIdentifier(rdf, rdf.getProperty(
                        XmlToRdf.GENERIC_NS + "id"), ABOX, "");
                return rdf;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        private String getGrants(String token, int skip) throws InterruptedException {
            String queryStr = "search grants where"
                    + " (start_year >= 2014 and start_year <= 2017 and research_orgs.id = \"grid.5170.3\")"
                    + " return grants[active_year + end_date + funders + id + project_num + start_date + start_year + title + abstract + funding_eur + grant_number + investigator_details + research_orgs]"
                    + " limit 200 skip " + skip;
            return getDslResponse(queryStr, token);
        }
        
        private String getPubs(String token, int skip) throws InterruptedException {
            String queryStr = "search publications where"
                    + " (year >= 2014 and year <= 2017 and research_orgs.id = \"grid.5170.3\" and type in [\"article\", \"chapter\", \"proceeding\"])"
                    //+ " return publications"
                    + " return publications[id + type + title + authors + doi + pmid + pmcid + date + year + mesh_terms + journal + issn + volume + issue]"
                    + " limit 200 skip " + skip;
            return getDslResponse(queryStr, token);
        }
        
        private String getPatents(String token, int skip) throws InterruptedException {
            String queryStr = "search patents where"
                    + " (granted_year >= 2014 and granted_year <= 2017 and assignees.id = \"grid.5170.3\")"
                    + " return patents[assignees + granted_year + id + title + inventor_names + funders + abstract + associated_grant_ids]"
                    + " limit 200 skip " + skip;
            return getDslResponse(queryStr, token);
        }
        
        private String getClinicalTrials(String token, int skip) throws InterruptedException {
            String queryStr = "search clinical_trials where"
                    + " (active_years >= 2014 and active_years <= 2017 and organizations.id = \"grid.5170.3\")"
                    + " return clinical_trials[id + title + abstract + active_years + associated_grant_ids + date + researchers + organizations + funders]"
                    + " limit 200 skip " + skip;
            return getDslResponse(queryStr, token);
        }
        
        protected Model renameByIdentifier(Model m, Property identifier, 
                String namespace, String localNamePrefix) {
            Map<Resource, String> idMap = new HashMap<Resource, String>();
            StmtIterator sit = m.listStatements(null, identifier, (RDFNode) null);
            while(sit.hasNext()) {
                Statement stmt = sit.next();
                if(stmt.getObject().isLiteral()) {
                    idMap.put(stmt.getSubject(), 
                            //stripNonWordChars(stmt.getObject().asLiteral().getLexicalForm()));
                            stmt.getObject().asLiteral().getLexicalForm());
                }
            }
            for(Resource res : idMap.keySet()) {
                ResourceUtils.renameResource(
                        res, namespace + localNamePrefix + idMap.get(res));
            }
            return m;
        }
        
        protected String stripNonWordChars(String value) {
            if(value == null) {
                return value;
            } else {
                return value.replaceAll("\\W", "");
            }
        }

    }

    @Override
    protected Model filter(Model model) {
        if(true) {
            return filterGeneric(model);
        } else {
            return model;
        }
    }

    @Override
    protected Model mapToVIVO(Model model) {
        List<String> queries = Arrays.asList(
                //"050-orcidId.rq",
                "100-publicationTypes.rq",
                "110-publicationMetadata.rq",
                "120-publicationDate.rq",
                "130-publicationJournal.rq",
                "140-publicationAuthorship.rq"
                );
        for(String query : queries) {
            log.debug("Executing query " + query);
            long pre = model.size();
            log.debug("Pre-query model size: " + pre);
            construct(SPARQL_RESOURCE_DIR + query, model, ABOX + getPrefixName() + "-");
            log.debug("Post-query model size: " + model.size());
            if(model.size() - pre == 0 ) {
                log.info(query + " constructed no triples");
            }
        }
        model = renameByIdentifier(model, model.getProperty(
                XmlToRdf.GENERIC_NS + "person_researcher_id"), ABOX, "");
        //model = renameByIdentifier(model, model.getProperty(
        //        XmlToRdf.GENERIC_NS + "person_orcidStr"), ABOX, "orcid-");
        queries = Arrays.asList(         
                "150-publicationAuthor.rq",
                "160-publicationAuthorPosition.rq"
                );
        for(String query : queries) {
            log.debug("Executing query " + query);
            long pre = model.size();
            log.debug("Pre-query model size: " + pre);
            construct(SPARQL_RESOURCE_DIR + query, model, ABOX + getPrefixName() + "-");
            log.debug("Post-query model size: " + model.size());
            if(model.size() - pre == 0 ) {
                log.info(query + " constructed no triples");
            }
        }
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions";
    }
    
}



