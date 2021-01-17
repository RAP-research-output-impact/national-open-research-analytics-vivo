/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.search.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

import dk.deffopera.nora.vivo.search.NoraSearchFacets;
import dk.deffopera.nora.vivo.search.Param;
import dk.deffopera.nora.vivo.search.SearchFacet;
import dk.deffopera.nora.vivo.search.SearchFacetCategory;
import edu.cornell.mannlib.vitro.webapp.application.ApplicationUtils;
import edu.cornell.mannlib.vitro.webapp.beans.ApplicationBean;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.VClass;
import edu.cornell.mannlib.vitro.webapp.beans.VClassGroup;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.FreemarkerHttpServlet;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder.ParamMap;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ExceptionResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.dao.IndividualDao;
import edu.cornell.mannlib.vitro.webapp.dao.VClassDao;
import edu.cornell.mannlib.vitro.webapp.dao.VClassGroupDao;
import edu.cornell.mannlib.vitro.webapp.dao.VClassGroupsForRequest;
import edu.cornell.mannlib.vitro.webapp.dao.VitroVocabulary;
import edu.cornell.mannlib.vitro.webapp.dao.jena.VClassGroupCache;
import edu.cornell.mannlib.vitro.webapp.i18n.I18n;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchEngine;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchFacetField;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchFacetField.Count;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchQuery;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchQuery.Order;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchResponse;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchResultDocument;
import edu.cornell.mannlib.vitro.webapp.modules.searchEngine.SearchResultDocumentList;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFServiceException;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ResultSetConsumer;
import edu.cornell.mannlib.vitro.webapp.search.VitroSearchTermNames;
import edu.cornell.mannlib.vitro.webapp.search.controller.PagedSearchController.YearComparator;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.LinkTemplateModel;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.searchresult.IndividualSearchResult;
import edu.ucsf.vitro.opensocial.OpenSocialManager;

/**
 * Paged search controller that uses the search engine
 */

