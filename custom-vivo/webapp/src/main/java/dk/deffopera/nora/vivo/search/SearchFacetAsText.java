package dk.deffopera.nora.vivo.search;

public class SearchFacetAsText extends SearchFacet {

    public SearchFacetAsText(String fieldName, String publicName) {
        super(fieldName, publicName);
    }
    
    @Override
    public boolean isFacetAsText() {
        return true;
    }
    
}
