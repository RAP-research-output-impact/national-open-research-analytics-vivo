package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;

import dk.deffopera.nora.vivo.etl.datasource.DataSource;
import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.datasource.connector.ConnectorDataSource;
import dk.deffopera.nora.vivo.etl.util.HttpUtils;
import dk.deffopera.nora.vivo.etl.util.JsonToXMLConverter;
import dk.deffopera.nora.vivo.etl.util.RdfUtils;
import dk.deffopera.nora.vivo.etl.util.XmlToRdf;

public class DimensionsConnector extends ConnectorDataSource 
        implements DataSource {
    
    protected static final String DIMENSIONS_API = "https://app.dimensions.ai/api/";
    protected static final String ABOX = "http://vivo.deffopera.dk/individual/";
    protected static final String SPARQL_RESOURCE_DIR = "/dimensions/sparql/";
    protected static final long REQUEST_INTERVAL = 2000; // ms
    protected static final Map<String, String> ugrids = new HashMap<String, String>();
    protected static final Map<String, String> hgrids = new HashMap<String, String>();    
    
    static {
        // TODO load dynamically from a CSV
        ugrids.put("DTU", "grid.5170.3");
        ugrids.put("Arhus University", "grid.7048.b");
        ugrids.put("Aalborg University", "grid.5117.2");
        ugrids.put("University of Southern Denmark", "grid.10825.3e");
        ugrids.put("Copenhagen Business School", "grid.4655.2");
        ugrids.put("IT University", "grid.32190.39");
        ugrids.put("Roskilde University", "grid.11702.35");
        ugrids.put("Copenhagen University", "grid.5254.6");
        hgrids.put("Rigshospitalet", "grid.475435.4");
        hgrids.put("Aarhus University Hospital",  "grid.154185.c");
        hgrids.put("Bispebjerg Hospital", "grid.411702.1");
        hgrids.put("Hvidovre Hospital", "grid.411905.8");
        hgrids.put("Copenhagen University Hospital", "grid.4973.9");   
        hgrids.put("Herlev Hospital", "grid.411900.d");
        hgrids.put("Glostrup Hospital", "grid.411719.b");
        hgrids.put("Gentofte Hospital", "grid.411646.0");
        hgrids.put("Frederiksberg Hospital", "grid.415046.2");
        hgrids.put("Vejle Sygehus", "grid.417271.6");
        hgrids.put("Nordsjællands Hospital", "grid.414092.a");
        hgrids.put("Odense University Hospital", "grid.7143.1");
        hgrids.put("Regionshospitalet Viborg", "grid.416838.0");
        hgrids.put("Marselisborg Hospital", "grid.477140.1");
        hgrids.put("Sydvestjysk Sygehus", "grid.414576.5");
        hgrids.put("Kolding Hospital", "grid.415434.3");
        hgrids.put("Aalborg Hospital", "grid.27530.33");
        hgrids.put("Amager Hospital", "grid.413660.6");
        hgrids.put("Roskilde Sygehus", "grid.416059.f");
        hgrids.put("Regionshospitalet Herning", "grid.414058.c");
        hgrids.put("Regionshospital Holstebro", "grid.414304.6");
        hgrids.put("Svendborg Sygehus", "grid.416768.a");
        hgrids.put("Køge Hospital", "grid.416055.3");
        hgrids.put("Regionshospitalet Randers", "grid.415677.6");
        hgrids.put("Næstved Sygehus", "grid.416369.f");
        hgrids.put("Hosrsens Hospital", "grid.414334.5");
        hgrids.put("Sygehus Sønderjylland", "grid.416811.b");
        hgrids.put("Nykøbing Hospital", "grid.413717.7");
        hgrids.put("Holbæk Sygehus", "grid.414289.2");
        hgrids.put("Sygehus Vendsyssel", "grid.414274.0");
        hgrids.put("Skive Hospital", "grid.416035.5");
        hgrids.put("Sygehus Lillebælt", "grid.459623.f");
        hgrids.put("Fredericia Sygehus", "grid.415040.4");
        hgrids.put("Slagelse Hospital", "grid.452905.f");
        hgrids.put("Regional Hospital West Jutland", "grid.452681.c");
        hgrids.put("Zealand University Hospital", "grid.476266.7");
        hgrids.put("Regionshospitalet Silkeborg", "grid.477812.f");
        hgrids.put("Rinsted Sygehus", "grid.477756.0");
        hgrids.put("Regionshospital Nordjylland", "grid.487445.e");
    }
       
    private static final Log log = LogFactory.getLog(DimensionsConnector.class);
    protected HttpUtils httpUtils = new HttpUtils();
    protected String token;
    
    private MongoClient mongoClient;
    protected MongoCollection<Document> mongoCollection;
    protected MongoCollection<Document> ddfDoiCollection;
        
    public DimensionsConnector(String username, String password, 
            String mongoServer, String mongoPort, String mongoCollection, 
            String mongoUsername, String mongoPassword) {                     
        //this.token = getToken(username, password);
        MongoCredential credential = MongoCredential.createScramSha256Credential(
                mongoUsername, "opera", mongoPassword.toCharArray());
        
        this.mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .retryReads(true)
                        .applyToSocketSettings(builder -> builder.readTimeout(
                                180, TimeUnit.SECONDS))
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(
                                        new ServerAddress(
                                                mongoServer, Integer.parseInt(mongoPort)))))
                        .credential(credential)
                        .build());
        MongoDatabase database = mongoClient.getDatabase("opera");
        MongoIterable<String> collNames = database.listCollectionNames();
        for(String collName : collNames) {
            log.info("Collection name: " + collName);
        }
        //MongoCollection<Document> gridCollection = database.getCollection("uni-org-type");
        //MongoCursor<Document> gridCursor = gridCollection.find().cursor();
        //while(gridCursor.hasNext()) {
        //   Document gridDoc = gridCursor.next();
        //   log.info(gridDoc.toJson());
        //}
        this.ddfDoiCollection = database.getCollection("ddf");
        this.mongoCollection = database.getCollection(mongoCollection);                
    }
    
    @Override
    public int getBatchSize() {
        return 20;
    }
    
    protected String getToken(String username, String password) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("username", username);
        json.put("password", password);
        String tokenJson = httpUtils.getHttpPostResponse(
                DIMENSIONS_API + "auth.json", json.toString(), "application/json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tokenObj;
        try {
            tokenObj = mapper.readTree(tokenJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tokenObj.get("token").textValue();
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new MongoIterator(this.mongoCollection);
    }
    
    protected class MongoIterator implements IteratorWithSize<Model> {

        protected MongoCollection<Document> collection;
        protected MongoCursor<Document> cursor;
        protected int toRdfIteration = 0;
        protected int MONGO_DOCS_PER_ITERATION = 10;
               
        protected MongoIterator() {}
        
        public MongoIterator(MongoCollection<Document> collection) {
            this.collection = collection;
            //Iterator<String> distincts = collection.distinct("meta.raw.type", String.class).iterator();
            //while(distincts.hasNext()) {
            //    String distinct = distincts.next();
            //    log.info("type: " + distinct);
            //}
            cursor = collection.find(
                    Filters.and(
                            Filters.eq("meta.raw.dbname", "publications")
                            //Filters.eq("meta.raw.type", "article"),  // TODO remove
                            //Filters.exists("meta.raw.category_sdg")  // TODO remove
                            //Filters.exists("meta.raw.author_affiliations", false)
                            //Filters.eq("meta.raw.id", "pub.1100249993")
                    )
                 )
                    .noCursorTimeout(true).iterator();
        }
        
        @Override
        public boolean hasNext() {
            return cursor.hasNext();
        }

        @Override
        public Model next() {
            Model results = ModelFactory.createDefaultModel();
            int batch = MONGO_DOCS_PER_ITERATION;
            while(batch > 0 && cursor.hasNext()) {
                batch--;
                long start = System.currentTimeMillis();
                log.debug("Getting next document from cursor");
                Document d = cursor.next();
                log.info((System.currentTimeMillis() - start) + " ms to retrieve document");
                start = System.currentTimeMillis();
                String jsonStr = d.toJson();   
                JSONObject jsonObj = new JSONObject(jsonStr);
                JSONObject fullJsonObj = jsonObj;
                log.debug((System.currentTimeMillis() - start) + " ms to convert to JSON");
                start = System.currentTimeMillis();
                JSONObject meta = jsonObj.getJSONObject("meta");                
                jsonObj = meta.getJSONObject("raw");
                try {               
                    if(jsonObj.has("authors")) {
                        JSONArray authors = jsonObj.getJSONArray("authors");
                        for(int authi = 0; authi < authors.length(); authi++) {
                            JSONObject author = authors.getJSONObject(authi);
                            author.put("authorRank", authi + 1);
                        }                           
                    }
                } catch (JSONException e) {
                    log.info(jsonObj.toString(2));
                    throw (e);
                }
                //long start2 = System.currentTimeMillis();
                //addDDFGrids(jsonObj);
                //log.debug((System.currentTimeMillis() - start2) + " ms to retrieve DDF grids");
                if(log.isDebugEnabled()) {
                    log.debug(jsonObj.toString(2));
                }                                
                if(toRdfIteration < MONGO_DOCS_PER_ITERATION) {
                  log.info(fullJsonObj.toString(2));
                }
                log.debug((System.currentTimeMillis() - start) + " ms to preprocess JSON");
                start = System.currentTimeMillis();
                Model rdf = toRdf(fullJsonObj.toString());
                if(toRdfIteration < MONGO_DOCS_PER_ITERATION) {
                    //StringWriter out = new StringWriter();
                    //rdf.write(out, "TTL");
                    //log.info(out.toString());
                }
                results.add(rdf);
                log.debug((System.currentTimeMillis() - start) + " ms to convert to RDF");
            }
            return results;
        }
        
        private void addDDFGrids(JSONObject json) {
            if(json.has("doi")) {
                MongoCursor<Document> cursor = ddfDoiCollection.find(Filters.eq(
                        "doi", json.getString("doi"))).iterator();
                List<String> grids = new ArrayList<String>();
                while(cursor.hasNext()) {
                    String ddfJson = cursor.next().toJson();
                    JSONObject ddf = new JSONObject(ddfJson);
                    if(ddf.has("grid")) {
                        grids.add(ddf.getString("grid"));
                    }                    
                }
                if(!grids.isEmpty()) {
                    log.info("DDF grids found :)");
                    json.put("ddfGrids", grids);
                } else {
                    log.info("No DDF grids found :(");
                }
            }
            
            
        }
        
        protected Model toRdf(String data) {                
            try {
                JsonToXMLConverter json2xml = new JsonToXMLConverter();
                XmlToRdf xml2rdf = new XmlToRdf();
                RdfUtils rdfUtils = new RdfUtils();                
                String xml = json2xml.convertJsonToXml(data);
                Model rdf = xml2rdf.toRDF(xml);
                toRdfIteration++;
                rdf = rdfUtils.renameBNodes(rdf, ABOX + getPrefixName() + "-n" + toRdfIteration + "-", rdf);
                return rdf;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Integer size() {
            return ((int) collection.count() / MONGO_DOCS_PER_ITERATION) + 1;            
        }

        @Override
        public void close() {
            if(cursor != null) {
                log.info("Closing iterator");
                cursor.close();
            }
            if(mongoClient != null) {
                log.info("Closing Mongo client");
                mongoClient.close();
            }
        }
        
    }
    
    @Override
    protected Model filter(Model model) {
        if(true) {
            // TODO remove
            //return generateOrgs();
            return filterGeneric(model);
            //Model out = ModelFactory.createDefaultModel();
            //out.add(model.listStatements(null, model.getProperty(
            //        "http://vivo.deffopera.dk/ontology/osrap/matchingstatus"), (RDFNode) null));
            //out.add(model.listStatements(null, model.getProperty(
            //        "http://vivo.deffopera.dk/ontology/osrap/activeYear"), (RDFNode) null));
            //return out;
        } else {
            return model;
        }
    }
    
    @Override
    protected Model mapToVIVO(Model model) {
        long start = System.currentTimeMillis();
        List<String> queries = Arrays.asList(
                //"050-orcidId.rq",
                "100-publicationTypes.rq",
                "110-publicationMetadata.rq"
                //"115-abstract.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + query, model, ABOX + getPrefixName() + "-");
        }
        model = renameByIdentifier(model, model.getProperty(
                XmlToRdf.GENERIC_NS + "publication_id"), ABOX, "");
        queries = Arrays.asList(
                "120-publicationDate.rq",
                "130-publicationJournal.rq",
                //"140-publicationAuthorship.rq"
                "141-publicationAuthorship1.rq",
                "142-publicationAuthorship2.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + query, model, ABOX + getPrefixName() + "-");
        }
        model = renameByIdentifier(model, model.getProperty(
               XmlToRdf.GENERIC_NS + "person_researcher_id"), ABOX, "");
        model = renameByIdentifier(model, model.getProperty(
                XmlToRdf.GENERIC_NS + "person_orcidStr"), ABOX, "orcid-");
        queries = Arrays.asList(         
                "150-publicationAuthor.rq",
                "160-publicationAuthorPosition.rq",
                "170-publisher.rq",
                "180-mesh.rq",
                "190-for.rq",
                "200-rcdc.rq",
                "210-hrcs.rq",
                "215-sdg.rq",
                "220-openAccess.rq",
                "230-funding.rq",
                "240-references.rq"
                );
        for(String query : queries) {
            construct(SPARQL_RESOURCE_DIR + query, model, ABOX + getPrefixName() + "-");            
        }
        model = renameByIdentifier(model, model.getProperty(
                XmlToRdf.GENERIC_NS + "org_id"), ABOX, "");
        log.debug((System.currentTimeMillis() - start) + " ms to map to VIVO");
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "dimensions";
    }
    
}



