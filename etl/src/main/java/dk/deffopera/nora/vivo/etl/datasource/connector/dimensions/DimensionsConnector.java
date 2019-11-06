package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dk.deffopera.nora.vivo.etl.datasource.DataSource;
import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.datasource.connector.ConnectorDataSource;
import dk.deffopera.nora.vivo.etl.util.HttpUtils;
import dk.deffopera.nora.vivo.etl.util.JsonToXMLConverter;
import dk.deffopera.nora.vivo.etl.util.RdfUtils;
import dk.deffopera.nora.vivo.etl.util.XmlToRdf;

public class DimensionsConnector extends ConnectorDataSource 
        implements DataSource {

    private static final String DIMENSIONS_API = "https://app.dimensions.ai/api/";
    private static final String ABOX = "http://vivo.deffopera.dk/individual/";
    private static final String SPARQL_RESOURCE_DIR = "/dimensions/sparql/";
    private static final long REQUEST_INTERVAL = 2000; // ms
    private static final Map<String, String> ugrids = new HashMap<String, String>();
    private static final Map<String, String> hgrids = new HashMap<String, String>();
    
    static {
        // TODO load dynamically from a CSV
        ugrids.put("DTU", "grid.5170.3");
        ugrids.put("Arhus University", "grid.7048.b");
        ugrids.put("Aalborg University", "grid.5117.2");
        ugrids.put("University of Southern Denmark", "grid.10825.3e");
        ugrids.put("Copenhagen Business School", "grid.4655.2");
        ugrids.put("IT University", "grid.32190.39");
        ugrids.put("Roskilde University", "grid.11702.35");
        ugrids.put("Copenhagen University", "grid.5254.6");
        hgrids.put("Rigshospitalet", "grid.475435.4");
        hgrids.put("Aarhus University Hospital",  "grid.154185.c");
        hgrids.put("Bispebjerg Hospital", "grid.411702.1");
        hgrids.put("Hvidovre Hospital", "grid.411905.8");
        hgrids.put("Copenhagen University Hospital", "grid.4973.9");   
        hgrids.put("Herlev Hospital", "grid.411900.d");
        hgrids.put("Glostrup Hospital", "grid.411719.b");
        hgrids.put("Gentofte Hospital", "grid.411646.0");
        hgrids.put("Frederiksberg Hospital", "grid.415046.2");
        hgrids.put("Vejle Sygehus", "grid.417271.6");
        hgrids.put("Nordsjællands Hospital", "grid.414092.a");
        hgrids.put("Odense University Hospital", "grid.7143.1");
        hgrids.put("Regionshospitalet Viborg", "grid.416838.0");
        hgrids.put("Marselisborg Hospital", "grid.477140.1");
        hgrids.put("Sydvestjysk Sygehus", "grid.414576.5");
        hgrids.put("Kolding Hospital", "grid.415434.3");
        hgrids.put("Aalborg Hospital", "grid.27530.33");
        hgrids.put("Amager Hospital", "grid.413660.6");
        hgrids.put("Roskilde Sygehus", "grid.416059.f");
        hgrids.put("Regionshospitalet Herning", "grid.414058.c");
        hgrids.put("Regionshospital Holstebro", "grid.414304.6");
        hgrids.put("Svendborg Sygehus", "grid.416768.a");
        hgrids.put("Køge Hospital", "grid.416055.3");
        hgrids.put("Regionshospitalet Randers", "grid.415677.6");
        hgrids.put("Næstved Sygehus", "grid.416369.f");
        hgrids.put("Hosrsens Hospital", "grid.414334.5");
        hgrids.put("Sygehus Sønderjylland", "grid.416811.b");
        hgrids.put("Nykøbing Hospital", "grid.413717.7");
        hgrids.put("Holbæk Sygehus", "grid.414289.2");
        hgrids.put("Sygehus Vendsyssel", "grid.414274.0");
        hgrids.put("Skive Hospital", "grid.416035.5");
        hgrids.put("Sygehus Lillebælt", "grid.459623.f");
        hgrids.put("Fredericia Sygehus", "grid.415040.4");
        hgrids.put("Slagelse Hospital", "grid.452905.f");
        hgrids.put("Regional Hospital West Jutland", "grid.452681.c");
        hgrids.put("Zealand University Hospital", "grid.476266.7");
        hgrids.put("Regionshospitalet Silkeborg", "grid.477812.f");
        hgrids.put("Rinsted Sygehus", "grid.477756.0");
        hgrids.put("Regionshospital Nordjylland", "grid.487445.e");
    }
       
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
        private Map<String, int[]> totals = new HashMap<String, int[]>();
        
        List<String> grids = new ArrayList<String>();
        private int requestCount;       
        private Model firstIteration;
        
        public DimensionsIterator(String token) {
            this.token = token;
            grids.addAll(ugrids.values());
            grids.addAll(hgrids.values());
            log.info(grids.size() + " grids");
            for(String grid : grids) {
                int[] total = new int[4];
                for(int i = 0; i < total.length; i++) {
                    total[i] = 1;
                }
                totals.put(grid, total);
            }
            try {
                firstIteration = ModelFactory.createDefaultModel();
                for(String grid : grids) {
                    Model gridModel = getResults(grid);
                    setTotals(grid, gridModel);    
                    firstIteration.add(gridModel);                    
                }                                
            } catch (InterruptedException e) {
                log.error(e, e);
                throw new RuntimeException(e);
            }
        }
        
        private void setTotals(String grid, Model model) {
            for(int i = 0; i < sources.length; i++) {
                StmtIterator sit = model.listStatements(
                        null, model.getProperty(
                                XmlToRdf.GENERIC_NS + sources[i]), (RDFNode) null);
                if(sit.hasNext()) {
                    Statement stmt = sit.next();
                    Resource r = stmt.getSubject();
                    Statement stats = r.getProperty(model.getProperty(
                            XmlToRdf.GENERIC_NS + "_stats"));
                    if(stats != null) {
                        RDFNode statsNode = stats.getObject();
                        if(statsNode.isResource()) {
                            Resource statsRes = statsNode.asResource();
                            Statement totalCount = statsRes.getProperty(
                                    model.getProperty(
                                            XmlToRdf.GENERIC_NS + "total_count"));
                            if(totalCount != null) {
                                RDFNode totalCountNode = totalCount.getObject();
                                if(totalCountNode.isLiteral()) {
                                    try {
                                        int totalCountInt = totalCountNode.asLiteral().getInt();
                                        log.info(grid + " " + totalCountInt + " total " + sources[i]);
                                        int[] total = totals.get(grid);
                                        total[i] = totalCountInt / RESULTS_PER_REQUEST + 1;
                                        totals.put(grid, total);
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
            for(String grid : grids) {
                for(int i = 0; i < totals.get(grid).length; i++) {
                   if(requestCount < totals.get(grid)[i]) {
                       return true;
                   }
                }
            }
            return false;
        }

        @Override
        public Model next() {
            Model results = ModelFactory.createDefaultModel();
            if(requestCount == 0) {                
                results = firstIteration;
                firstIteration = null;
            } else {
                try {
                    for(String grid : grids) {
                        results.add(getResults(grid));
                    }
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
            for(String grid : grids) {
                for(int i = 0; i < totals.get(grid).length; i++) {
                    if(totals.get(grid)[i] > size) {
                        size = totals.get(grid)[i];
                    }
                }
            }
            return size;
        }

        @Override
        public void close() {
            // no API method for logging out; nothing to do for now
        }
        
        private Model getResults(String grid) throws InterruptedException {
            Model model = ModelFactory.createDefaultModel();
            if(requestCount < totals.get(grid)[0]) {
                model.add(toRdf(getPubs(grid, this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            if(requestCount < totals.get(grid)[1]) {
                model.add(toRdf(getGrants(grid, this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            if(requestCount < totals.get(grid)[2]) {
                model.add(toRdf(getPatents(grid, this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            if(requestCount < totals.get(grid)[3]) {
                model.add(toRdf(getClinicalTrials(grid, this.token, requestCount * RESULTS_PER_REQUEST)));
            }
            log.debug("Model size " + model.size());
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
        
        private String getGrants(String grid, String token, int skip) throws InterruptedException {
            String queryStr = "search grants where"
                    + " (start_year >= 2014 and start_year <= 2017 and research_orgs.id = \"" + grid + "\")"
                    + " return grants[active_year + end_date + funders + id + project_num + start_date + start_year + title + abstract + funding_eur + grant_number + investigator_details + research_orgs]"
                    + " limit 200 skip " + skip;
            log.debug(queryStr);
            return getDslResponse(queryStr, token);
        }
        
        private String getPubs(String grid, String token, int skip) throws InterruptedException {
            String queryStr = "search publications where"
                    + " (year >= 2014 and year <= 2017 and research_orgs.id = \"" + grid + "\" and type in [\"article\", \"chapter\", \"proceeding\"])"
                    //+ " return publications"
                    + " return publications[id + type + title + authors + doi + pmid + pmcid + date + year + mesh_terms + journal + issn + volume + issue]"
                    + " limit 200 skip " + skip;
            log.debug(queryStr);
            return getDslResponse(queryStr, token);
        }
        
        private String getPatents(String grid, String token, int skip) throws InterruptedException {
            String queryStr = "search patents where"
                    + " (granted_year >= 2014 and granted_year <= 2017 and assignees.id = \"" + grid + "\")"
                    + " return patents[assignees + granted_year + id + title + inventor_names + funders + abstract + associated_grant_ids]"
                    + " limit 200 skip " + skip;
            log.debug(queryStr);
            return getDslResponse(queryStr, token);
        }
        
        private String getClinicalTrials(String grid, String token, int skip) throws InterruptedException {
            String queryStr = "search clinical_trials where"
                    + " (active_years >= 2014 and active_years <= 2017 and organizations.id = \"" + grid + "\")"
                    + " return clinical_trials[id + title + abstract + active_years + associated_grant_ids + date + researchers + organizations + funders]"
                    + " limit 200 skip " + skip;
            log.debug(queryStr);
            return getDslResponse(queryStr, token);
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



