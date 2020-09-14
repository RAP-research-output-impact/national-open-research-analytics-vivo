package dk.deffopera.nora.vivo.etl.datasource.connector;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.datasource.connector.dimensions.DimensionsConnector;
import dk.deffopera.nora.vivo.etl.datasource.connector.dimensions.DimensionsHealthcareOrgsConnector;
import dk.deffopera.nora.vivo.etl.util.XmlToRdf;

public class GridJsonConnector extends DimensionsConnector {

    private static final Log log = LogFactory.getLog(DimensionsHealthcareOrgsConnector.class);
    
    private String collectionName;
    
    public GridJsonConnector(String username, String password, String mongoServer, String mongoPort,
            String mongoCollection, String mongoUsername, String mongoPassword) {
        super(username, password, mongoServer, mongoPort, mongoCollection, 
                mongoUsername, mongoPassword);
        this.collectionName = mongoCollection;
    }
    
    @Override
    public int getBatchSize() {
        return 1;
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new GridJsonIterator(this.mongoCollection);
    }
    
    protected class GridJsonIterator extends MongoIterator implements IteratorWithSize<Model> {

        private int iteration = 0;
        
        public GridJsonIterator(MongoCollection<Document> collection) {
            this.collection = collection;
            ArrayList<Bson> bsons = new ArrayList<Bson>();
            for(String grid : ugrids.values()) {
                bsons.add(Filters.eq("id", grid));
            }
            this.cursor = collection.find(Filters.or(bsons))
                    .noCursorTimeout(true).iterator();
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
        model = renameByIdentifier(model, model.getProperty(XmlToRdf.GENERIC_NS + "id"), ABOX, "");
        Model out = ModelFactory.createDefaultModel();
        StmtIterator sit = model.listStatements();
        while(sit.hasNext()) {
            Statement stmt = sit.next();
            if(XmlToRdf.GENERIC_NS.equals(stmt.getPredicate().getNameSpace())) {
                out.add(stmt.getSubject(), out.getProperty(
                        "http://vivo.deffopera.dk/ontology/osrap/" + stmt.getPredicate().getLocalName()),
                        stmt.getObject());
            } else if(!RDF.type.equals(stmt.getPredicate())) {
                out.add(stmt);
            }
        }
        return out;
    }
    
    @Override 
    public Model filter(Model model) {
       return model;
    }

    @Override
    protected String getPrefixName() {
        return "gridJson";
    }

}
