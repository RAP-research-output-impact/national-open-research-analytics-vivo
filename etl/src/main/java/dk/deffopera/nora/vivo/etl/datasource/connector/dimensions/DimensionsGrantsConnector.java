package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;

public class DimensionsGrantsConnector extends DimensionsConnector {

    private static final Log log = LogFactory.getLog(DimensionsGrantsConnector.class);
    
    public DimensionsGrantsConnector(String username, String password, String mongoServer, String mongoPort,
            String mongoCollection, String mongoUsername, String mongoPassword) {
        super(username, password, mongoServer, mongoPort, mongoCollection, 
                mongoUsername, mongoPassword);
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new GrantsIterator(this.mongoCollection);
    }
    
    protected class GrantsIterator extends MongoIterator implements IteratorWithSize<Model> {

        public GrantsIterator(MongoCollection<Document> collection) {
            this.collection = collection;
            Iterator<String> distincts = collection.distinct("meta.raw.investigator_details.role", String.class).iterator();
            while(distincts.hasNext()) {
                String distinct = distincts.next();
                log.info("role: " + distinct);
            }
            this.cursor = collection.find(Filters.eq("meta.raw.dbname", "grants"))
                    .noCursorTimeout(true).iterator();
        }
        
        @Override
        public Model next() {
            Model results = ModelFactory.createDefaultModel();
            int batch = MONGO_DOCS_PER_ITERATION;
            while(batch > 0 && cursor.hasNext()) {
                batch--;
                Document d = cursor.next();
                String jsonStr = d.toJson();   
                JSONObject fullJsonObj = new JSONObject(jsonStr);                  
                // Use the whole JSON so we can access the 'who'
                if(log.isDebugEnabled()) {
                    log.debug(fullJsonObj.toString(2));
                }
                JSONObject jsonObj = fullJsonObj.getJSONObject("meta").getJSONObject("raw");
                if(!jsonObj.has("researcher_details")) {
                    log.info("researchers not found");
                } else {
                    log.info("researchers found");
                    JSONArray authors = jsonObj.getJSONArray("researcher_details");
                    for(int authi = 0; authi < authors.length(); authi++) {
                        JSONObject author = authors.getJSONObject(authi);
                        author.put("authorRank", authi + 1);
                    }    
                }
                log.info(fullJsonObj.toString(2));
                results.add(toRdf(fullJsonObj.toString()));
// too slow: not indexed in Mongo?
//                JSONObject raw = jsonObj.getJSONObject("meta").getJSONObject("raw");
//                if(raw.has("resulting_publication_ids")) {
//                    JSONArray pubs = raw.getJSONArray("resulting_publication_ids");
//                    for(int i = 0; i < pubs.length(); i++) {
//                        String pubId = pubs.getString(i);
//                        log.info("Requesting publication " + pubId);
//                        MongoCursor<Document> pubCursor = null;
//                        try {
//                            pubCursor = collection.find(Filters.eq(
//                                    "meta.raw.dbname.id", pubId)).iterator();
//                            while(pubCursor.hasNext()) {
//                                String pubJsonStr = pubCursor.next().toJson();
//                                log.info("Adding pub RDF");
//                                results.add(toRdf(pubJsonStr));
//                            }
//                        } finally {
//                            if(pubCursor != null) {
//                                pubCursor.close();
//                            }
//                        }                                
//                    }
//                }           
            }
            return results;
        }                
        
    }
    
    @Override
    protected Model mapToVIVO(Model model) {
        long start = System.currentTimeMillis();
        //StringWriter out = new StringWriter();
        //model.write(out, "TTL");
        //log.info(out.toString());
        List<String> queries = Arrays.asList("050-grantId.rq");
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "grants/" + query, model, ABOX + getPrefixName() + "-");
        }        
        model = renameByIdentifier(model, model.getProperty("tmp:grantId"), ABOX, "");
        queries = Arrays.asList(
                "100-grant.rq",
                "190-for.rq",
                "200-investigator.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "grants/" + query, model, ABOX + getPrefixName() + "-");
        }
        log.info((System.currentTimeMillis() - start) + " ms to run mappings");
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions-grant";
    }

}
