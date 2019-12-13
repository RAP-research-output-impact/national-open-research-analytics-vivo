package dk.deffopera.nora.vivo.etl.datasource.connector;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import dk.deffopera.nora.vivo.etl.datasource.DataSource;
import dk.deffopera.nora.vivo.etl.datasource.IteratorWithSize;

public class GraphClearer extends ConnectorDataSource implements DataSource {

    @Override
    protected IteratorWithSize<Model> getSourceModelIterator() {
        return new EmptyModelIterator();
    }
    
    private class EmptyModelIterator implements IteratorWithSize<Model> {

        boolean done = false;
        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public Model next() {
            done = true;
            return ModelFactory.createDefaultModel();
        }

        @Override
        public Integer size() {
            return 1;
        }

        @Override
        public void close() {
            // nada
        }
        
    }

    @Override
    protected Model filter(Model model) {
        return model;
    }

    @Override
    protected Model mapToVIVO(Model model) {
        return model;
    }

    @Override
    protected String getPrefixName() {
        return "";
    }

}
