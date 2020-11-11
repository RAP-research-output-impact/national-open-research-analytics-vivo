package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.util.XmlToRdf;

public class DimensionsAddressesConnector extends DimensionsConnector {

    private static final Log log = LogFactory.getLog(DimensionsAddressesConnector.class);
    
    public DimensionsAddressesConnector(String username, String password, String mongoServer, String mongoPort,
            String mongoCollection, String mongoUsername, String mongoPassword) {
        super(username, password, mongoServer, mongoPort, mongoCollection, 
                mongoUsername, mongoPassword);
    }

    @Override
    public int getBatchSize() {
        return 100;
    }

    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new AddressesIterator(this.mongoCollection);
    }

    protected class AddressesIterator extends MongoIterator implements IteratorWithSize<Model> {

        private int iteration = 0;

        public AddressesIterator(MongoCollection<Document> collection) {
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
                filters.add(Filters.eq("_id", new ObjectId(defaultid)));
            }
            log.info("RDFizing next batch of documents from MongoDB");
            MongoCursor<Document> dcur = collection.find(Filters.or(filters)).iterator();
            try {
                while(dcur.hasNext()) {
                    Document d = dcur.next();
                    String jsonStr = d.toJson();
                    JSONObject fullJsonObj = new JSONObject(jsonStr);     
                    if(iteration < 6) {
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
        model = renameByIdentifier(model, model.getProperty(XmlToRdf.GENERIC_NS + "id"), ABOX, "");
        List<String> queries = Arrays.asList(
                "100-address.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + "addresses/" + query, model, ABOX + getPrefixName() + "-");
        }
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions-addresses";
    }

}
