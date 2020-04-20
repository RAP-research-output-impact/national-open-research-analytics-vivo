package dk.deffopera.nora.vivo.search;

import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder.ParamMap;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.LinkTemplateModel;

public class SearchFacetCategory extends LinkTemplateModel {

    private final boolean selected;
    private final long count;
    
    public SearchFacetCategory(String label, ParamMap facetParams, long count) {
        this(label, facetParams, count, false);
    }
    
    public SearchFacetCategory(String label, ParamMap facetParams, long count, boolean selected) {
        super(label, "/search", facetParams);
        this.count = count;
        this.selected = selected;
    }
    
    public SearchFacetCategory(String label, String path, long count, boolean selected) {        
        super(label, path);
        this.count = count;
        this.selected = selected;
    }
    
    public long getCount() {
        return this.count;
    }
    
    public boolean isSelected() {
        return this.selected;
    }
    
}
