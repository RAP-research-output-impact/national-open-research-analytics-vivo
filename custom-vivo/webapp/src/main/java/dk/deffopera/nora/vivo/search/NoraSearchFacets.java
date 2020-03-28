package dk.deffopera.nora.vivo.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class NoraSearchFacets {

    private static List<SearchFacet> searchFacets = new ArrayList<SearchFacet>();
    private static List<SearchFacetAsText> searchFacetsAsText = new ArrayList<SearchFacetAsText>();
    private static Map<String, SearchFacet> facetsByFieldName = new HashMap<String, SearchFacet>();
    
    static {
        searchFacets.add(new SearchFacet("facet_document-type_ss", "Document types"));
        searchFacets.add(new SearchFacet("facet_publication-year_ss", "Publication years"));
        searchFacets.add(new SearchFacet("facet_university_ss", "Universities"));
        searchFacets.add(new SearchFacet("facet_hospital_ss", "Hospitals"));
        searchFacets.add(new SearchFacet("facet_organization_ss", "Organizations"));
        searchFacets.add(new SearchFacet("facet_journal_ss", "Journals"));
        searchFacets.add(new SearchFacet("facet_publisher_ss", "Publisher"));
        searchFacets.add(new SearchFacet("facet_open-access_ss", "Open Access"));
        searchFacets.add(new SearchFacet("facet_repository_ss", "Repository"));
        searchFacets.add(new SearchFacet("facet_funder_ss", "Funder"));
        searchFacets.add(new SearchFacet("facet_retrieval_ss", "Retrieval"));
        searchFacets.add(new SearchFacet("facet_research-category_ss", "Research Category"));
        searchFacets.add(new SearchFacet("facet_grant-status_ss", "Status"));
        searchFacets.add(new SearchFacet("facet_original-assignee_ss", "Original Assignee"));
        searchFacets.add(new SearchFacet("facet_jurisdiction_ss", "Jurisdiction"));        
        searchFacets.add(new SearchFacet("facet_filing-status_ss", "Filing Status"));
        searchFacets.add(new SearchFacet("facet_legal-status_ss", "Legal Status"));
        searchFacets.add(new SearchFacet("facet_phase_ss", "Phase"));
        searchFacets.add(new SearchFacet("facet_sponsor-collaborator_ss", "Sponsor/collaborator"));
        for(SearchFacet facet : searchFacets) {
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
    
    public static List<SearchFacet> getSearchFacets() {
        ArrayList<SearchFacet> facets = new ArrayList<SearchFacet>();
        for(SearchFacet sf : searchFacets) {
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
        for(SearchFacet sf : searchFacets) {
            facets.add(sf.getFieldName());
        }
        return facets;
    }
}
