package dk.deffopera.nora.vivo.etl.datasource.connector;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;

import dk.deffopera.nora.vivo.etl.datasource.DataSourceBase;
import dk.deffopera.nora.vivo.etl.datasource.DataSourceConfiguration;
import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;
import dk.deffopera.nora.vivo.etl.datasource.VivoVocabulary;

public abstract class ConnectorDataSource extends DataSourceBase {
    
    private static final Log log = LogFactory.getLog(ConnectorDataSource.class);
    /* number of iterator elements to be processed at once in memory 
    before being flushed to a SPARQL endpoint */
    protected final static int DEFAULT_BATCH_SIZE = 1;
    protected final static int TRIPLE_BUFFER_SIZE = 12500;
    private static final List<String> FILTER_OUT = Arrays.asList(
            "generalizedXMLtoRDF/0.1", "vitro/0.7#position", "vitro/0.7#value", "XMLSchema-instance");
    private static final String FILTER_OUT_RES = "match_nothing"; 
    
    protected Model result;
    
    /**
     * to be overridden by subclasses
     * @return Model representing a discrete "record" in the source data,
     * lifted to RDF but not (necessarily) mapped to the VIVO ontology or
     * filtered for relevance 
     */
    protected abstract IteratorWithSize<Model> getSourceModelIterator();
    
    /**
     * The number of table rows to be processed at once in memory before
     * being flushed to a SPARQL endpoint
     * @return
     */
    protected int getBatchSize() {
        return DEFAULT_BATCH_SIZE;
    }
    
    /**
     * to be overridden by subclasses
     * @param model
     * @return model filtered to relevant resources according to the query
     * terms or other criteria
     */
    protected abstract Model filter(Model model);
    
    /**
     * A filter that removes generic statements and types produced by
     * XML (or JSON) to RDF lifting
     * @param model
     * @return model with generic statements removed
     */
    protected Model filterGeneric(Model model) {
        Model filtered = ModelFactory.createDefaultModel();
        StmtIterator sit = model.listStatements();
        while(sit.hasNext()) {
            Statement stmt = sit.next();
            if( (RDF.type.equals(stmt.getPredicate()))
                        && (stmt.getObject().isURIResource())
                        && (stmt.getObject().asResource().getURI().contains(FILTER_OUT.get(0))) ) {                                   
                continue;     
            } 
            boolean filterPredicate = false;
            for (String filterOut : FILTER_OUT) {
                if(stmt.getPredicate().getURI().contains(filterOut)) {
                    filterPredicate = true;
                    break;
                }
            }
            if(filterPredicate) {
                continue;
            }
            if(stmt.getSubject().isURIResource() 
                    && stmt.getSubject().getURI().contains(FILTER_OUT_RES)) {
                continue;
            }
            if(stmt.getObject().isURIResource() 
                    && stmt.getObject().asResource().getURI().contains(FILTER_OUT_RES)) {
                continue;
            }
            filtered.add(stmt);
        }
        return filtered;
    }
    
    /**
     * to be overridden by subclasses
     * @param model
     * @return model with VIVO-compatible statements added
     */
    protected abstract Model mapToVIVO(Model model);
 
    @Override
    public void runIngest() throws InterruptedException {
        Date currentDateTime = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String graphTimeSuffix = "-" + df.format(currentDateTime);
        log.debug("Processing a limit of " + this.getConfiguration().getLimit() + " records");
        log.debug("Processing in batches of " + getBatchSize() + " records");
        String graphURI = getConfiguration().getResultsGraphURI();
        if(!activeEndpointForResults()) {
            result = ModelFactory.createDefaultModel();
        }
        IteratorWithSize<Model> it = getSourceModelIterator();
        try {
            Integer totalRecords = it.size();
            if(totalRecords != null) {
                this.getStatus().setTotalRecords(totalRecords);
                log.info(it.size() + " total records");
            }        
            Model buffer = ModelFactory.createDefaultModel();
            this.getStatus().setMessage("harvesting records");
            boolean dataWrittenToEndpoint = false;
            int count = 0;
            while(it.hasNext() && count < this.getConfiguration().getLimit()) {
                try {
                    if(this.getStatus().isStopRequested()) {
                        throw new InterruptedException();
                    }
                    count++;                
                    Model model = mapToVIVO(it.next());
                    log.debug(model.size() + " statements before filtering");
                    if(this.getStatus().isStopRequested()) {
                        throw new InterruptedException();
                    }
                    model = filter(model);
                    log.debug(model.size() + " statements after filtering");
                    if(this.getStatus().isStopRequested()) {
                        throw new InterruptedException();
                    }
                    String defaultNamespace = getDefaultNamespace(this.getConfiguration());
                    // TODO 2019-07-08 revisit because dangerous: rewrites geopolitical abox entities, etc.
                    //if(defaultNamespace != null) {                    
                        // model = rewriteUris(model, defaultNamespace, getPrefixName());
                    //}
                    if(this.getStatus().isStopRequested()) {
                        throw new InterruptedException();
                    }
                    if(activeEndpointForResults()) {
                        buffer.add(model);                
                        //if(count % getBatchSize() == 0 || !it.hasNext()
                        if(buffer.size() >= TRIPLE_BUFFER_SIZE || !it.hasNext()
                                || count == this.getConfiguration().getLimit()) {
                            if(buffer.size() > 0) {
                                dataWrittenToEndpoint = true;
                            }
                            log.debug("Adding " + buffer.size() + " triples to endpoint");
                            addToEndpoint(buffer, graphURI + graphTimeSuffix);
                            buffer.removeAll();
                        }
                    } else {
                        result.add(model);
                    }
                    this.getStatus().setProcessedRecords(count);                
                    if(totalRecords != null && totalRecords > 0) {
                        float completionPercentage = ((float) count / (float) totalRecords) * 100;
                        log.info("Completion percentage " + completionPercentage);
                        this.getStatus().setCompletionPercentage((int) completionPercentage);
                    }
                } catch (InterruptedException e) {
                    throw(e); // this is the one exception we want to throw 
                } catch (Exception e) {
                    log.error(e, e);
                    this.getStatus().setErrorRecords(this.getStatus().getErrorRecords() + 1);
                }
            }
            boolean skipClearingOldData = false;
            if(!dataWrittenToEndpoint) {
                if(totalRecords == null) {
                    skipClearingOldData = true;
                } else if (this.getStatus().getErrorRecords() > (totalRecords / 5)) {
                    skipClearingOldData = true;
                }
            }
            if(activeEndpointForResults() && !skipClearingOldData) {
                this.getStatus().setMessage("removing old data");
                log.info("removing old data");
                List<String> allVersionsOfSource = getGraphsWithBaseURI(graphURI, 
                        getSparqlEndpoint());
                for(String version : allVersionsOfSource) {
                    if(this.getStatus().isStopRequested()) {
                        throw new InterruptedException();
                    }
                    if(version.startsWith(graphURI) 
                            && !version.endsWith(graphTimeSuffix)) {
                        log.info("Clearing graph " + version);
                        getSparqlEndpoint().clearGraph(version);
                    }
                }
            }
        } finally {
            if(it != null) {
                log.info("Closing iterator");
                it.close();       
            }
        }        
    }
    
