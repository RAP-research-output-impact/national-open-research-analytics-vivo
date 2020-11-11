package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;

public class DimensionsClinicalTrialsConnector extends DimensionsConnector {

    private static final Log log = LogFactory.getLog(DimensionsClinicalTrialsConnector.class);

    public DimensionsClinicalTrialsConnector(String username, String password, String mongoServer, String mongoPort,
            String mongoCollection, String mongoUsername, String mongoPassword) {
        super(username, password, mongoServer, mongoPort, mongoCollection, 
                mongoUsername, mongoPassword);
    }

    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new ClinicalTrialsIterator(this.mongoCollection);
    }

    protected class ClinicalTrialsIterator extends MongoIterator implements IteratorWithSize<Model> {

        private int iteration = 0;
        
        public ClinicalTrialsIterator(MongoCollection<Document> collection) {
            super(collection, "clinical_trials");
        }

        private Set<String> roles = new HashSet<String>();

        @Override
        public Model next() {
            iteration++;
            Model results = ModelFactory.createDefaultModel();
            int batch = MONGO_DOCS_PER_ITERATION;
            List<Bson> filters = new ArrayList<Bson>();
            while(batch > 0 && defaultidIt.hasNext()) {
                batch--;
                String defaultid = defaultidIt.next();
                filters.add(Filters.eq("meta.defaultid", defaultid));
            }
            log.info("RDFizing next batch of documents from MongoDB");
            MongoCursor<Document> dcur = collection.find(Filters.or(filters)).iterator();
            try {
                while(dcur.hasNext()) {
                    Document d = dcur.next();
                    String jsonStr = d.toJson();  
                    JSONObject fullJsonObj = new JSONObject(jsonStr);               
                    JSONObject jsonObj = fullJsonObj.getJSONObject("meta").getJSONObject("raw");
                    if(log.isDebugEnabled()) {
                        log.debug(jsonObj.toString(2));
                    }                
                    try {                  
                        if(!jsonObj.has("researchers")) {
                            log.info("researchers not found");
                        } else {
                            log.info("researchers found");
                            JSONArray authors = jsonObj.getJSONArray("researchers");
                            for(int authi = 0; authi < authors.length(); authi++) {
                                JSONObject author = authors.getJSONObject(authi);
                                author.put("authorRank", authi + 1);
                                if(author.has("last_name")) {
                                    author.put("last_name_normalized", StringUtils.stripAccents(
                                            author.getString("last_name").toLowerCase()));
                                }
                            }    
                        }
                        if(!jsonObj.has("investigator_details")) {
                            log.info("investigator details not found");
                        } else {
                            log.info("investigator details found");
                            JSONArray investigatorsParsed = new JSONArray();
                            JSONArray investigators = jsonObj.getJSONArray("investigator_details");
                            for(int invi = 0; invi < investigators.length(); invi++) {
                                JSONArray investigator = investigators.getJSONArray(invi);
                                JSONObject investigatorParsed = new JSONObject();
                                investigatorParsed.put("name", investigator.get(0));
                                investigatorParsed.put("name_normalized", StringUtils.stripAccents(
                                        investigator.getString(0).toLowerCase()));
                                investigatorParsed.put("role", investigator.get(2));
                                roles.add(investigator.getString(2));                        
                                investigatorParsed.put("org_line_1", investigator.get(3));                                                
                                investigatorParsed.put("org_line_2", investigator.get(4));                                                
                                investigatorParsed.put("grid", investigator.get(5));                        
                                investigatorParsed.put("authorRank", invi + 1);
                                investigatorsParsed.put(invi, investigatorParsed);
                            }
                            jsonObj.put("investigators_parsed", investigatorsParsed);
                        }
                        if(!jsonObj.has("active_years")) { 
                            log.info("active_years not found");
                        } else {
                            log.info("active_years found");
                            JSONArray activeYears = jsonObj.getJSONArray("active_years");
                            if(activeYears.isEmpty()) {
                                log.warn("active_years array is empty");
                            } else {
                                jsonObj.put("start_year", activeYears.get(0));
                                jsonObj.put("end_year", activeYears.get(activeYears.length() - 1));
                            }
                        }
                    } catch (JSONException e) {
                        log.info(jsonObj.toString(2));
                        throw (e);
                    }
                    if(iteration < 2) {
                        log.info(fullJsonObj.toString(2)); 
                    }
                    results.add(toRdf(fullJsonObj.toString()));
                }
                return results;
            } finally {
                if(dcur != null) {
                    dcur.close();
                }
            }         
        }
        
        @Override
        public void close() {            
            log.info("**** List of roles");
            for(String role : roles) {
                log.info(role);
            }
            super.close();
        }
    }
    
    
    @Override
    protected Model mapToVIVO(Model model) {
        List<String> queries = Arrays.asList("050-clinicalTrialId.rq");
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "clinical_trials/" + query, model, ABOX + getPrefixName() + "-");
        }        
        model = renameByIdentifier(model, model.getProperty("tmp:clinicalTrialId"), ABOX, "clinical_trial.");
        queries = Arrays.asList(
                "100-clinicalTrial.rq",                
                "190-for.rq",
                "200-investigator.rq",
                "210-funder.rq",
                "500-researchOrgs.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "clinical_trials/" + query, model, ABOX + getPrefixName() + "-");
        }
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions-clinical_trials";
    }

}
