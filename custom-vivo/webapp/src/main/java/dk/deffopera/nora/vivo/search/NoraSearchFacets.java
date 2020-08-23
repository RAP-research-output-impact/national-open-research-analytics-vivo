package dk.deffopera.nora.vivo.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class NoraSearchFacets {

    private static List<SearchFacet> commonSearchFacets = new ArrayList<SearchFacet>();
    private static List<SearchFacet> additionalSearchFacets = new ArrayList<SearchFacet>();
    private static List<SearchFacet> parentSearchFacets = new ArrayList<SearchFacet>();
    private static List<SearchFacetAsText> searchFacetsAsText = new ArrayList<SearchFacetAsText>();
    private static Map<String, SearchFacet> facetsByFieldName = new HashMap<String, SearchFacet>();
    private static final boolean DISPLAY_IN_SIDEBAR = true;
    private static final boolean IS_UNION_FACET = true;
            
    static {
        List<SearchFacet> allFacets = new ArrayList<SearchFacet>();
        
        // facets common to all record types
        SearchFacet recordTypes = new SearchFacet("facet_content-type_ss", "Record Types");
        recordTypes.setDisplayInSidebar(false);
        commonSearchFacets.add(recordTypes);
        commonSearchFacets.add(new SearchFacet("facet_year_ss", "Years", DISPLAY_IN_SIDEBAR, IS_UNION_FACET));
        
        SearchFacet danishOrgsParent = new SearchFacet("facet_organization-all_ss", "Danish Universities & Hospitals", DISPLAY_IN_SIDEBAR, IS_UNION_FACET);
        allFacets.add(danishOrgsParent);
        parentSearchFacets.add(danishOrgsParent);
        commonSearchFacets.add(new SearchFacet("facet_university_ss", "Danish Universities", DISPLAY_IN_SIDEBAR, IS_UNION_FACET, danishOrgsParent));
        commonSearchFacets.add(new SearchFacet("facet_hospital_ss", "Danish Hospitals", DISPLAY_IN_SIDEBAR, IS_UNION_FACET, danishOrgsParent));
        
        commonSearchFacets.add(new SearchFacet("facet_organization_ss", "Other Organizations", DISPLAY_IN_SIDEBAR, IS_UNION_FACET));
        commonSearchFacets.add(new SearchFacet("facet_funder_ss", "Funders"));
        commonSearchFacets.add(new SearchFacet("facet_main-subject-area_ss", "Main Subject Areas"));
        commonSearchFacets.add(new SearchFacet("facet_research-category_ss", "Subject Categories"));
            
        // Allow faceting by contributor by an appropriate request, but do 
        // not display the facet in the sidebar
        commonSearchFacets.add(new SearchFacet("facet_contributor_ss", "Contributor", !DISPLAY_IN_SIDEBAR));
                
        commonSearchFacets.add(new SearchFacet("facet_continent_ss", "Continents", DISPLAY_IN_SIDEBAR));
        commonSearchFacets.add(new SearchFacet("facet_country_ss", "Countries", DISPLAY_IN_SIDEBAR));
        
        // facets not indexed for all record types
        additionalSearchFacets.add(new SearchFacet("facet_document-type_ss", "Publication Types"));
        additionalSearchFacets.add(new SearchFacet("facet_publication-year_ss", "Publication Years", !DISPLAY_IN_SIDEBAR));
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
        additionalSearchFacets.add(new SearchFacet("facet_sdg_ss", "Sustainable Development Goals"));
        additionalSearchFacets.add(new SearchFacet("facet_retrieval_ss", "Retrieval"));
        
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
            facets.add(sf.clone());
        }
        return facets;
    }
    
    public static List<SearchFacet> getAdditionalSearchFacets() {
        ArrayList<SearchFacet> facets = new ArrayList<SearchFacet>();
        for(SearchFacet sf : additionalSearchFacets) {
            facets.add(sf.clone());
        }
        return facets;
    }
    
    public static List<SearchFacet> getAllSearchFacets() {
        ArrayList<SearchFacet> facets = new ArrayList<SearchFacet>();
        for(List<SearchFacet> facetList : Arrays.asList(commonSearchFacets, 
                additionalSearchFacets, parentSearchFacets)) {
            for(SearchFacet sf : facetList) {
                facets.add(sf.clone());
            }    
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
        for(List<SearchFacet> facetList : Arrays.asList(commonSearchFacets, 
                additionalSearchFacets, parentSearchFacets)) {
            for(SearchFacet sf : facetList) {
                facets.add(sf.getFieldName());
            }    
        }
        return facets;
    }
        
}