    protected String getDefaultNamespace(DataSourceConfiguration configuration) {
        Object o = configuration.getParameterMap().get("Vitro.defaultNamespace");
        if(o instanceof String) {
            return (String) o;
        } else {
            return null;
        }
    }

    protected boolean activeEndpointForResults() {
        return (this.getConfiguration().getEndpointParameters() != null 
                && this.getConfiguration().getResultsGraphURI() != null);
    }
    
    @Override
    public Model getResult() {
        return this.result;
    }
    
    protected Model renameByIdentifier(Model m, Property identifier, 
            String namespace, String localNamePrefix) {
        Map<Resource, String> idMap = new HashMap<Resource, String>();
        StmtIterator sit = m.listStatements(null, identifier, (RDFNode) null);
        while(sit.hasNext()) {
            Statement stmt = sit.next();
            if(stmt.getObject().isLiteral()) {
                idMap.put(stmt.getSubject(), 
                        stripSpaces(stmt.getObject().asLiteral().getLexicalForm()));
                        //stmt.getObject().asLiteral().getLexicalForm());
                        //stripNonWordChars(stmt.getObject().asLiteral().getLexicalForm()));
            }
        }
        for(Resource res : idMap.keySet()) {
            ResourceUtils.renameResource(
                    res, namespace + localNamePrefix + idMap.get(res));
        }
        return m;
    }
    
    protected String stripSpaces(String value) {
        if(value == null) {
            return value;
        } else {
            return value.replaceAll(" ", "");
        }
    }
    
    protected String stripNonWordChars(String value) {
        if(value == null) {
            return value;
        } else {
            return value.replaceAll("\\W", "");
        }
    }
    
    protected Model rewriteUris(Model model, String namespace, String localNamePrefix) {
        Model out = ModelFactory.createDefaultModel();
        StmtIterator sit = model.listStatements();
        while(sit.hasNext()) {
            Statement stmt = sit.next();
            Resource subj = rewriteResource(stmt.getSubject(), namespace, localNamePrefix);
            RDFNode obj = stmt.getObject();
            if( (!stmt.getPredicate().equals(RDF.type)) && (obj.isURIResource()) ) {
                obj = rewriteResource(obj.asResource(), namespace, localNamePrefix);
            }
            out.add(subj, stmt.getPredicate(), obj);
        }
        return out;
    }
    
    protected Resource rewriteResource(Resource res, String namespace, String localNamePrefix) {
        if(!res.isURIResource()) {
            return res;
        }
        // don't rewrite ORCID iDs, VIVO individuals or resources that are 
        // already in the default namespace
        if(res.getURI().startsWith(namespace) 
                || res.getURI().contains("orcid.org") 
                || res.getURI().startsWith(VivoVocabulary.VIVO)) {
            return res;
        }
        try {
            URI uri = new URI(res.getURI());
            String newLocalName = localNamePrefix + uri.getPath();
            newLocalName = newLocalName.replaceAll("/",  "-");
            return ResourceFactory.createResource(namespace + newLocalName);
        } catch (URISyntaxException e) {
            log.debug(e, e);
            return res;
        }                
    }
    
    protected abstract String getPrefixName();
    
}
