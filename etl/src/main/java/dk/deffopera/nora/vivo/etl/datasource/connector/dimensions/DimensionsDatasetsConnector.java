package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

public class DimensionsDatasetsConnector extends DimensionsConnector {

    private static final Log log = LogFactory.getLog(DimensionsDatasetsConnector.class);
    
    public DimensionsDatasetsConnector(String username, String password, String mongoServer, String mongoPort,
            String mongoCollection, String mongoUsername, String mongoPassword) {
        super(username, password, mongoServer, mongoPort, mongoCollection, 
                mongoUsername, mongoPassword);
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new GrantsIterator(this.mongoCollection);
    }
    
    protected class GrantsIterator extends MongoIterator implements IteratorWithSize<Model> {

        private List<String> defaultids = new ArrayList<String>();
        private Iterator<String> defaultidIt;        
        private int iteration = 0;
        
        public GrantsIterator(MongoCollection<Document> collection) {
            this.collection = collection;
            try {
                this.cursor = collection.find(Filters.eq("meta.raw.dbname", "datasets"))
                        .projection(new Document("meta.defaultid", 1))
                        .noCursorTimeout(true).iterator();
                while(cursor.hasNext()) {
                    String jsonStr = cursor.next().toJson();
                    JSONObject json = new JSONObject(jsonStr);
                    String defaultid = json.getJSONObject("meta").getString("defaultid");
                    defaultids.add(defaultid);
                }
            } finally {
                if(this.cursor != null) {
                    cursor.close();
                }
            }
            this.defaultidIt = defaultids.iterator();
            log.info(defaultids.size() + " documents to retrieve");
        }
        
        @Override
        public boolean hasNext() {
            return defaultidIt.hasNext();
        }
        
        @Override
        public Model next() {
            iteration++;
            Model results = ModelFactory.createDefaultModel();
            long start = System.currentTimeMillis();
            int batch = MONGO_DOCS_PER_ITERATION;
            List<Bson> filters = new ArrayList<Bson>();
            while(batch > 0 && defaultidIt.hasNext()) {
                //while(batch > 0 && cursor.hasNext()) {
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
                    if(iteration < 2) {
                        log.info(fullJsonObj.toString(2));
                    }
                    JSONObject jsonObj = fullJsonObj.getJSONObject("meta").getJSONObject("raw");
                    if(log.isDebugEnabled()) {
                        log.debug(jsonObj.toString(2));
                    }                
                    try {                  
                        if(!jsonObj.has("researchers")) {
                            log.debug("researchers not found");
                        } else {
                            log.debug("researchers found");
                            JSONArray authors = jsonObj.getJSONArray("researchers");
                            for(int authi = 0; authi < authors.length(); authi++) {
                                JSONObject author = authors.getJSONObject(authi);
                                author.put("authorRank", authi + 1);
                            }    
                        }                                                        
                    } catch (JSONException e) {
                        log.info(jsonObj.toString(2));
                        throw (e);
                    }
                    results.add(toRdf(fullJsonObj.toString()));
                }
                log.info((System.currentTimeMillis() - start) + " ms to RDFize batch of documents");
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
        List<String> queries = Arrays.asList("050-datasetId.rq");
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "datasets/" + query, model, ABOX + getPrefixName() + "-");
        }        
        model = renameByIdentifier(model, model.getProperty("tmp:datasetId"), ABOX, "dataset.");
        queries = Arrays.asList(
                "100-dataset.rq",
                "140-datasetAuthorship.rq",
                "190-for.rq",
                "500-researchOrgs.rq"                
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "datasets/" + query, model, ABOX + getPrefixName() + "-");
        }
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions-dataset";
    }

}
