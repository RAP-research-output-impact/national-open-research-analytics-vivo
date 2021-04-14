package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class DimensionsPatentsConnector extends DimensionsConnector {

    private static final Log log = LogFactory.getLog(DimensionsPatentsConnector.class);
    
    public DimensionsPatentsConnector(String username, String password, String mongoServer, String mongoPort,
            String mongoCollection, String mongoUsername, String mongoPassword) {
        super(username, password, mongoServer, mongoPort, mongoCollection, 
                mongoUsername, mongoPassword);
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new PatentsIterator(this.mongoCollection);
    }
    
    protected class PatentsIterator extends MongoIterator implements IteratorWithSize<Model> {

        private int iteration = 0;
        
        public PatentsIterator(MongoCollection<Document> collection) {
            super(collection, "patents");
        }
        
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
                                String lastName = author.getString("last_name");
                                lastName = StringUtils.stripAccents(lastName.toLowerCase());
                                author.put("authorRank", authi + 1);
                                author.put("last_name_lower", lastName);
                            }    
                        }    
                        if(!jsonObj.has("inventor_names")) {
                            log.info("inventor_names not found");                        
                        } else {
                            log.info("inventor_names found");
                            JSONArray inventors = new JSONArray();
                            JSONArray inventorNames = jsonObj.getJSONArray("inventor_names");
                            for(int invi = 0; invi < inventorNames.length(); invi++) {
                                String uncontrolledInventorName = inventorNames.getString(invi);
                                String inventorName = StringUtils.stripAccents(
                                        uncontrolledInventorName.toLowerCase());
                                JSONObject inventor = new JSONObject();
                                inventor.put("name", inventorName);
                                inventor.put("uncontrolledName", uncontrolledInventorName);
                                inventor.put("authorRank", invi + 1);
                                inventors.put(invi, inventor);
                            }
                            jsonObj.put("inventors", inventors);
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
    }
    
    @Override
    protected Model mapToVIVO(Model model) {
        List<String> queries = Arrays.asList("050-patentId.rq");
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "patents/" + query, model, ABOX + getPrefixName() + "-");
        }        
        model = renameByIdentifier(model, model.getProperty("tmp:patentId"), ABOX, "patent.");
        queries = Arrays.asList(
                "100-patent.rq",
                "140-patentAuthorship.rq",
                "190-for.rq",
		"230-funding.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "patents/" + query, model, ABOX + getPrefixName() + "-");
        }
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions-patent";
    }

}
