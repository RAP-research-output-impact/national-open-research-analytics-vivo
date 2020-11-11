package dk.deffopera.nora.vivo.etl.datasource.connector;

import java.util.ArrayList;
import java.util.List;

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
import com.mongodb.client.MongoCursor;
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
        return 10;
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new GridJsonIterator(this.mongoCollection);
    }
    
    protected class GridJsonIterator extends MongoIterator implements IteratorWithSize<Model> {

        private int iteration = 0;
        
        public GridJsonIterator(MongoCollection<Document> collection) {
            super(collection, null);
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
                filters.add(Filters.eq("_id", defaultid));
            }
            log.info("RDFizing next batch of documents from MongoDB");
            MongoCursor<Document> dcur = collection.find(Filters.or(filters)).iterator();
            try {
                while(dcur.hasNext()) {
                    Document d = dcur.next();
                    String jsonStr = d.toJson();       
                if(iteration < 6) {
                    JSONObject fullJsonObj = new JSONObject(jsonStr);
                    log.info(fullJsonObj.toString(2));
                }
                results.add(toRdf(jsonStr));
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
