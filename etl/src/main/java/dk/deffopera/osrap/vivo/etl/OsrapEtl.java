package dk.deffopera.osrap.vivo.etl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dk.deffopera.osrap.vivo.etl.util.HttpUtils;
import dk.deffopera.osrap.vivo.etl.util.JsonToXMLConverter;
import dk.deffopera.osrap.vivo.etl.util.RdfUtils;
import dk.deffopera.osrap.vivo.etl.util.XmlToRdf;

public class OsrapEtl {

    private static final String DIMENSIONS_API = "https://app.dimensions.ai/api/";
    private static final String ABOX = "http://vivo.deffopera.dk/individual/";
    private static final long REQUEST_INTERVAL = 2000; // ms
    
    private static final Log log = LogFactory.getLog(OsrapEtl.class);
    private HttpUtils httpUtils = new HttpUtils();
    
    public static void main(String[] args) throws InterruptedException {
        if(args.length < 3) {
            System.out.println("Usage: <dimensions username> <dimensions password> <output file path>");
            return;
        }
        OsrapEtl etl = new OsrapEtl();
        String token = etl.getToken(args[0], args[1]);
        String filepath = args[2];
        Model rdf = ModelFactory.createDefaultModel();        
        rdf.add(etl.toRdf(etl.getPubs(token)));        
        rdf.add(etl.toRdf(etl.getGrants(token)));
        rdf.add(etl.toRdf(etl.getPatents(token)));
        rdf.add(etl.toRdf(etl.getClinicalTrials(token)));
        OutputStream out;
        try {
            out = new FileOutputStream(new File(filepath));
            rdf.write(out, "TTL");
            log.info("Wrote " + rdf.size() + " triples to " + filepath);
        } catch (FileNotFoundException e) {
            log.error(e, e);
            throw new RuntimeException(e);
        }        
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
            //ObjectMapper mapper = new ObjectMapper();
            //JsonNode dataObj;
            //dataObj = mapper.readTree(data);
            //System.out.println(mapper.writerWithDefaultPrettyPrinter()
            //        .writeValueAsString(dataObj));
            JsonToXMLConverter json2xml = new JsonToXMLConverter();
            XmlToRdf xml2rdf = new XmlToRdf();
            RdfUtils rdfUtils = new RdfUtils();
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
    
    private String getToken(String username, String password) {
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
    
    private String getGrants(String token) throws InterruptedException {
        String queryStr = "search grants where"
                + " (start_year >= 2014 and start_year <= 2017 and research_orgs.id = \"grid.5170.3\")"
                + " return grants[active_year + end_date + funders + id + project_num + start_date + start_year + title + abstract + funding_eur + grant_number + investigator_details + research_orgs]"
                + " limit 100";
        return getDslResponse(queryStr, token);
    }
    
    private String getPubs(String token) throws InterruptedException {
        String queryStr = "search publications where"
                + " (year >= 2014 and year <= 2017 and research_orgs.id = \"grid.5170.3\" and type in [\"article\", \"chapter\", \"proceeding\"])"
                //+ " return publications"
                + " return publications[id + type + title + authors + doi + pmid + pmcid + date + year + mesh_terms + journal + issn + volume + issue]"
                + " limit 100";
        return getDslResponse(queryStr, token);
    }
    
    private String getPatents(String token) throws InterruptedException {
        String queryStr = "search patents where"
                + " (granted_year >= 2014 and granted_year <= 2017 and assignees.id = \"grid.5170.3\")"
                + " return patents[assignees + granted_year + id + title + inventor_names + funders + abstract + associated_grant_ids]"
                + " limit 100";
        return getDslResponse(queryStr, token);
    }
    
    private String getClinicalTrials(String token) throws InterruptedException {
        String queryStr = "search clinical_trials where"
                + " (active_years >= 2014 and active_years <= 2017 and organizations.id = \"grid.5170.3\")"
                + " return clinical_trials"
                + " limit 100";
        return getDslResponse(queryStr, token);
    }
    
    protected static Model renameByIdentifier(Model m, Property identifier, 
            String namespace, String localNamePrefix) {
        Map<Resource, String> idMap = new HashMap<Resource, String>();
        StmtIterator sit = m.listStatements(null, identifier, (RDFNode) null);
        while(sit.hasNext()) {
            Statement stmt = sit.next();
            if(stmt.getObject().isLiteral()) {
                idMap.put(stmt.getSubject(), 
                        //stripNonWordChars(stmt.getObject().asLiteral().getLexicalForm()));
                        stmt.getObject().asLiteral().getLexicalForm());
            }
        }
        for(Resource res : idMap.keySet()) {
            ResourceUtils.renameResource(
                    res, namespace + localNamePrefix + idMap.get(res));
        }
        return m;
    }
    
    protected static String stripNonWordChars(String value) {
        if(value == null) {
            return value;
        } else {
            return value.replaceAll("\\W", "");
        }
    }
    
}


