package dk.deffopera.nora.vivo.etl.datasource.connector;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.bson.Document;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCollection;

import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.datasource.connector.dimensions.DimensionsConnector;
import dk.deffopera.nora.vivo.etl.datasource.connector.dimensions.DimensionsHealthcareOrgsConnector;
import dk.deffopera.nora.vivo.etl.util.JsonToXMLConverter;
import dk.deffopera.nora.vivo.etl.util.RdfUtils;
import dk.deffopera.nora.vivo.etl.util.XmlToRdf;

public class BFIOAIConnector extends DimensionsConnector {

    private static final Log log = LogFactory.getLog(DimensionsHealthcareOrgsConnector.class);
    
    public BFIOAIConnector(String username, String password, String mongoServer, String mongoPort,
            String mongoCollection, String mongoUsername, String mongoPassword) {
        super(username, password, mongoServer, mongoPort, mongoCollection, 
                mongoUsername, mongoPassword);
    }
    
    @Override
    public int getBatchSize() {
        return 1000;
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new BFIOAIIterator(this.mongoCollection);
    }
    
    protected class BFIOAIIterator extends MongoIterator implements IteratorWithSize<Model> {

        private int iteration = 0;
        
        public BFIOAIIterator(MongoCollection<Document> collection) {
            this.collection = collection;
            this.cursor = collection.find().noCursorTimeout(true).iterator();
        }
        
        @Override
        public Model next() {
            iteration++;
            Model results = ModelFactory.createDefaultModel();
            int batch = MONGO_DOCS_PER_ITERATION;
            while(batch > 0 && cursor.hasNext()) {
                batch--;
                //long start = System.currentTimeMillis();
                //log.info("Getting next document from cursor");
                Document d = cursor.next();
                //log.info((System.currentTimeMillis() - start) + " ms to retrieve document");
                String jsonStr = d.toJson();        
                if(iteration < 6) {
                    JSONObject fullJsonObj = new JSONObject(jsonStr);
                    log.info(fullJsonObj.toString(2));
                }
                results.add(toRdf(jsonStr));
            }         
            return results;
        }          
        
    }
    
    @Override
    protected Model mapToVIVO(Model model) {
        model = renameByIdentifier(model, model.getProperty(XmlToRdf.GENERIC_NS + "DOI"), ABOX, "bfioai-doi-");
        List<String> queries = Arrays.asList(
                "100-bfioai.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "bfioai/" + query, model, ABOX + getPrefixName() + "-");
        }
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions-healthcare_orgs";
    }

}
