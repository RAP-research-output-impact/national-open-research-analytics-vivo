package dk.deffopera.nora.vivo.etl.datasource.connector.dimensions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import dk.deffopera.nora.vivo.etl.datasource.DataSource;
import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.util.JsonToXMLConverter;
import dk.deffopera.nora.vivo.etl.util.RdfUtils;
import dk.deffopera.nora.vivo.etl.util.SparqlEndpoint;
import dk.deffopera.nora.vivo.etl.util.XmlToRdf;

public class DimensionsByDoiConnector extends DimensionsConnector implements DataSource {

    private static final String DEFAULT_NAMESPACE = "http://vivo.deffopera.dk/individual/";
    private static final Map<String, String> ugrids = new HashMap<String, String>();
    
    static {        
        ugrids.put("dtu", "grid.5170.3");
        ugrids.put("au", "grid.7048.b");
        ugrids.put("aau", "grid.5117.2");
        ugrids.put("sdu", "grid.10825.3e");
        ugrids.put("cbs", "grid.4655.2");
        ugrids.put("itu", "grid.32190.39");
        ugrids.put("ruc", "grid.11702.35");
        ugrids.put("ku", "grid.5254.6");
    }
    
    private static final Log log = LogFactory.getLog(DimensionsByDoiConnector.class);    
    
    private SparqlEndpoint sourceEndpoint;
    private Map<String, String> allDois;
    private Map<String, Set<String>> univToDois = new HashMap<String, Set<String>>();
    
    public DimensionsByDoiConnector(String username, String password, SparqlEndpoint endpoint) {
        super(username, password);
        this.sourceEndpoint = endpoint;
        log.info("Getting all DOIs");
        this.allDois = getAllDois();
        for(String abbr : ugrids.keySet()) {
            log.info("Getting DOIs associated with " + abbr);
            Set<String> dois = getDoisForGrid(ugrids.get(abbr));
            log.info(dois.size() + " dois");
            univToDois.put(abbr, dois);
        }
    }
    
    private Map<String, String> getAllDois() {
        String queryStr = "SELECT ?doi ?pub WHERE { \n" +
                "  ?pub <http://purl.org/ontology/bibo/doi> ?doi \n" +
                "} \n";
        Map<String, String> dois = new HashMap<String, String>();
        ResultSet rs = sourceEndpoint.getResultSet(queryStr);
        while(rs.hasNext()) {
            QuerySolution qsoln = rs.next();
            RDFNode pubNode = qsoln.get("pub");
            if(!pubNode.isURIResource()) {
                continue;
            } else {
                String pubURI = pubNode.asResource().getURI();
                dois.put(qsoln.get("doi").asLiteral().getLexicalForm().toLowerCase(), pubURI);    
            }
            
        }
        return dois;
    }
    
