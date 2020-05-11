package dk.deffopera.nora.vivo.search;

import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder.ParamMap;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.LinkTemplateModel;

public class SearchFacetCategory extends LinkTemplateModel {

    private final boolean selected;
    private final long count;
    private final String value;
    
    public SearchFacetCategory(String label, ParamMap facetParams, long count) {
        this(label, null, facetParams, count, false);
    }
    
    public SearchFacetCategory(String label, String value, ParamMap facetParams, 
            long count) {
        this(label, value, facetParams, count, false);
    }
    
    public SearchFacetCategory(String label, String value, ParamMap facetParams, 
            long count, boolean selected) {
        super(label, "/search", facetParams);
        this.value = value;
        this.count = count;
        this.selected = selected;
    }
    
    public SearchFacetCategory(String label, String value, String path, 
            long count, boolean selected) {        
        super(label, path);
        this.value = value;
        this.count = count;
        this.selected = selected;
    }
    
    public String getValue() {
        return this.value;
    }
    
    public long getCount() {
        return this.count;
    }
    
    public boolean isSelected() {
        return this.selected;
    }
    
}