public class PagedSearchController extends FreemarkerHttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(PagedSearchController.class);

    protected static final int DEFAULT_HITS_PER_PAGE = 10;
    protected static final int DEFAULT_MAX_HIT_COUNT = 1000;
    protected static final int FACET_LIMIT = 15;

    private static final String PARAM_XML_REQUEST = "xml";
    private static final String PARAM_CSV_REQUEST = "csv";
    private static final String PARAM_JSON_REQUEST = "json";
    private static final String PARAM_START_INDEX = "startIndex";
    private static final String PARAM_HITS_PER_PAGE = "hitsPerPage";
    private static final String PARAM_CLASSGROUP = "classgroup";
    private static final String PARAM_RDFTYPE = "type";
    private static final String PARAM_SEARCHMODE = "searchMode";
    private static final String PARAM_RECORD_TYPE = "facet_content-type_ss";
    private static final String PARAM_SORTFIELD = "sortField";
    private static final String VALUE_DELIMITER = ";;";
    private static final String GROUP_DELIMITER = "||";
    // Nora make this field public
    public static final String PARAM_QUERY_TEXT = "querytext";
    public static final String FACET_FIELD_PREFIX = "facet_";
    public static final String PARAM_FACET_AS_TEXT = "facetAsText";
    public static final String PARAM_FACET_TEXT_VALUE = "facetTextValue";   
    
    protected static final String LABEL_ABBR_QUERY = "SELECT ?label ?abbr WHERE { \n"
                                                   + "  ?x <" + RDFS.label.getURI() + "> ?label \n"
                                                   + "  OPTIONAL { ?x <http://vivoweb.org/ontology/core#abbreviation> ?abbr } \n"
                                                   + "} \n";     

    protected static final Map<Format,Map<Result,String>> templateTable;

    protected enum Format {
        HTML, XML, CSV;
    }

    protected enum Result {
        PAGED, ERROR, BAD_QUERY
    }

    static{
        templateTable = setupTemplateTable();
    }

    /**
     * Overriding doGet from FreemarkerHttpController to do a page template (as
     * opposed to body template) style output for XML requests.
     *
     * This follows the pattern in AutocompleteController.java.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        VitroRequest vreq = new VitroRequest(request);
        boolean wasXmlRequested = isRequestedFormat(PARAM_XML_REQUEST, vreq);
        boolean wasCSVRequested = isRequestedFormat(PARAM_CSV_REQUEST, vreq);
        boolean wasJsonRequested = isRequestedFormat(PARAM_JSON_REQUEST, vreq);
        if( !wasXmlRequested && !wasCSVRequested && !wasJsonRequested){
            super.doGet(vreq,response);
        }else if(wasJsonRequested) {            
            writeJson(vreq, response);          
        }else if (wasXmlRequested){
            try {
                ResponseValues rvalues = processRequest(vreq);

                response.setCharacterEncoding("UTF-8");
                response.setContentType("text/xml;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=search.xml");
                writeTemplate(rvalues.getTemplateName(), rvalues.getMap(), request, response);
            } catch (Exception e) {
                log.error(e, e);
            }
        }else if (wasCSVRequested){
        	try {
                ResponseValues rvalues = processRequest(vreq);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("text/csv;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=search.csv");
                writeTemplate(rvalues.getTemplateName(), rvalues.getMap(), request, response);
            } catch (Exception e) {
                log.error(e, e);
            }
        }
    }
    
    private String getJsonFacet(VitroRequest vreq) {
        return vreq.getParameter("jsonFacet");    
    }
    
    private int getFacetLimit(VitroRequest vreq) {
        String facetLimitStr = vreq.getParameter("facetLimit");
        if(facetLimitStr != null) {
            try {
                return Integer.parseInt(facetLimitStr, 10);
            } catch (NumberFormatException e) {
                log.error("Ignoring invalid facetLimit integer " + facetLimitStr);
            }
        }
        return FACET_LIMIT;
    }
    
    private int getFacetOffset(VitroRequest vreq) {
        String facetOffsetStr = vreq.getParameter("facetOffset");
        if(facetOffsetStr != null) {
            try {
                return Integer.parseInt(facetOffsetStr, 10);
            } catch (NumberFormatException e) {
                log.error("Ignoring invalid facetOffset integer " + facetOffsetStr);
            }
        }
        return 0;
    }
            
    
    private void writeJson(VitroRequest vreq, 
            HttpServletResponse response) {
        try {
            String facetFieldName = getJsonFacet(vreq);
            if(facetFieldName == null) {
                facetFieldName = "facet_content-type_ss";
            }
            ResponseValues rvalues = processRequest(vreq);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            StringWriter sw = new StringWriter();
            JSONArray json = new JSONArray();
            SearchFacet facet = null;
            for(String facetListName : Arrays.asList("commonFacets", 
                    "additionalFacets")) {
                Object facetListObj = rvalues.getMap().get(facetListName);
                if(facetListObj instanceof List) {
                    List facetList = (List) facetListObj;
                    for(Object facetObj : facetList) {
                        if(facetObj instanceof SearchFacet) {
                            SearchFacet sFacet = (SearchFacet) facetObj;
                            if(facetFieldName.equals(sFacet.getFieldName())) {
                                facet = sFacet;
                                break;
                            }
                        }
                    }
                }
            }
            if(facet != null) {
                int i = -1;
                int facetOffset = getFacetOffset(vreq);
                for(SearchFacetCategory cat : facet.getCategories()) {
                    i++;
                    if(i < facetOffset) {
                        continue;
                    }
                    JSONObject catJson = new JSONObject();
                    catJson.put("text", cat.getText());
                    catJson.put("url", cat.getUrl());
                    catJson.put("value", cat.getValue());
                    catJson.put("count", cat.getCount());
                    json.put(catJson);
                }
            }
            sw.write(json.toString(2));
            write(sw, response, 200);
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException(e);
        }  
    }

    @Override
    protected ResponseValues processRequest(VitroRequest vreq) {    	    	
    	
        //There may be other non-html formats in the future
        Format format = getFormat(vreq);
        boolean wasXmlRequested = Format.XML == format;
        boolean wasCSVRequested = Format.CSV == format;
        log.debug("Requested format was " + (wasXmlRequested ? "xml" : "html"));
        boolean wasHtmlRequested = ! (wasXmlRequested || wasCSVRequested);

        try {

            //make sure an IndividualDao is available
            if( vreq.getWebappDaoFactory() == null
                    || vreq.getWebappDaoFactory().getIndividualDao() == null ){
                log.error("Could not get webappDaoFactory or IndividualDao");
                throw new Exception("Could not access model.");
            }
            IndividualDao iDao = vreq.getWebappDaoFactory().getIndividualDao();
            VClassGroupDao grpDao = vreq.getWebappDaoFactory().getVClassGroupDao();
            VClassDao vclassDao = vreq.getWebappDaoFactory().getVClassDao();

            ApplicationBean appBean = vreq.getAppBean();

            log.debug("IndividualDao is " + iDao.toString() + " Public classes in the classgroup are " + grpDao.getPublicGroupsWithVClasses().toString());
            log.debug("VClassDao is "+ vclassDao.toString() );

            int startIndex = getStartIndex(vreq);
            int hitsPerPage = getHitsPerPage( vreq );

            String queryText = vreq.getParameter(PARAM_QUERY_TEXT);
            log.debug("Query text is \""+ queryText + "\"");

// Nora: allow empty searches
            if(queryText == null) {
                queryText = "";
            }
//            String badQueryMsg = badQueryText( queryText, vreq );
//            if( badQueryMsg != null ){
//                return doFailedSearch(badQueryMsg, queryText, format, vreq);
//            }

            
            SearchEngine search = ApplicationUtils.instance().getSearchEngine();

            // If we were talking directly to Solr, these first two queries
            // could return only facets and not matching documents, but we 
            // don't currently have that option through the VIVO search engine
            // interface.
            
            List<String> unionFacetNames = getUnionFacetNames();
            
            // Do a search query with the union facets also excluded from the
            // filtering so that we can get all possible results
            SearchQuery allRecordTypesQuery = getQuery(queryText, Arrays.asList(
                    "facet_content-type_ss"), unionFacetNames, 
                    hitsPerPage, startIndex, vreq);
            SearchResponse allRecordTypesResponse = null;
            
            // Do a search query with the union facets also excluded from the
            // filtering so that we can get all possible results
//            SearchQuery allOrgsQuery = getQuery(queryText, unionFacetNames, unionFacetNames, 
//                    hitsPerPage, startIndex, vreq);
//            SearchResponse allOrgsResponse = null;
            
            // Do the main query with the facet filters.
            SearchQuery mainQuery = getQuery(queryText, Arrays.asList(""), unionFacetNames, 
                    hitsPerPage, startIndex, vreq);
            search = ApplicationUtils.instance().getSearchEngine();
            SearchResponse mainResponse = null;

            try {
                allRecordTypesResponse = search.query(allRecordTypesQuery);
                //allOrgsResponse = search.query(allOrgsQuery);
                mainResponse = search.query(mainQuery);                
            } catch (Exception ex) {
                String msg = makeBadSearchMessage(queryText, ex.getMessage(), vreq);
                log.error("could not run search query",ex);
                return doFailedSearch(msg, queryText, format, vreq);
            }

            if (mainResponse == null) {
                log.error("Search response was null");
                return doFailedSearch(I18n.text(
                        vreq, "error_in_search_request"), queryText, format, vreq);
            }
            
            SearchResultDocumentList docs = mainResponse.getResults();
            if (docs == null) {
                log.error("Document list for a search was null");
                return doFailedSearch(I18n.text(vreq, "error_in_search_request"), queryText,format, vreq);
            }

            long hitCount = docs.getNumFound();
            log.debug("Number of hits = " + hitCount);
            if ( hitCount < 1 ) {
                return doNoHits(queryText, format, vreq);
            }
            
            Map<String, Object> body = new HashMap<String, Object>();
            
            // Nora
            body.put("typeCounts", getTypeCounts(allRecordTypesResponse, vreq, queryText));
            body.put("allTypesLink", getAllTypesLink(vreq, queryText));
            int totalEntities = getTotalEntities(vreq);
            body.put("totalEntities", totalEntities);
            //if(totalEntities == hitCount) {
            //    body.put("allRecordsSelected", true);
            //}
            String searchMode = getParamSearchMode(vreq);
            body.put(PARAM_SEARCHMODE, searchMode);
            List<SearchFacet> commonSearchFacets = getFacetLinks(
                    NoraSearchFacets.getCommonSearchFacets(), vreq, mainResponse, queryText); 
            if(!"all".equals(searchMode)) {
                body.put("additionalFacets", getFacetLinks(
                        NoraSearchFacets.getAdditionalSearchFacets(), vreq, mainResponse, queryText));
            } else {
                body.put("allRecordsSelected", true);
            }
            body.put("facetsAsText", NoraSearchFacets.getSearchFacetsAsText());
            body.put(PARAM_FACET_AS_TEXT, vreq.getParameter(PARAM_FACET_AS_TEXT));
            body.put(PARAM_FACET_TEXT_VALUE, vreq.getParameter(PARAM_FACET_TEXT_VALUE));
            body.put("noraQueryReduce", getNoraQueryReduceLinks(vreq, grpDao, vclassDao));            
            
            List<SearchFacet> filteredSearchFacets = getFacetLinks(
                    NoraSearchFacets.getCommonSearchFacets(), vreq, mainResponse, queryText);
            // overlay the non-union facets with their new values
            List<SearchFacet> mergedFacets = new ArrayList<SearchFacet>(); 
            for(SearchFacet cf : commonSearchFacets) {
                if(cf.isUnionFacet()) {
                    mergedFacets.add(cf);
                } else {
                    for(SearchFacet filtered : filteredSearchFacets) {
                        if(filtered.getFieldName().equals(cf.getFieldName())) {
                            mergedFacets.add(filtered);
                        }
                    }
                }
            }
            body.put("commonFacets", mergedFacets);

            List<Individual> individuals = new ArrayList<Individual>(docs.size());
            Iterator<SearchResultDocument> docIter = docs.iterator();
            while( docIter.hasNext() ){
                try {
                    SearchResultDocument doc = docIter.next();
                    String uri = doc.getStringValue(VitroSearchTermNames.URI);
                    Individual ind = iDao.getIndividualByURI(uri);
                    if(ind != null) {
                      ind.setSearchSnippet( getSnippet(doc, mainResponse) );
                      individuals.add(ind);
                    }
                } catch(Exception e) {
                    log.error("Problem getting usable individuals from search hits. ",e);
                }
            }

//          ParamMap pagingLinkParams = new ParamMap();
            ParamMap pagingLinkParams = NoraGetQueryParamMap(vreq);
            pagingLinkParams.put(PARAM_QUERY_TEXT, queryText);
            pagingLinkParams.put(PARAM_HITS_PER_PAGE, String.valueOf(hitsPerPage));

            if( wasXmlRequested ){
                pagingLinkParams.put(PARAM_XML_REQUEST,"1");
            }

            /* Compile the data for the templates */

            String classGroupParam = getParamClassgroup(vreq);
            log.debug("ClassGroupParam is \""+ classGroupParam + "\"");
            boolean classGroupFilterRequested = false;
            if (!StringUtils.isEmpty(classGroupParam)) {
                VClassGroup grp = grpDao.getGroupByURI(classGroupParam);
                classGroupFilterRequested = true;
                if (grp != null && grp.getPublicName() != null) {
                    body.put("classGroupURI", grp.getURI());
                    body.put("classGroupName", grp.getPublicName());
                }
            }

            String typeParam = getParamType(vreq);
            boolean typeFilterRequested = false;
            if (!StringUtils.isEmpty(typeParam)) {
                VClass type = vclassDao.getVClassByURI(typeParam);
                typeFilterRequested = true;
                if (type != null && type.getName() != null)
                    body.put("typeName", type.getName());
            }

            /* Add ClassGroup and type refinement links to body */
            if( wasHtmlRequested ){
                if ( !classGroupFilterRequested && !typeFilterRequested ) {
                    // Search request includes no ClassGroup and no type, so add ClassGroup search refinement links.
                    body.put("classGroupLinks", getClassGroupsLinks(vreq, grpDao, docs, mainResponse, queryText));
                } else if ( classGroupFilterRequested && !typeFilterRequested ) {
                    // Search request is for a ClassGroup, so add rdf:type search refinement links
                    // but try to filter out classes that are subclasses
                    body.put("classLinks", getVClassLinks(vreq, vclassDao, docs, mainResponse, queryText));
                    pagingLinkParams.put(PARAM_CLASSGROUP, classGroupParam);
                } else {
                    //search request is for a class so there are no more refinements
                    pagingLinkParams.put(PARAM_RDFTYPE, typeParam);
                }
            }
            
            String sortField = getParamSortField(vreq);
            body.put(PARAM_SORTFIELD, sortField);
            pagingLinkParams.put(PARAM_SORTFIELD, sortField);

            body.put("individuals", IndividualSearchResult
                    .getIndividualTemplateModels(individuals, vreq));

            body.put("querytext", queryText);
            body.put("title", queryText + " - " + appBean.getApplicationName()
                    + " Search Results");

            body.put("hitCount", hitCount);
            body.put("startIndex", startIndex);

            body.put("pagingLinks",
                    getPagingLinks(startIndex, hitsPerPage, hitCount,
                                   vreq.getServletPath(),
                                   pagingLinkParams, vreq));

            if (startIndex != 0) {
                body.put("prevPage", getPreviousPageLink(startIndex,
                        hitsPerPage, vreq.getServletPath(), pagingLinkParams));
            }
            if (startIndex < (hitCount - hitsPerPage)) {
                body.put("nextPage", getNextPageLink(startIndex, hitsPerPage,
                        vreq.getServletPath(), pagingLinkParams));
            }
            
            // make a link back to this same search
            body.put("sortFormHiddenFields", getSortFormParameters(pagingLinkParams));

	        // VIVO OpenSocial Extension by UCSF
	        try {
		        OpenSocialManager openSocialManager = new OpenSocialManager(vreq, "search");
		        // put list of people found onto pubsub channel
	            // only turn this on for a people only search
	            if ("http://vivoweb.org/ontology#vitroClassGrouppeople".equals(getParamClassgroup(vreq))) {
			        List<String> ids = OpenSocialManager.getOpenSocialId(individuals);
			        openSocialManager.setPubsubData(OpenSocialManager.JSON_PERSONID_CHANNEL,
			        		OpenSocialManager.buildJSONPersonIds(ids, "" + ids.size() + " people found"));
	            }
				// TODO put this in a better place to guarantee that it gets called at the proper time!
				openSocialManager.removePubsubGadgetsWithoutData();
		        body.put("openSocial", openSocialManager);
		        if (openSocialManager.isVisible()) {
		        	body.put("bodyOnload", "my.init();");
		        }
	        } catch (IOException e) {
	            log.error("IOException in doTemplate()", e);
	        } catch (SQLException e) {
	            log.error("SQLException in doTemplate()", e);
	        }

	        String template = templateTable.get(format).get(Result.PAGED);

            return new TemplateResponseValues(template, body);
        } catch (Throwable e) {
            return doSearchError(e,format);
        }
    }
    
    private LinkTemplateModel getAllTypesLink(VitroRequest vreq, String querytext) {
        ParamMap params = NoraGetQueryParamMap(vreq);        
        ParamMap paramsMinusType = new ParamMap();
        for(String key : params.keySet()) {
            if(!"facet_content-type_ss".equals(key)) {
                paramsMinusType.put(key, params.get(key));
            }
        }
        paramsMinusType.put(PagedSearchController.PARAM_QUERY_TEXT, querytext);
        return new LinkTemplateModel("All types", vreq.getServletPath(), paramsMinusType);
    }
    
    private List<String> getUnionFacetNames() {
        List<String> unionFacetNames = new ArrayList<String>();
        for(SearchFacet facet : NoraSearchFacets.getAllSearchFacets()) {
            if(facet.isUnionFacet()) {
                unionFacetNames.add(facet.getFieldName());
            }
        }
        return unionFacetNames;
    }
    
    private List<Param> getSortFormParameters(Map <String, String> parameterMap) {
        List<Param> paramList = new ArrayList<Param>();
        for(String key : parameterMap.keySet()) {
            paramList.add(new Param(key, parameterMap.get(key)));
        }
        return paramList;
    }
    
    private List<SearchFacetCategory> getTypeCounts(
            SearchResponse allRecordTypesResponse, VitroRequest vreq, 
            String queryText) {
        List<SearchFacetCategory> typeCounts = 
                new ArrayList<SearchFacetCategory>();
        List<SearchFacet> searchFacets = getFacetLinks(
                NoraSearchFacets.getCommonSearchFacets(), vreq, 
                allRecordTypesResponse, queryText);
        for(SearchFacet f : searchFacets) {
            if("facet_content-type_ss".equals(f.getFieldName())) {
                for(String catName : Arrays.asList("Publications", "Datasets", 
                        "Grants", "Patents", "Clinical trials")) {
                    String catKey = catName.toLowerCase().replaceAll(" ", "_");
                    SearchFacetCategory cat = null;
                    for(SearchFacetCategory catg : f.getCategories()) {
                        if(catKey.equals(catg.getText()) || catName.equals(catg.getText())) {
                            cat = catg;
                            break;
                        }
                    }                                      
                    if(cat != null) {
                        boolean selected = false;
                        String[] contentTypes = vreq.getParameterValues(
                                "facet_content-type_ss");
                        if(contentTypes != null) {
                            for(int i = 0; i < contentTypes.length; i++) {
                                String[] values = contentTypes[i].split(VALUE_DELIMITER);
                                for(int j = 0; j < values.length; j++) {
                                    if(catName.equals(values[j]) || catKey.equals(values[j])) {
                                        selected = true;
                                    }
                                }
                            }
                        }
                        String url = cat.getUrl();
                        // The LinkTemplateModel will happily prepend the context path to something
                        // that already starts with the context path, making it impossible
                        // to clone a SearchFacetCategory unless we do this trickery.
                        String contextPath = UrlBuilder.getUrl("/");
                        if(url.startsWith(contextPath)) {
                            url = url.substring(contextPath.length());
                        }
                        SearchFacetCategory catClone = new SearchFacetCategory(
                                catName, cat.getValue(), url, cat.getCount(), selected);                          
                        typeCounts.add(catClone);
                    } else {
                        typeCounts.add(new SearchFacetCategory(
                                catName, new ParamMap(), 0));
                    }
                }
                break;
            }
        }
        return typeCounts;
    }
    
    private int getTotalEntities(VitroRequest vreq) {
        VClassGroupsForRequest cache = VClassGroupCache.getVClassGroups(vreq);
        Map<String, List<String>> mode2types = new HashMap<String, List<String>>();
        mode2types.put("publications", Arrays.asList(
                "http://purl.org/ontology/bibo/AcademicArticle", 
                "http://purl.org/ontology/bibo/Chapter", 
                "http://vivoweb.org/ontology/core#ConferencePaper"));
        mode2types.put("datasets", Arrays.asList("http://vivoweb.org/ontology/core#Dataset"));
        mode2types.put("grants", Arrays.asList("http://vivoweb.org/ontology/core#Grant"));
        mode2types.put("patents", Arrays.asList("http://purl.org/ontology/bibo/Patent"));
        mode2types.put("clinical_trials", Arrays.asList("http://purl.obolibrary.org/obo/ERO_0000016"));
        int total = 0;
        //      Map<String, Integer> countsForTemplate = new HashMap<String, Integer>();
        for(String key : mode2types.keySet()) {
            for(String typeURI : mode2types.get(key)) {
                VClass vclass = cache.getCachedVClass(typeURI);
                if(vclass != null && vclass.getEntityCount() > 0) {
                    total += vclass.getEntityCount();   
                }
            }
            //        countsForTemplate.put(key, total);
        }
        return total;
    }

    private int getHitsPerPage(VitroRequest vreq) {
        int hitsPerPage = DEFAULT_HITS_PER_PAGE;
        try{
            hitsPerPage = Integer.parseInt(vreq.getParameter(PARAM_HITS_PER_PAGE));
        } catch (Throwable e) {
            hitsPerPage = DEFAULT_HITS_PER_PAGE;
        }
        log.debug("hitsPerPage is " + hitsPerPage);
        return hitsPerPage;
    }

    private int getStartIndex(VitroRequest vreq) {
        int startIndex = 0;
        try{
            startIndex = Integer.parseInt(vreq.getParameter(PARAM_START_INDEX));
        }catch (Throwable e) {
            startIndex = 0;
        }
        log.debug("startIndex is " + startIndex);
        return startIndex;
    }

    private String badQueryText(String qtxt, VitroRequest vreq) {
        if( qtxt == null || "".equals( qtxt.trim() ) )
        	return I18n.text(vreq, "enter_search_term");

        if( qtxt.equals("*:*") )
        	return I18n.text(vreq, "invalid_search_term") ;

        return null;
    }

    /**
     * Get the links to the facet categories for the individuals in the documents
     */
    private static List<SearchFacet> getFacetLinks(List<SearchFacet> facetList, 
            VitroRequest request, SearchResponse response, String querytext) {
        List<SearchFacet> searchFacets = new ArrayList<SearchFacet>();
        for(SearchFacet sf : facetList) {
            SearchFacetField ff = null;
            for (SearchFacetField sff : response.getFacetFields()) {
                if(!sff.getValues().isEmpty()
                        && sff.getName().equals(sf.getFieldName())) {
                    ff = sff;
                    break;
                }
            }
            if(ff == null) {
                continue;
            }
            List<Count> values = ff.getValues();
            if ("facet_year_ss".equals(sf.getFieldName())) {
                // sort the year values in descending chronological order,
                // not by hit count
                List<Count> yearValues = new ArrayList<Count>();
                yearValues.addAll(values);
                Collections.sort(yearValues, new YearComparator());
                values = yearValues;
            }
            for(Count value : values) {
                if(value.getCount() < 1) {
                    continue;
                }
                String name = value.getName();
                String label = humanReadableFacetValue(name, request);
                // need a fresh copy of the params because we're gonna modify it
                ParamMap facetParams = NoraGetQueryParamMap(request);
                facetParams.put(PagedSearchController.PARAM_QUERY_TEXT, querytext);
                String val = facetParams.get(ff.getName());
                if (!("facet_content-type_ss".equals(ff.getName())) 
                        && (val != null) && (!StringUtils.isEmpty(val))) {
                    facetParams.put(ff.getName(), val + VALUE_DELIMITER + name);
                } else {
                    facetParams.put(ff.getName(), name);
                }
                SearchFacetCategory category = new SearchFacetCategory(
                        label, name, facetParams, value.getCount());
                sf.getCategories().add(category);
            }
            if(sf.getParentFacet() == null) {
                searchFacets.add(sf);
                log.debug("Added facet " + sf.getPublicName() + " to template.");
            } else {
                if(sf.isDisplayInSidebar() && !sf.getCategories().isEmpty() 
                        && !searchFacets.contains(sf.getParentFacet())) {
                    searchFacets.add(sf.getParentFacet());
                    log.debug("Added facet " + sf.getPublicName() + " to template.");
                }
            }            
            
        }
        return searchFacets;
    }

    public static class YearComparator implements Comparator<Count> {

        @Override
        public int compare(Count arg0, Count arg1) {
            return String.CASE_INSENSITIVE_ORDER.compare(arg1.getName(), arg0.getName());
        }

    }
    
    /**
     * Get the class groups represented for the individuals in the documents.
     */
    private List<VClassGroupSearchLink> getClassGroupsLinks(VitroRequest vreq, VClassGroupDao grpDao, SearchResultDocumentList docs, SearchResponse rsp, String qtxt) {
        Map<String,Long> cgURItoCount = new HashMap<String,Long>();

        List<VClassGroup> classgroups = new ArrayList<VClassGroup>( );
        List<SearchFacetField> ffs = rsp.getFacetFields();
        for(SearchFacetField ff : ffs){
            if(VitroSearchTermNames.CLASSGROUP_URI.equals(ff.getName())){
                List<Count> counts = ff.getValues();
                for( Count ct: counts){
                    VClassGroup vcg = grpDao.getGroupByURI( ct.getName() );
                    if( vcg == null ){
                        log.debug("could not get classgroup for URI " + ct.getName());
                    }else{
                        classgroups.add(vcg);
                        cgURItoCount.put(vcg.getURI(),  ct.getCount());
                    }
                }
            }
        }

        grpDao.sortGroupList(classgroups);

        VClassGroupsForRequest vcgfr = VClassGroupCache.getVClassGroups(vreq);
        List<VClassGroupSearchLink> classGroupLinks = new ArrayList<VClassGroupSearchLink>(classgroups.size());
        for (VClassGroup vcg : classgroups) {
        	String groupURI = vcg.getURI();
			VClassGroup localizedVcg = vcgfr.getGroup(groupURI);
            long count = cgURItoCount.get( groupURI );
            if (localizedVcg.getPublicName() != null && count > 0 )  {
                classGroupLinks.add(new VClassGroupSearchLink(vreq, qtxt, localizedVcg, count));
            }
        }
        return classGroupLinks;
    }

    private List<VClassSearchLink> getVClassLinks(VitroRequest vreq, VClassDao vclassDao, SearchResultDocumentList docs, SearchResponse rsp, String qtxt){
        HashSet<String> typesInHits = getVClassUrisForHits(docs);
        List<VClass> classes = new ArrayList<VClass>(typesInHits.size());
        Map<String,Long> typeURItoCount = new HashMap<String,Long>();

        List<SearchFacetField> ffs = rsp.getFacetFields();
        for(SearchFacetField ff : ffs){
            if(VitroSearchTermNames.RDFTYPE.equals(ff.getName())){
                List<Count> counts = ff.getValues();
                for( Count ct: counts){
                    String typeUri = ct.getName();
                    long count = ct.getCount();
                    try{
                        if( VitroVocabulary.OWL_THING.equals(typeUri) ||
                            count == 0 )
                            continue;
                        VClass type = vclassDao.getVClassByURI(typeUri);
                        if( type != null &&
                            ! type.isAnonymous() &&
                              type.getName() != null && !"".equals(type.getName()) &&
                              type.getGroupURI() != null ){ //don't display classes that aren't in classgroups
                            typeURItoCount.put(typeUri,count);
                            classes.add(type);
                        }
                    }catch(Exception ex){
                        if( log.isDebugEnabled() )
                            log.debug("could not add type " + typeUri, ex);
                    }
                }
            }
        }


        Collections.sort(classes, new Comparator<VClass>(){
            public int compare(VClass o1, VClass o2) {
                return o1.compareTo(o2);
            }});

        List<VClassSearchLink> vClassLinks = new ArrayList<VClassSearchLink>(classes.size());
        for (VClass vc : classes) {
            long count = typeURItoCount.get(vc.getURI());
            vClassLinks.add(new VClassSearchLink(vreq, qtxt, vc, count ));
        }

        return vClassLinks;
    }

    private HashSet<String> getVClassUrisForHits(SearchResultDocumentList docs){
        HashSet<String> typesInHits = new HashSet<String>();
        for (SearchResultDocument doc : docs) {
            try {
                Collection<Object> types = doc.getFieldValues(VitroSearchTermNames.RDFTYPE);
                if (types != null) {
                    for (Object o : types) {
                        String typeUri = o.toString();
                        typesInHits.add(typeUri);
                    }
                }
            } catch (Exception e) {
                log.error("problems getting rdf:type for search hits",e);
            }
        }
        return typesInHits;
    }

    private String getSnippet(SearchResultDocument doc, SearchResponse response) {
        String docId = doc.getStringValue(VitroSearchTermNames.DOCID);
        StringBuffer text = new StringBuffer();
        Map<String, Map<String, List<String>>> highlights = response.getHighlighting();
        if (highlights != null && highlights.get(docId) != null) {
            List<String> snippets = highlights.get(docId).get(VitroSearchTermNames.ALLTEXT);
            if (snippets != null && snippets.size() > 0) {
                text.append("... " + snippets.get(0) + " ...");
            }
        }
        return text.toString();
    }

    private SearchQuery getQuery(String queryText, List<String> excludeFacets, 
            List<String> unionFacets,  int hitsPerPage, int startIndex, 
            VitroRequest vreq) {
        if(excludeFacets == null) {
            excludeFacets = new ArrayList<String>();
        }
        if(unionFacets == null) {
            unionFacets = new ArrayList<String>();
        }
        // Nora: AND in search terms for specific "facet as text" field
        String facetAsText = vreq.getParameter(PARAM_FACET_AS_TEXT);
        if(facetAsText != null) {
            SearchFacet textFacet = NoraSearchFacets.getSearchFacetByFieldName(facetAsText);
            if(textFacet != null && textFacet.isFacetAsText()) {
                String textValue = vreq.getParameter(PARAM_FACET_TEXT_VALUE);
                if(textValue != null) {
                    if (!StringUtils.isEmpty(queryText)) {
                        queryText += " AND ";
                    }
                    if (facetAsText.contains("publication-year")) {
                        if (textValue.contains("-")) {
                            queryText += "(";
                            String years[] = textValue.split("-");
                            int yearFrom = Integer.valueOf(years[0]);
                            int yearTo = Integer.valueOf(years[1]) + 1;
                            for (int year = yearFrom; year < yearTo; year++) {
                                if (year > yearFrom) {
                                    queryText += " OR ";
                                }
                                queryText += textFacet.getFieldName() + ":" + year;
                            }
                            queryText += ")";
                        } else {
                            queryText += textFacet.getFieldName() + ":" + textValue;
                        }
                    } else {
                        queryText += textFacet.getFieldName() + ":\""
                                + textValue.replaceAll(Pattern.quote("\""), "") + "\"";
                    }
                }
            }
        }

        log.debug("query text is " + queryText);

        // Lowercase the search term to support wildcard searches: The search engine applies no text
        // processing to a wildcard search term.
        SearchQuery query = ApplicationUtils.instance().getSearchEngine().createQuery(queryText);

        query.setStart( startIndex )
             .setRows(hitsPerPage);
        
        String sortField = getParamSortField(vreq);
        if(sortField != null) {
            String[] sortAndDirection = sortField.split("\\|");
            // this string replacement may no longer be necessary
            sortField = sortAndDirection[0].replace("facet_", "sort_").replace("_ss", "_s");
            log.debug("adding sort field " + sortField);
            if("DESC".equals(sortAndDirection[1])) {
                query.addSortField(sortField, Order.DESC);    
            } else {
                // default to ascending
                query.addSortField(sortField, Order.ASC);
            }            
        }

        // ClassGroup filtering param
        String classgroupParam = getParamClassgroup(vreq);
        
        query.addFilterQuery("!" + VitroSearchTermNames.RDFTYPE 
                + ":\"http://nora.deffopera.dk/ontology/display/HideFromSearch\"");
        
        // rdf:type filtering param
        String typeParam = getParamType(vreq);

        if (  ! StringUtils.isBlank(typeParam) ) {
            // rdf:type filtering
            log.debug("Firing type query ");
            log.debug("request.getParameter(type) is "+ typeParam);
            // NORA special treatment of owl:Thing: OR together only the types
            // of interest in the interface (bibo:Document, vivo:Grant and ClinicalTrial)
            if(OWL.Thing.getURI().equals(typeParam)) {
                query.addFilterQuery(VitroSearchTermNames.RDFTYPE + ":\"http://purl.obolibrary.org/obo/ERO_0000016\" OR " 
                        + VitroSearchTermNames.RDFTYPE + ":\"http://vivoweb.org/ontology/core#Grant\" OR "
                        + VitroSearchTermNames.RDFTYPE + ":\"http://purl.org/ontology/bibo/Document\"");
            } else {
                query.addFilterQuery(VitroSearchTermNames.RDFTYPE + ":\"" + typeParam + "\"");
            }
            //with type filtering we don't have facets.
        } else if ( ! StringUtils.isBlank(classgroupParam) ) {
            // ClassGroup filtering
            log.debug("Firing classgroup query ");
            log.debug("request.getParameter(classgroup) is "+ classgroupParam);
            query.addFilterQuery(VitroSearchTermNames.CLASSGROUP_URI + ":\"" + classgroupParam + "\"");
            //with ClassGroup filtering we want type facets
            query.addFacetFields(VitroSearchTermNames.RDFTYPE).setFacetLimit(-1);
        } else {
            //When no filtering is set, we want ClassGroup facets
            query.addFacetFields(VitroSearchTermNames.CLASSGROUP_URI).setFacetLimit(-1);
        }
        String jsonFacet = getJsonFacet(vreq);
        if(jsonFacet != null) {
            query.addFacetFields(jsonFacet).setFacetLimit(getFacetLimit(vreq) + getFacetOffset(vreq)).setFacetMinCount(1);
        } else {
            for(SearchFacet facet : NoraSearchFacets.getCommonSearchFacets()) {
                query.addFacetFields(facet.getFieldName()).setFacetLimit(FACET_LIMIT).setFacetMinCount(1);
            }
            if(!"all".equals(getParamSearchMode(vreq))) {
                for(SearchFacet facet : NoraSearchFacets.getAdditionalSearchFacets()) {
                    query.addFacetFields(facet.getFieldName()).setFacetLimit(FACET_LIMIT).setFacetMinCount(1);
                }
            }
        }
        addNoraFacetFields(query, excludeFacets, unionFacets, vreq);
        log.debug("Query = " + query.toString());
        return query;
    }
    
    protected static String getParamSortField(VitroRequest vreq) {
        String value = vreq.getParameter(PARAM_SORTFIELD);
        if(value != null) {
            return value;
        } else {
            return "sort_year_s|DESC";
        }
    }
    
    protected static String getParamClassgroup(VitroRequest vreq) {
        String classgroupParam = null;
        String searchMode = vreq.getParameter(PARAM_SEARCHMODE);
        if("publications".equals(searchMode)) {
            classgroupParam = "http://vivoweb.org/ontology#vitroClassGrouppublications";
        } else {
            classgroupParam = vreq.getParameter(PARAM_CLASSGROUP);
        }
        return classgroupParam;
    }
    
    protected static String getParamType(VitroRequest vreq) {
         String typeParam = null;
         String searchMode = vreq.getParameter(PARAM_SEARCHMODE);
         if("all".equals(searchMode)) {
             typeParam = OWL.Thing.getURI();
         } else if("datasets".equals(searchMode)) {
             typeParam = "http://vivoweb.org/ontology/core#Dataset";
         } else if ("grants".equals(searchMode)) {
             typeParam = "http://vivoweb.org/ontology/core#Grant";
         } else if ("patents".equals(searchMode)) {
             typeParam = "http://purl.org/ontology/bibo/Patent";
         } else if ("clinical_trials".equals(searchMode)) {
             typeParam = "http://purl.obolibrary.org/obo/ERO_0000016";
         } else {
             typeParam = vreq.getParameter(PARAM_RDFTYPE);
         } 
         return typeParam;
    }
    
    private static String getParamRecordType(VitroRequest vreq) {
        return vreq.getParameter(PARAM_RECORD_TYPE);
    }
    
    protected static String getParamSearchMode(VitroRequest vreq) {
        String searchModeParam = vreq.getParameter(PARAM_SEARCHMODE);
        if(searchModeParam != null) {
            return searchModeParam;
        } else if("http://vivoweb.org/ontology#vitroClassGrouppublications".equals(
                getParamClassgroup(vreq)) || "publications".equals(getParamRecordType(vreq))) {
            return "publications";
        } else if("http://vivoweb.org/ontology/core#Dataset".equals(
                getParamType(vreq)) || "datasets".equals(getParamRecordType(vreq))) {
            return "datasets";
        } else if("http://vivoweb.org/ontology/core#Grant".equals(getParamType(
                vreq)) || "grants".equals(getParamRecordType(vreq))) {
            return "grants";
        } else if("http://purl.org/ontology/bibo/Patent".equals(getParamType(
                vreq)) || "patents".equals(getParamRecordType(vreq))) {
            return "patents";
        } else if("http://purl.obolibrary.org/obo/ERO_0000016".equals(
                getParamType(vreq)) || "clinical_trials".equals(getParamRecordType(vreq))) {
            return "clinical_trials";
        } else if(OWL.Thing.getURI().equals(getParamType(vreq)) 
                || (getParamRecordType(vreq) == null)) {
            //(getParamClassgroup(vreq) == null && getParamType(vreq) == null){
            return "all";
        } else {
            return null;
        }
    }

    protected static void addNoraFacetFields(SearchQuery query, 
            List<String> excludeFacets, List<String> unionFacets, VitroRequest vreq) {       
        
        ParamMap facetParams = getFacetParamMap(vreq);
                
        // regular facet behavior
        for(String parameterName : facetParams.keySet()) {
            if(excludeFacets.contains(parameterName)) {
                // skip the excluded facet
                continue;
            }
            String parameterValue = facetParams.get(parameterName);
            if(parameterValue.endsWith(GROUP_DELIMITER)) {
                parameterValue = parameterValue.substring(
                        0, parameterValue.length() - GROUP_DELIMITER.length());
            }
            if(!parameterValue.isEmpty() && !unionFacets.contains(parameterName)) {
                if (parameterValue.contains(VALUE_DELIMITER)) {
                    StringBuilder builder = new StringBuilder();
                    for (String val : parameterValue.split(VALUE_DELIMITER)) {
                        if(builder.length() > 0) {
                            builder.append(" AND ");
                        }
                        builder.append(parameterName + ":\"" + val + "\"");
                    }
                    log.info("Query path1: " + builder.toString());
                    query.addFilterQuery(builder.toString());
                } else {
                    log.info("Query path2: " + parameterName + ":\"" + parameterValue + "\"");
                    query.addFilterQuery(parameterName + ":\"" + parameterValue + "\"");
                }
            }
        }
        
        // TODO refactor this and the above!
        // union facet behavior
        StringBuilder builder = new StringBuilder();
        for(String parameterName : facetParams.keySet()) {
            String parameterValue = facetParams.get(parameterName);
            if(excludeFacets.contains(parameterName)) {
                // skip the excluded facet
                continue;
            }
            if(parameterValue.endsWith(GROUP_DELIMITER)) {
                parameterValue = parameterValue.substring(
                        0, parameterValue.length() - GROUP_DELIMITER.length());
            }
            if(!parameterValue.isEmpty() && unionFacets.contains(parameterName)) {
                for(String group: parameterValue.split(Pattern.quote(GROUP_DELIMITER))) {
                    if(group.trim().isEmpty()) {
                        continue;
                    }
                    if(builder.length() > 0) {
                        builder.append(" AND ");
                    }
                    builder.append("( ");
                    StringBuilder valBuilder = new StringBuilder();
                    for (String val : group.split(VALUE_DELIMITER)) {
                        if(valBuilder.length() > 0) {
                            valBuilder.append(" OR ");
                        }
                        valBuilder.append(parameterName + ":\"" + val + "\"");
                        // 2020-11-17 temporary fix to avoid reindexing TODO: remove
                        if("facet_organization-all_ss".equals(parameterName) && !val.startsWith("http")) {
                            valBuilder.append(" OR ").append("facet_hospital_ss:" + val + "\"");
                        }
                    }
                    builder.append(valBuilder).append(")");
                }                             
            }
        }
        if(builder.length() > 0) {
            log.info("Query path 3: " + builder.toString());
            query.addFilterQuery(builder.toString());    
        }
        
	    query.setFacetMinCount(1);
    }

    private static ParamMap getFacetParamMap(VitroRequest vreq) {
        ParamMap map = new ParamMap();
        Enumeration<String> parameterNames = vreq.getParameterNames();
        while(parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            if(parameterName.startsWith(FACET_FIELD_PREFIX)) {
                int previousValueIndex = -1;
                StringBuilder builder = new StringBuilder(); 
                String[] parameterValues = vreq.getParameterValues(parameterName);              
                for(int i = 0; i < parameterValues.length; i++) {
                    String parameterValue = parameterValues[i];
                    if(parameterValue.endsWith(GROUP_DELIMITER)) {
                        previousValueIndex = i;
                    }
                }
                if(previousValueIndex >= 0) {
                    builder.append(parameterValues[previousValueIndex]);
                }
                StringBuilder newValues = new StringBuilder();
                for(int i = 0; i < parameterValues.length; i++) {
                    if(i != previousValueIndex) {
                        if(newValues.length() > 0) {
                            newValues.append(VALUE_DELIMITER);    
                        }
                        newValues.append(parameterValues[i]);
                    }
                }
                builder.append(newValues);
                SearchFacet sf = NoraSearchFacets.getSearchFacetByFieldName(
                        parameterName);
                if(sf.isUnionFacet()) {
                    builder.append(GROUP_DELIMITER);
                }
                log.debug(parameterName + " = " + builder.toString());
                map.put(parameterName, builder.toString());
            }
        }
        return map;
    }

    private static ParamMap NoraGetQueryParamMap(VitroRequest vreq) {
        ParamMap map = getFacetParamMap(vreq);
        String s = vreq.getParameter(PARAM_FACET_TEXT_VALUE);
        if(!StringUtils.isEmpty(s)) {
            map.put(PARAM_FACET_TEXT_VALUE, s);
            map.put(PARAM_FACET_AS_TEXT, vreq.getParameter(PARAM_FACET_AS_TEXT));
        }
        s = getParamSortField(vreq);
        if(!StringUtils.isEmpty(s)) {
            map.put(PARAM_SORTFIELD, s);
        }
        s = vreq.getParameter(PARAM_QUERY_TEXT);
        if(!StringUtils.isEmpty(s)) {
            map.put(PARAM_QUERY_TEXT, s);
        }
        s = getParamType(vreq);
        if(!StringUtils.isEmpty(s)) {
            map.put(PARAM_RDFTYPE, s);
        } else {
            s = getParamClassgroup(vreq);
            if(!StringUtils.isEmpty(s)) {
                map.put(PARAM_CLASSGROUP, s);
            }
        }
        return map;
    }

    private static List<LinkTemplateModel> getNoraQueryReduceLinks(
            VitroRequest vreq, VClassGroupDao grpDao, VClassDao vclassDao) {
        ParamMap map = NoraGetQueryParamMap(vreq);
        List<LinkTemplateModel> qr = new ArrayList<LinkTemplateModel>();

        String s = map.get(PARAM_QUERY_TEXT);
        if ((s != null) && (!StringUtils.isEmpty(s))) {
            map.remove(PARAM_QUERY_TEXT);
            qr.add(new LinkTemplateModel(s, "/search", map));
            map.put(PARAM_QUERY_TEXT, s);
        }
        s = map.get(PARAM_FACET_TEXT_VALUE);
        if ((s != null) && (!StringUtils.isEmpty(s))) {
            String label = map.get(PARAM_FACET_AS_TEXT);
            SearchFacet textFacet = NoraSearchFacets.getSearchFacetByFieldName(
                    map.get(PARAM_FACET_AS_TEXT));
            if(textFacet != null && textFacet.isFacetAsText()) {
                label = textFacet.getPublicName();
            }
            map.remove(PARAM_FACET_TEXT_VALUE);
            qr.add(new LinkTemplateModel(label + ": " + s, "/search", map));
            map.put(PARAM_FACET_TEXT_VALUE, s);
        }
/*      Don't display or give the option to remove the class group for now, it adds to confusion when just viewing publication
        s = map.get(PARAM_CLASSGROUP);
        if ((s != null) && (!StringUtils.isEmpty(s))) {
            VClassGroup vcg = grpDao.getGroupByURI(s);
            String label = "";
            if( vcg == null ){
                label = s;
            } else {
                label = vcg.getPublicName();
            }
            map.remove(PARAM_CLASSGROUP);
            qr.add(new LinkTemplateModel("Type: " + label, "/search", map));
            map.put(PARAM_CLASSGROUP, s);
        }
        s = map.get(PARAM_RDFTYPE);
        if ((s != null) && (!StringUtils.isEmpty(s))) {
            VClass type = vclassDao.getVClassByURI(s);
            String label = "";
            if( type == null ){
                label = s;
            } else {
                label = type.getName();
            }
            map.remove(PARAM_RDFTYPE);
            qr.add(new LinkTemplateModel("Sub-Type: " + label, "/search", map));
            map.put(PARAM_RDFTYPE, s);
        }
*/
        for(String key : NoraSearchFacets.getFacetFields()) {
            log.debug("Building query reduce link for " + key);
            s = map.get(key);
            if ((s != null) && (!StringUtils.isEmpty(s))) {
                String label = key;
                SearchFacet textFacet = NoraSearchFacets.getSearchFacetByFieldName(key);
                if(textFacet != null) {
                   label = textFacet.getPublicName();
                }
                if (!textFacet.isUnionFacet() && s.contains(VALUE_DELIMITER)) {
                    List<String> vals = new ArrayList<String>(Arrays.asList(
                            s.split(Pattern.quote(VALUE_DELIMITER))));
                    for (int i = 0; i < vals.size(); i++) {
                        String val = vals.get(i);
                        String valueLabel = humanReadableFacetValue(val, vreq);
                        map.put(key, StringUtils.join(vals, VALUE_DELIMITER));
                        vals.add(i, val);
                        qr.add(new LinkTemplateModel(label + ": " + valueLabel, "/search", map));
                    }
                    map.put(key, s);
                } else if (textFacet.isUnionFacet() 
                        && (s.contains(VALUE_DELIMITER) || s.contains(GROUP_DELIMITER))) {
                    List<String> groups = new ArrayList<String>(Arrays.asList(
                            s.split(Pattern.quote(GROUP_DELIMITER))));
                    for (int i = 0; i < groups.size(); i++) {
                        String group = groups.get(i);        
                        if(group.trim().isEmpty()) {
                            continue;
                        }
                        groups.remove(i);
                        map.put(key, StringUtils.join(groups, GROUP_DELIMITER));
                        StringBuilder valueLabels = new StringBuilder();
                        List<String> vals = new ArrayList<String>(Arrays.asList(
                                group.split(Pattern.quote(VALUE_DELIMITER))));
                        for (String val : vals) {
                            if(valueLabels.length() > 0) {
                                valueLabels.append(" OR ");
                            }
                            valueLabels.append(humanReadableFacetValue(val, vreq));                         
                        }
                        qr.add(new LinkTemplateModel(label + ": " 
                                + valueLabels.toString(), "/search", map));                   
                        groups.add(i, group);
                    }
                } else {
                    String val = humanReadableFacetValue(s, vreq);                    
                    map.remove(key);
                    qr.add(new LinkTemplateModel(label + ": " + val, "/search", map));
                    map.put(key, s);
                }
            }
        }
        return qr;
    }
    
    /**
     * If the facet value is an RDF IRI, look up and return the rdfs:label, if
     * available.  Otherwise return the raw facet value.
     * @param facetValue
     * @param vreq
     * @return the facet value's rdfs:label (if IRI and label available), 
     * otherwise original value
     */
    private static String humanReadableFacetValue(String facetValue, 
            VitroRequest vreq) {
        if(facetValue.startsWith("http://")) {
            //IndividualDao iDao = vreq.getWebappDaoFactory()
            //        .getIndividualDao();
            //Individual ind = iDao.getIndividualByURI(facetValue);
            ParameterizedSparqlString pss = new ParameterizedSparqlString(
                    LABEL_ABBR_QUERY);
            pss.setIri("x", facetValue);
            try {
                LabelAbbrConsumer consumer = new LabelAbbrConsumer();
                vreq.getRDFService().sparqlSelectQuery(
                        pss.toString(), consumer);
                if(consumer.getAbbreviation() != null) {
                    return consumer.getAbbreviation();
                } else if(consumer.getLabel() != null) {
                    return consumer.getLabel();
                } else {
                    return facetValue;
                }
            } catch (RDFServiceException e) {
                log.error(e, e);
                return facetValue;
            }            
            //if(ind != null) {            
            //    facetValue = ind.getRdfsLabel();
            //}
        }
        return facetValue;
    }
    
    private static class LabelAbbrConsumer extends ResultSetConsumer {

        private String label;
        private String abbreviation;
        
        public String getLabel() {
            return label;
        }
        
        public String getAbbreviation() {
            return abbreviation;
        }
        
        @Override
        protected void processQuerySolution(QuerySolution qsoln) {
            if(qsoln.contains("label") && qsoln.get("label").isLiteral()) {
                label = qsoln.getLiteral("label").getLexicalForm();
            }
            if(qsoln.contains("abbr") && qsoln.get("abbr").isLiteral()) {
                abbreviation = qsoln.getLiteral("abbr").getLexicalForm();
            }
        }
        
    }

    public static class VClassGroupSearchLink extends LinkTemplateModel {
        long count = 0;
        VClassGroupSearchLink(VitroRequest vreq, String querytext, VClassGroup classgroup, long count) {
            super(classgroup.getPublicName(), "/search", VClassGroupSearchLinkMap(vreq, querytext, classgroup));
            this.count = count;
        }
        public String getCount() { return Long.toString(count); }
        private static ParamMap VClassGroupSearchLinkMap(VitroRequest vreq, String querytext, VClassGroup classgroup) {
            ParamMap map = NoraGetQueryParamMap(vreq);
            map.put(PARAM_QUERY_TEXT, querytext);
            map.put(PARAM_CLASSGROUP, classgroup.getURI());
            return map;
        }
    }

    public static class VClassSearchLink extends LinkTemplateModel {
        long count = 0;
        VClassSearchLink(VitroRequest vreq, String querytext, VClass type, long count) {
            super(type.getName(), "/search", VClassSearchLinkMap(vreq, querytext, type));
            this.count = count;
        }
        public String getCount() { return Long.toString(count); }
        private static ParamMap VClassSearchLinkMap(VitroRequest vreq, String querytext, VClass type) {
            ParamMap map = NoraGetQueryParamMap(vreq);
            map.put(PARAM_QUERY_TEXT, querytext);
            map.put(PARAM_RDFTYPE, type.getURI());
            return map;
        }
    }

    protected static List<PagingLink> getPagingLinks(int startIndex, int hitsPerPage, long hitCount, String baseUrl, ParamMap params, VitroRequest vreq) {

        List<PagingLink> pagingLinks = new ArrayList<PagingLink>();

        // No paging links if only one page of results
        if (hitCount <= hitsPerPage) {
            return pagingLinks;
        }

        int maxHitCount = DEFAULT_MAX_HIT_COUNT ;
        if( startIndex >= DEFAULT_MAX_HIT_COUNT  - hitsPerPage )
            maxHitCount = startIndex + DEFAULT_MAX_HIT_COUNT;

        for (int i = 0; i < hitCount; i += hitsPerPage) {
            params.put(PARAM_START_INDEX, String.valueOf(i));
            if ( i < maxHitCount - hitsPerPage) {
                int pageNumber = i/hitsPerPage + 1;
                boolean iIsCurrentPage = (i >= startIndex && i < (startIndex + hitsPerPage));
                if ( iIsCurrentPage ) {
                    pagingLinks.add(new PagingLink(pageNumber));
                } else {
                    pagingLinks.add(new PagingLink(pageNumber, baseUrl, params));
                }
            } else {
            	pagingLinks.add(new PagingLink(I18n.text(vreq, "paging_link_more"), baseUrl, params));
                break;
            }
        }

        return pagingLinks;
    }

    private String getPreviousPageLink(int startIndex, int hitsPerPage, String baseUrl, ParamMap params) {
        params.put(PARAM_START_INDEX, String.valueOf(startIndex-hitsPerPage));
        return UrlBuilder.getUrl(baseUrl, params);
    }

    private String getNextPageLink(int startIndex, int hitsPerPage, String baseUrl, ParamMap params) {
        params.put(PARAM_START_INDEX, String.valueOf(startIndex+hitsPerPage));
        return UrlBuilder.getUrl(baseUrl, params);
    }

    protected static class PagingLink extends LinkTemplateModel {

        PagingLink(int pageNumber, String baseUrl, ParamMap params) {
            super(String.valueOf(pageNumber), baseUrl, params);
        }

        // Constructor for current page item: not a link, so no url value.
        PagingLink(int pageNumber) {
            setText(String.valueOf(pageNumber));
        }

        // Constructor for "more..." item
        PagingLink(String text, String baseUrl, ParamMap params) {
            super(text, baseUrl, params);
        }
    }

    private ExceptionResponseValues doSearchError(Throwable e, Format f) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "Search failed: " + e.getMessage());
        return new ExceptionResponseValues(getTemplate(f,Result.ERROR), body, e);
    }

    private TemplateResponseValues doFailedSearch(String message, String querytext, Format f, VitroRequest vreq) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("title", I18n.text(vreq, "search_for", querytext));
        if ( StringUtils.isEmpty(message) ) {
        	message = I18n.text(vreq, "search_failed");
        }
        body.put("message", message);
        return new TemplateResponseValues(getTemplate(f,Result.ERROR), body);
    }

    private TemplateResponseValues doNoHits(String querytext, Format f, VitroRequest vreq) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("title", I18n.text(vreq, "search_for", querytext));
        body.put("message", I18n.text(vreq, "no_matching_results"));
        body.put("querytext", querytext);
        body.put("facetsAsText", NoraSearchFacets.getSearchFacetsAsText());
        body.put(PARAM_FACET_AS_TEXT, vreq.getParameter(PARAM_FACET_AS_TEXT));
        body.put(PARAM_FACET_TEXT_VALUE, vreq.getParameter(PARAM_FACET_TEXT_VALUE));
        return new TemplateResponseValues(getTemplate(f,Result.ERROR), body);
    }

    /**
     * Makes a message to display to user for a bad search term.
     */
    private String makeBadSearchMessage(String querytext, String exceptionMsg, VitroRequest vreq){
        String rv = "";
        try{
            //try to get the column in the search term that is causing the problems
            int coli = exceptionMsg.indexOf("column");
            if( coli == -1) return "";
            int numi = exceptionMsg.indexOf(".", coli+7);
            if( numi == -1 ) return "";
            String part = exceptionMsg.substring(coli+7,numi );
            int i = Integer.parseInt(part) - 1;

            // figure out where to cut preview and post-view
            int errorWindow = 5;
            int pre = i - errorWindow;
            if (pre < 0)
                pre = 0;
            int post = i + errorWindow;
            if (post > querytext.length())
                post = querytext.length();
            // log.warn("pre: " + pre + " post: " + post + " term len:
            // " + term.length());

            // get part of the search term before the error and after
            String before = querytext.substring(pre, i);
            String after = "";
            if (post > i)
                after = querytext.substring(i + 1, post);

            rv = I18n.text(vreq, "search_term_error_near") +
            		" <span class='searchQuote'>"
                + before + "<span class='searchError'>" + querytext.charAt(i)
                + "</span>" + after + "</span>";
        } catch (Throwable ex) {
            return "";
        }
        return rv;
    }

    public static final int MAX_QUERY_LENGTH = 500;

    protected boolean isRequestedFormat(String paramName, VitroRequest req){
        if( req != null ){
            String param = req.getParameter(paramName);
            if( param != null && "1".equals(param)){
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    protected Format getFormat(VitroRequest req){
        if( req != null && req.getParameter("xml") != null && "1".equals(req.getParameter("xml")))
            return Format.XML;
        else if ( req != null && req.getParameter("csv") != null && "1".equals(req.getParameter("csv")))
        	return Format.CSV;
        else
            return Format.HTML;
    }

    protected static String getTemplate(Format format, Result result){
        if( format != null && result != null)
            return templateTable.get(format).get(result);
        else{
            log.error("getTemplate() must not have a null format or result.");
            return templateTable.get(Format.HTML).get(Result.ERROR);
        }
    }

    protected static Map<Format,Map<Result,String>> setupTemplateTable(){
        Map<Format,Map<Result,String>> table = new HashMap<>();

        HashMap<Result,String> resultsToTemplates = new HashMap<Result,String>();

        // set up HTML format
        resultsToTemplates.put(Result.PAGED, "search-pagedResults.ftl");
        resultsToTemplates.put(Result.ERROR, "search-error.ftl");
        // resultsToTemplates.put(Result.BAD_QUERY, "search-badQuery.ftl");
        table.put(Format.HTML, Collections.unmodifiableMap(resultsToTemplates));

        // set up XML format
        resultsToTemplates = new HashMap<Result,String>();
        resultsToTemplates.put(Result.PAGED, "search-xmlResults.ftl");
        resultsToTemplates.put(Result.ERROR, "search-xmlError.ftl");

        // resultsToTemplates.put(Result.BAD_QUERY, "search-xmlBadQuery.ftl");
        table.put(Format.XML, Collections.unmodifiableMap(resultsToTemplates));

        // set up CSV format
        resultsToTemplates = new HashMap<Result,String>();
        resultsToTemplates.put(Result.PAGED, "search-csvResults.ftl");
        resultsToTemplates.put(Result.ERROR, "search-csvError.ftl");

        // resultsToTemplates.put(Result.BAD_QUERY, "search-xmlBadQuery.ftl");
        table.put(Format.CSV, Collections.unmodifiableMap(resultsToTemplates));


        return Collections.unmodifiableMap(table);
    }
    
}
