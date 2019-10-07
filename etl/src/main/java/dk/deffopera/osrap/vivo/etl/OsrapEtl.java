package dk.deffopera.osrap.vivo.etl;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dk.deffopera.osrap.vivo.etl.util.HttpUtils;

public class OsrapEtl {

    private static final String DIMENSIONS_API = "https://app.dimensions.ai/api/";
    
    private static final Log log = LogFactory.getLog(OsrapEtl.class);
    private HttpUtils httpUtils = new HttpUtils();
    
    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("Usage: <dimensions username> <dimensions password>");
            return;
        }
        OsrapEtl etl = new OsrapEtl();
        String token = etl.getToken(args[0], args[1]);
        System.out.println(token);
        String data = etl.getPubs(token);
        System.out.println(data);
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
                + " return publications"
                + " limit 10";
        return httpUtils.getHttpPostResponse(DIMENSIONS_API + "dsl.json",
                queryStr, "application/json", token);
    }
    
}


