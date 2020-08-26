package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
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

        public GrantsIterator(MongoCollection<Document> collection) {
            this.collection = collection;
            this.cursor = collection.find(Filters.eq("meta.raw.dbname", "datasets"))
                    .noCursorTimeout(true).iterator();
        }
        
        @Override
        public Model next() {
            Model results = ModelFactory.createDefaultModel();
            int batch = MONGO_DOCS_PER_ITERATION;
            while(batch > 0 && cursor.hasNext()) {
                batch--;
                long start = System.currentTimeMillis();
                log.info("Getting next document from cursor");
                Document d = cursor.next();
                log.info((System.currentTimeMillis() - start) + " ms to retrieve document");
                String jsonStr = d.toJson();   
                JSONObject fullJsonObj = new JSONObject(jsonStr);
                log.info(fullJsonObj.toString(2));                
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
                        }    
                    }                                                        
                } catch (JSONException e) {
                    log.info(jsonObj.toString(2));
                    throw (e);
                }
                results.add(toRdf(fullJsonObj.toString()));
            }
            return results;
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
