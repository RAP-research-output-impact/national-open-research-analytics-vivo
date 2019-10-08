package dk.deffopera.osrap.vivo.etl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;
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
    
    private static final Log log = LogFactory.getLog(OsrapEtl.class);
    private HttpUtils httpUtils = new HttpUtils();
    
    public static void main(String[] args) {
        if(args.length < 3) {
            System.out.println("Usage: <dimensions username> <dimensions password> <output file path>");
            return;
        }
        OsrapEtl etl = new OsrapEtl();
        String token = etl.getToken(args[0], args[1]);
        String filepath = args[2];
        System.out.println(token);
        String data = etl.getPubs(token);
        //System.out.println(data);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataObj;
        try {
            dataObj = mapper.readTree(data);
            System.out.println(mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dataObj));
            JsonToXMLConverter json2xml = new JsonToXMLConverter();
            XmlToRdf xml2rdf = new XmlToRdf();
            RdfUtils rdfUtils = new RdfUtils();
            String xml = json2xml.convertJsonToXml(data);
            Model rdf = xml2rdf.toRDF(xml);
            rdf = rdfUtils.renameBNodes(rdf, ABOX + "n", rdf);
            rdf = renameByIdentifier(rdf, rdf.getProperty(
                    XmlToRdf.GENERIC_NS + "id"), ABOX, "");
            OutputStream out = new FileOutputStream(new File(filepath));
            rdf.write(out, "TTL");
            rdf.write(System.out, "TTL");
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
    
    private String getPubs(String token) {
        String queryStr = "search publications where"
                + " research_orgs.id = \"grid.5170.3\""
                //+ " return publications"
                + " return publications[id + type + title + authors + author_affiliations + doi + pmid + pmcid + date + year + mesh_terms + journal + issn + volume + issue]"
                + " limit 10";
        return httpUtils.getHttpPostResponse(DIMENSIONS_API + "dsl.json",
                queryStr, "application/json", token);
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


