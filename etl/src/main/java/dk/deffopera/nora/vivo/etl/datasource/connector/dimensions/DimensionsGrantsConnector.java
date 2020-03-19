package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.util.XmlToRdf;

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
            Iterator<String> distincts = collection.distinct("meta.raw.dbname", String.class).iterator();
            while(distincts.hasNext()) {
                String distinct = distincts.next();
                log.info("dbname: " + distinct);
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
                JSONObject jsonObj = new JSONObject(jsonStr);
                log.info(jsonObj.toString(2));                
                jsonObj = jsonObj.getJSONObject("meta").getJSONObject("raw");
                if(log.isDebugEnabled()) {
                    log.debug(jsonObj.toString(2));
                }                
                results.add(toRdf(jsonObj.toString()));
            }
            return results;
        }                
        
    }
    
    @Override
    protected Model mapToVIVO(Model model) {
        StringWriter out = new StringWriter();
        model.write(out, "TTL");
        log.info(out.toString());
        List<String> queries = Arrays.asList("050-grantId.rq");
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "grants/" + query, model, ABOX + getPrefixName() + "-");
        }        
        model = renameByIdentifier(model, model.getProperty("tmp:grantId"), ABOX, "");
        queries = Arrays.asList(
                "100-grant.rq",
                "190-for.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "grants/" + query, model, ABOX + getPrefixName() + "-");
        }
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions-grant";
    }

}
