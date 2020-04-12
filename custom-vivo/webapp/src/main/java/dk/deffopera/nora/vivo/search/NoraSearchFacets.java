package dk.deffopera.nora.vivo.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class NoraSearchFacets {

    private static List<SearchFacet> commonSearchFacets = new ArrayList<SearchFacet>();
    private static List<SearchFacet> additionalSearchFacets = new ArrayList<SearchFacet>();
    private static List<SearchFacetAsText> searchFacetsAsText = new ArrayList<SearchFacetAsText>();
    private static Map<String, SearchFacet> facetsByFieldName = new HashMap<String, SearchFacet>();
            
    static {
        // facets common to all record types
        commonSearchFacets.add(new SearchFacet("facet_content-type_ss", "Record Types"));
        commonSearchFacets.add(new SearchFacet("facet_year_ss", "Years"));
        commonSearchFacets.add(new SearchFacet("facet_university_ss", "Danish Universities"));
        commonSearchFacets.add(new SearchFacet("facet_hospital_ss", "Danish Hospitals"));
        commonSearchFacets.add(new SearchFacet("facet_organization_ss", "Other Organizations"));
        commonSearchFacets.add(new SearchFacet("facet_funder_ss", "Funders"));
        commonSearchFacets.add(new SearchFacet("facet_research-category_ss", "Subject Categories"));
            
        // Allow faceting by contributor by an appropriate request, but do 
        // not display the facet in the sidebar
        SearchFacet contributor = new SearchFacet("facet_contributor_ss", "Contributor");
        contributor.setDisplayInSidebar(false);
        commonSearchFacets.add(contributor);
        
        // facets not indexed for all record types
        additionalSearchFacets.add(new SearchFacet("facet_document-type_ss", "Document Types"));        
        additionalSearchFacets.add(new SearchFacet("facet_publication-year_ss", "Publication Years"));
        additionalSearchFacets.add(new SearchFacet("facet_start-year_ss", "Start Year"));
        additionalSearchFacets.add(new SearchFacet("facet_granted-year_ss", "Granted Years"));        
        additionalSearchFacets.add(new SearchFacet("facet_journal_ss", "Journals"));
        additionalSearchFacets.add(new SearchFacet("facet_publisher_ss", "Publishers"));
        additionalSearchFacets.add(new SearchFacet("facet_open-access_ss", "Open Access"));
        additionalSearchFacets.add(new SearchFacet("facet_repository_ss", "Repository"));
        additionalSearchFacets.add(new SearchFacet("facet_grant-status_ss", "Grant Status"));
        additionalSearchFacets.add(new SearchFacet("facet_original-assignee_ss", "Original Assignee"));        
        additionalSearchFacets.add(new SearchFacet("facet_jurisdiction_ss", "Jurisdiction"));        
        additionalSearchFacets.add(new SearchFacet("facet_filing-status_ss", "Filing Status"));
        additionalSearchFacets.add(new SearchFacet("facet_legal-status_ss", "Legal Status"));
        additionalSearchFacets.add(new SearchFacet("facet_sponsor-collaborator_ss", "Sponsor/collaborator"));
        additionalSearchFacets.add(new SearchFacet("facet_phase_ss", "Phase"));                
        additionalSearchFacets.add(new SearchFacet("facet_retrieval_ss", "Retrieval"));
        
        List<SearchFacet> allFacets = new ArrayList<SearchFacet>();
        allFacets.addAll(commonSearchFacets);
        allFacets.addAll(additionalSearchFacets);
        for(SearchFacet facet : allFacets) {
            facetsByFieldName.put(facet.getFieldName(), facet);
            String textFieldName = facet.getFieldName().replaceAll(
                    Pattern.quote("_ss"), "_en")
                    .replaceAll(Pattern.quote("facet_"), "facetext_");
            SearchFacetAsText fat = new SearchFacetAsText(
                    textFieldName, facet.getPublicName());
            searchFacetsAsText.add(fat);
            facetsByFieldName.put(textFieldName, fat);
        }
    }
    
    public static List<SearchFacet> getCommonSearchFacets() {
        ArrayList<SearchFacet> facets = new ArrayList<SearchFacet>();
        for(SearchFacet sf : commonSearchFacets) {
            facets.add(new SearchFacet(sf.getFieldName(), sf.getPublicName()));
        }
        return facets;
    }
    
    public static List<SearchFacet> getAdditionalSearchFacets() {
        ArrayList<SearchFacet> facets = new ArrayList<SearchFacet>();
        for(SearchFacet sf : additionalSearchFacets) {
            facets.add(new SearchFacet(sf.getFieldName(), sf.getPublicName()));
        }
        return facets;
    }
    
    public static List<SearchFacetAsText> getSearchFacetsAsText() {
        ArrayList<SearchFacetAsText> facets = new ArrayList<SearchFacetAsText>();
        for(SearchFacetAsText sf : searchFacetsAsText) {
            facets.add(new SearchFacetAsText(sf.getFieldName(), sf.getPublicName()));
        }
        return facets;
    }
    
    public static SearchFacet getSearchFacetByFieldName(String fieldName) {
        return facetsByFieldName.get(fieldName);
    }

    public static List<String> getFacetFields() {
        ArrayList<String> facets = new ArrayList<String>();
        for(SearchFacet sf : commonSearchFacets) {
            facets.add(sf.getFieldName());
        }
        for(SearchFacet sf : additionalSearchFacets) {
            facets.add(sf.getFieldName());
        }
        return facets;
    }
}
