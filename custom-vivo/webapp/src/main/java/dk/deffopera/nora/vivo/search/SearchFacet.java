package dk.deffopera.nora.vivo.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a search facet with associated categories for display
 * on a search results page
 * @author Brian Lowe
 *
 */
public class SearchFacet {

    private String publicName;
    private String fieldName;
    private boolean displayInSidebar = true;
    private boolean isUnionFacet = false;
    private SearchFacet parentFacet = null;
    private List<SearchFacet> childFacets = new ArrayList<SearchFacet>();

    private List<SearchFacetCategory> categories = new ArrayList<SearchFacetCategory>();
    
    public SearchFacet(String fieldName, String publicName) {
        this.fieldName = fieldName;
        this.publicName = publicName;
    }
    
    public SearchFacet(String fieldName, String publicName, boolean displayInSidebar) {
        this(fieldName, publicName);
        this.displayInSidebar = displayInSidebar;
    }
    
    public SearchFacet(String fieldName, String publicName, 
            boolean displayInSidebar, boolean isUnionFacet) {
        this(fieldName, publicName, displayInSidebar);
        this.isUnionFacet = isUnionFacet;
    }
    
    public SearchFacet(String fieldName, String publicName, 
            boolean displayInSidebar, boolean isUnionFacet, 
            SearchFacet parentFacet) {
        this(fieldName, publicName, displayInSidebar, isUnionFacet);
        this.parentFacet = parentFacet;
        if(parentFacet != null) {
            parentFacet.addChildFacet(this);
        }
    }
    
    public SearchFacet clone() {
        return new SearchFacet(this.fieldName, this.publicName, 
                this.displayInSidebar, this.isUnionFacet, this.getParentFacet());        
    }
    
    /**
     * @return the name of the field used to store the facet in the search index
     */
    public String getPublicName() {
        return this.publicName;
    }
    
    /**
     * set the name of the field used to store the facet in the search index
     */
    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }
    
    /**
     * @return the name of the facet to display to the user
     */
    public String getFieldName() {
        return this.fieldName;
    }
    
    /**
     * Set the name of the facet to display to the user
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public List<SearchFacetCategory> getCategories() {
        return this.categories;
    }
    
    public boolean isFacetAsText() {
        return false;
    }
    
    public boolean isDisplayInSidebar() {
        return displayInSidebar;
    }

    public void setDisplayInSidebar(boolean displayInSidebar) {
        this.displayInSidebar = displayInSidebar;
    }
    
    /**
     * Should the values of this facet be ORed (including across 
     * the other union facets) rather than ANDed?
     * @return true if facet is a ORed facet, otherwise false
     */
    public boolean isUnionFacet() {
        return this.isUnionFacet;
    }
    
    public SearchFacet getParentFacet() {
        return this.parentFacet;
    }
    
    public void setParentFacet(SearchFacet parentFacet) {
        this.parentFacet = parentFacet;
    }
    
    public List<SearchFacet> getChildFacets() {
        return this.childFacets;
    }
    
    public void addChildFacet(SearchFacet childFacet) {
        for(int i = 0; i < this.childFacets.size(); i++) {
            if(childFacet.getFieldName().equals(
                    this.childFacets.get(i).getFieldName())) {
                this.childFacets.add(i, childFacet);
                return;
            }
        }
        this.childFacets.add(childFacet);
    }
    
}

