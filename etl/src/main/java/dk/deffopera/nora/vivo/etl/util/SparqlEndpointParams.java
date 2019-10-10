package dk.deffopera.nora.vivo.etl.util;

public class SparqlEndpointParams {

    private String endpointURI;
    private String endpointUpdateURI;
    private String username;
    private String password;
    private String httpBasicAuthUsername;
    private String httpBasicAuthPassword;

    public String getEndpointURI() {
        return this.endpointURI;
    }

    public void setEndpointURI(String endpointURI) {
        this.endpointURI = endpointURI;
    }

    public String getEndpointUpdateURI() {
        return this.endpointUpdateURI;
    }

    public void setEndpointUpdateURI(String endpointUpdateURI) {
        this.endpointUpdateURI = endpointUpdateURI;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getHttpBasicAuthUsername() {
        return this.httpBasicAuthUsername;
    }
    
    public void setHttpBasicAuthUsername(String httpBasicAuthUsername) {
        this.httpBasicAuthUsername = httpBasicAuthUsername;
    }
    
    public String getHttpBasicAuthPassword() {
        return this.httpBasicAuthPassword;
    }
    
    public void setHttpBasicAuthPassword(String httpBasicAuthPassword) {
        this.httpBasicAuthPassword = httpBasicAuthPassword;
    }

}
