package dk.deffopera.nora.vivo.etl.datasource.connector;

import org.apache.jena.rdf.model.Model;

import dk.deffopera.nora.vivo.etl.datasource.DataSource;
import dk.deffopera.nora.vivo.etl.datasource.DataSourceBase;

public class GraphClearer extends DataSourceBase implements DataSource {

    @Override
    protected void runIngest() throws InterruptedException {
        getSparqlEndpoint().clearGraph(getConfiguration().getResultsGraphURI());
    }

    @Override
    public Model getResult() {
        return null;
    }
    
}