    private Set<String> getDoisForGrid(String grid) {
        String queryStr = "SELECT ?doi WHERE { \n" +
                          "  ?authorship <http://vivo.deffopera.dk/ontology/osrap/relatedToOrganization> <" + DEFAULT_NAMESPACE + grid + "> . \n" +
                          "  ?authorship <http://vivoweb.org/ontology/core#relates> ?pub . \n" +
                          "  ?pub <http://purl.org/ontology/bibo/doi> ?doi \n" +
                          "} \n";
        log.info(queryStr);
        Set<String> dois = new HashSet<String>();
        ResultSet rs = sourceEndpoint.getResultSet(queryStr);
        while(rs.hasNext()) {
            QuerySolution qsoln = rs.next();
            dois.add(qsoln.get("doi").asLiteral().getLexicalForm().toLowerCase());
        }
        return dois;
    }
    
    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        Object dataDir = this.getConfiguration().getParameterMap().get("dataDir");
        if(!(dataDir instanceof String)) {
            throw new RuntimeException("dataDir parameter must point to " 
                    + "directory of DOI CSV files");
        }
        return new DimensionsByDoiIterator((String) dataDir, this.token);
    }
   
    private class DimensionsByDoiIterator implements IteratorWithSize<Model> {

        private static final int RESULTS_PER_REQUEST = 200;
        private String token;
        private List<File> doiFiles;                
        private Iterator<File> doiFileIt;
        
        public DimensionsByDoiIterator(String dataDir, String token) {
            this.token = token;
            log.info("Using dataDir " + dataDir);
            File dois = new File(dataDir);            
            this.doiFiles = Arrays.asList(dois.listFiles());
            doiFileIt = doiFiles.iterator();
        }
        
        @Override
        public boolean hasNext() {
            return doiFileIt.hasNext();
        }

        @Override
        public Model next() {
            Model model = ModelFactory.createDefaultModel();
            File doiFile = doiFileIt.next();
            String[] filenameParts = doiFile.getName().split("-");
            String yearStr = filenameParts[2];
            String univStr = filenameParts[3];
            univStr = univStr.replaceAll(".csv", "");
            log.info("Checking _" + univStr + "_ for year " + yearStr);
            Set<String> univDoiSet = univToDois.get(univStr);
            int year = Integer.parseInt(yearStr);
            if(year < 2014 || year > 2017) {
                log.info("Skipping " + doiFile.getName());
                return model;
            }
            Reader reader;
            CSVParser csvParser = null;
            try {
                reader = new BufferedReader(new FileReader(doiFile));
                csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
                int foundCount = 0;
                int foundButNotAssociatedCount = 0;
                int notFoundCount = 0;
                int notFoundInDimensionsCount = 0;
                List<String> doisToRetrieve = new ArrayList<String>();
                for (CSVRecord csvRecord : csvParser) {                    
                    String doi = csvRecord.get(0).toLowerCase();
                    if(doi != null && !doi.isEmpty()) {
                        log.info("Checking DOI " + doi);
                        if(univDoiSet.contains(doi)) {
                            log.info("DOI found and associated with DOI; all good");
                            foundCount++;
                        } else if (allDois.keySet().contains(doi)){
                            log.info("DOI found but not associated with " + univStr);
                            String pubURI = allDois.get(doi);
                            if(pubURI == null) {
                                throw new RuntimeException("No URI found for " + doi);                    
                            } else {
                                model.add(associationTriple(univStr, pubURI, false));
                                foundButNotAssociatedCount++;    
                            }                            
                        } else {
                            notFoundCount++;
                            log.info("DOI not found; adding to list to retrieve from Dimensions");
                            doisToRetrieve.add(doi);
                        }                        
                    }
                }
                int batchSize = 100;
                int j = 0;
                List<String> doisToOr = new ArrayList<String>();
                for(int i = 0; i < doisToRetrieve.size() ; i++) {                    
                    doisToOr.add(doisToRetrieve.get(i));
                    j++;
                    if(j == batchSize || (i + 1 == doisToRetrieve.size())) {
                        String dimensionsData = getPubs(doisToOr, token);                        
                        Model rdf = toRdf(dimensionsData);
                        Map<String, String> foundDois = getFoundDois(rdf);
                        log.info(foundDois.size() + " found Dois");
                        log.info(doisToOr.size() + " in query");
                        for(String doi : doisToOr) {
                            if(!foundDois.keySet().contains(doi)) {
                                model.add(missingPubMarker(univStr, doi));
                                notFoundInDimensionsCount++;
                            } else {                                
                                log.info("before " + model.size());
                                model.add(associationTriple(univStr, foundDois.get(doi), true));
                                log.info("after " + model.size());
                            }
                        }
                        model.add(rdf);                        
                        doisToOr.clear();
                        j = 0;
                    }
                }
                log.info("Total found: " + foundCount);
                log.info("Total found but not associated: " + foundButNotAssociatedCount);
                log.info("Total not found in VIVO: " + notFoundCount);
                log.info("Total not found in Dimensions: " + notFoundInDimensionsCount);
                return model;
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if(csvParser != null) {
                    try {
                        csvParser.close();
                    } catch (IOException e) {
                        log.error(e, e);;
                    }
                }
            }
        }
        
        private Model associationTriple(String univStr, String pubURI, boolean markRetrieved) {
            Model m = ModelFactory.createDefaultModel();
            String grid = ugrids.get(univStr);
            if(grid == null) {
                throw new RuntimeException("grid not found for " + univStr);
            } else {
                String univURI = DEFAULT_NAMESPACE + grid;
                if(pubURI == null) {
                    throw new RuntimeException("pubURI cannot be null");
                }
                m.add(m.getResource(pubURI), 
                        m.getProperty("http://vivo.deffopera.dk/ontology/osrap/relatedToOrganizationByDdfDoi"), m.getResource(univURI));
                m.add(m.getResource(univURI), 
                        m.getProperty("http://vivo.deffopera.dk/ontology/osrap/relatedToPublicationByDdfDoi"), m.getResource(pubURI));
                if(markRetrieved) {
                    m.add(m.getResource(pubURI), 
                            m.getProperty("http://vivo.deffopera.dk/ontology/osrap/retrievedFromDimensionsByDdfDoi"), m.getResource(univURI));
                }
            }
            log.info("Returning " + m.size() + " triples");
            return m;
        }
        
        private Model missingPubMarker(String univStr, String doi) {
            Model m = ModelFactory.createDefaultModel();
            String grid = ugrids.get(univStr);
            if(grid == null) {
                throw new RuntimeException("grid not found for " + univStr);
            } else {
                String univURI = DEFAULT_NAMESPACE + grid;
                String pubURI = DEFAULT_NAMESPACE + "notfoundindimensions-doi-" + doi;
                m.add(m.getResource(pubURI), m.getProperty("http://purl.org/ontology/bibo/doi"), doi);
                m.add(m.getResource(pubURI), RDF.type, m.getResource("http://vivo.deffopera.dk/ontology/osrap/NotFoundByDOIInDimensions"));
                m.add(m.getResource(pubURI), m.getProperty("http://vivo.deffopera.dk/ontology/osrap/notFoundForOrganization"), m.getResource(univURI));
            }
            return m;
        }
        
        private Map<String, String> getFoundDois(Model model) {
            Map<String, String> foundDois = new HashMap<String, String>();
            String queryStr = "SELECT ?s ?x WHERE { ?s <" + XmlToRdf.GENERIC_NS + "doi> ?x } ";
            QueryExecution qe = QueryExecutionFactory.create(queryStr, model);
            try {
                ResultSet rs = qe.execSelect();
                while(rs.hasNext()) {
                    QuerySolution qsoln = rs.next();
                    RDFNode sNode = qsoln.get("s");
                    if(!sNode.isURIResource()) {
                        continue;
                    } else {
                        String sURI = sNode.asResource().getURI();
                        foundDois.put(qsoln.get("x").asLiteral().getLexicalForm().toLowerCase(), sURI);
                    }
                }
                return foundDois;
            } finally {
                qe.close();
            }
        }

        @Override
        public Integer size() {
            return doiFiles.size();
        }

        @Override
        public void close() {
            // no API method for logging out; nothing to do for now
        }
        
        long lastRequest = 0;
        
        private String getDslResponse(String dslQuery, String token) 
                throws InterruptedException {
            long now = System.currentTimeMillis();
            long toWait = REQUEST_INTERVAL - (now - lastRequest);        
            if(toWait > 0) {
                Thread.sleep(toWait);
            }
            lastRequest = System.currentTimeMillis();
            return httpUtils.getHttpPostResponse(DIMENSIONS_API + "dsl.json",
                    dslQuery, "application/json", token);        
        }
        
        private Model toRdf(String data) {                
            try {
                JsonToXMLConverter json2xml = new JsonToXMLConverter();
                XmlToRdf xml2rdf = new XmlToRdf();
                RdfUtils rdfUtils = new RdfUtils();
                JSONObject jsonObj = new JSONObject(data);
                JSONArray pubs = jsonObj.getJSONArray("publications");
                for(int pubi = 0; pubi < pubs.length(); pubi++) {
                    JSONObject pub = pubs.getJSONObject(pubi);
                    JSONArray authors = pub.getJSONArray("authors");
                    for(int authi = 0; authi < authors.length(); authi++) {
                        JSONObject author = authors.getJSONObject(authi);
                        author.put("authorRank", authi + 1);
                    }                    
                }
                data = jsonObj.toString(2);
                String xml = json2xml.convertJsonToXml(data);
                Model rdf = xml2rdf.toRDF(xml);
                rdf = rdfUtils.renameBNodes(rdf, ABOX + "n", rdf);
                rdf = renameByIdentifier(rdf, rdf.getProperty(
                        XmlToRdf.GENERIC_NS + "id"), ABOX, "");
                return rdf;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        private String getPubs(List<String> dois, String token) throws InterruptedException {
            String queryStr = "search publications where"
                    + " (year >= 2014 and year <= 2017 and ";
            if(dois.size() == 1) {
                queryStr += "doi = \"" + dois.get(0) + "\" ";
            } else {
                queryStr += "( ";
                boolean first = true;
                for (String doi : dois) {
                    if(!first) {
                        queryStr += " or ";    
                    }
                    queryStr += "doi = \"" + doi + "\" ";                    
                    first = false;
                }
                queryStr += ") ";
            }
                    queryStr += "and type in [\"article\", \"chapter\", \"proceeding\"])"
                    + " return publications[id + type + title + authors + doi + pmid + pmcid + date + year + mesh_terms + journal + issn + volume + issue]"
                    + " limit 100";
            log.info(queryStr);
            return getDslResponse(queryStr, token);
        }
                
    }

}
