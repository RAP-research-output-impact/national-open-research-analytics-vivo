<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Template for displaying paged search results -->

<script src="${urls.theme}/js/jquery.corner.js"></script>

<div class="searchResultTypes">

  <a href="${urls.base}/search?classgroup=http%3A%2F%2Fvivoweb.org%2Fontology%23vitroClassGrouppublications">Publications (${typeCounts.publications!0})</a>

  <a href="${urls.base}/search?type=http%3A%2F%2Fvivoweb.org%2Fontology%2Fcore%23Dataset&facet_document-type_ss=http%3A%2F%2Fvivoweb.org%2Fontology%2Fcore%23Dataset">Datasets (${typeCounts.datasets!0})</a>

  <a href="${urls.base}/search?type=http%3A%2F%2Fvivoweb.org%2Fontology%2Fcore%23Grant">Grants (${typeCounts.grants!0})</a>

  <a href="${urls.base}/search?type=http%3A%2F%2Fpurl.org%2Fontology%2Fbibo%2FPatent">Patents (${typeCounts.patents!0})</a>

  <a href="${urls.base}/search?type=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FERO_0000016">Clinical Trials (${typeCounts.clinical_trials!0})</a>

</div> <!-- searchResultTypes -->

<div class="contentsBrowseGroup">
    <#-- Refinement links -->
    <div class="searchTOC-box">
        <table border="1" class="searchTOC">
            <tr>
                <td>
                    <h4 class="hitCount">Results: ${hitCount}</h4>
                    <hr width="80%"/>
                    <div class="searchTOC-header">Use Facets to Refine Results</div>
                </td>
            </tr>
            <#if commonFacets?has_content>
	        <tr class="search-facets-head">
                  <td style="align:left">GENERAL FACETS:</td>
		</tr>
                <#list commonFacets as facet>
		  <#if facet.displayInSidebar && facet.categories?has_content && (facet.categories?size > 1)>
                    <tr class="search-facets-head">
                        <td style="align: left">
                            <h4 class="search-facets-title"><div class="search-facets-toggle">+</div>${facet.publicName}</h4>
                        </td>
                    </tr>
                    <tr class="search-facets" style="display: none;">
                        <td>
                            <ul>
                                <#list facet.categories as category>
                                    <#if category.text?has_content && category.text != 'Journal' && category.text != 'Book' && category.text != 'Conference'>
                                        <li><a href="${category.url}" title="${category.text}">${category.text}</a><span>(${category.count})</span></li>
                                    </#if>
                                </#list>
                            </ul>
                        </td>
                    </tr>
		  </#if>
                </#list>
            </#if>
	    <#-- TODO: set up as Macro -->
            <#if additionalFacets?has_content>
	        <#assign recordTypeLabel = "ADDITIONAL"> <#-- should not actually be used -->
		<#if searchMode?? && searchMode=="publications">
		  <#assign recordTypeLabel = "PUBLICATION"> 
                <#elseif searchMode?? && searchMode="datasets">
                  <#assign recordTypeLabel = "ĐATASET">
                <#elseif searchMode?? && searchMode="grants">
                  <#assign recordTypeLabel = "GRANT">
                <#elseif searchMode?? && searchMode="patents">
                  <#assign recordTypeLabel = "PATENT">
                <#elseif searchMode?? && searchMode="clinical_trials">
                  <#assign recordTypeLabel = "CLINCAL TRIAL">
	        </#if>	
	        <tr class="search-facets-head">
                  <td style="align:left">${recordTypeLabel} FACETS:</td>
		</tr>
                <#list additionalFacets as facet>
		  <#if facet.displayInSidebar && facet.categories?has_content && (facet.categories?size > 1)>
                    <tr class="search-facets-head">
                        <td style="align: left">
                            <h4 class="search-facets-title"><div class="search-facets-toggle">+</div>${facet.publicName}</h4>
                        </td>
                    </tr>
                    <tr class="search-facets" style="display: none;">
                        <td>
                            <ul>
                                <#list facet.categories as category>
                                    <#if category.text?has_content && category.text != 'Journal' && category.text != 'Book' && category.text != 'Conference'>
                                        <li><a href="${category.url}" title="${category.text}">${category.text}</a><span>(${category.count})</span></li>
                                    </#if>
                                </#list>
                            </ul>
                        </td>
                    </tr>
		  </#if>
                </#list>
            </#if>
        </table>
    </div>
    <div id="nora-search-form">
        <form action="" method="GET">
	    <select name="searchMode">
              <option value="all" <#if searchMode?? && searchMode = "all">selected</#if>>all data</option>
              <option value="publications" <#if searchMode?? && searchMode = "publications">selected</#if>>publications</option>
              <option value="datasets" <#if searchMode?? && searchMode = "datasets">selected</#if>>datasets</option>
              <option value="grants" <#if searchMode?? && searchMode = "grants">selected</#if>>grants</option>
              <option value="patents" <#if searchMode?? && searchMode = "patents">selected</#if>>patents</option>
              <option value="clinical_trials" <#if searchMode?? && searchMode = "clinical_trials">selected</#if>>clinical trials</option>
	    </select>
            <input type="text" id="nora-search-text" name="querytext" value="${querytext}" />
	  <#--
            <strong>AND</strong>
            <select name="facetAsText">
                <#if facetAsText?has_content>
                    <#list facetsAsText as fat>
                        <option value="${fat.fieldName}" <#if fat.fieldName == facetAsText>selected</#if>>${fat.publicName}</option>
                    </#list>
                <#else>
                    <#list facetsAsText as fat>
                        <option value="${fat.fieldName}">${fat.publicName}</option>
                    </#list>
                </#if>
            </select>
            <#if facetTextValue?has_content>
                <input type="text" name="facetTextValue" value="${facetTextValue}"/>
            <#else>
                <input type="text" name="facetTextValue"/>
            </#if>
	 -->
            <#if classGroupURI?has_content>
                <input type="hidden" name="classgroup" value="${classGroupURI}" />
            </#if>
            <input id="nora-search-submit" type="submit" value="Search"/>
        </form>
        <!--
        <span id="searchHelp" style="text-align: right;"><a href="${urls.base}/searchHelp" title="${i18n().search_help}">${i18n().not_expected_results}</a></span>
        -->
    </div>
    <div style="width: 57.5%; float: right;">
        <#list noraQueryReduce as link>
            <div class="qr-box">
                <span class="qr-text">${link.text}</span>
                <span class="qr-link" onClick="window.location.href='${link.url}';"><a href="${link.url}">X</a></span>
            </div>
        </#list>
    </div>
    <#if sortFormHiddenFields?? && commonFacets?has_content>
        <div style="width: 57.5%; float: right; text-align: right;">
          Sort by <form style="display: inline;" action="${urls.base}/search" method="GET">
            <select name="sortField" onchange="this.form.submit()">
              <option <#if sortField?? && sortField = "sort_year_s|ASC">selected="selected"</#if> value="sort_year_s|ASC">year (ascending)</option>
              <option <#if sortField?? && sortField = "sort_year_s|DESC">selected="selected"</#if> value="sort_year_s|DESC">year (descending)</option>
              <option <#if sortField?? && sortField = "nameLowercaseSingleValued|ASC">selected="selected"</#if> value="nameLowercaseSingleValued|ASC">title (ascending)</option>
              <option <#if sortField?? && sortField = "nameLowercaseSingleValued|DESC">selected="selected"</#if> value="nameLowercaseSingleValued|DESC">title (descending)</option>
	    </select>
	    <#list sortFormHiddenFields as field>
	      <#if field.name?? && (field.name != "sortField") && (field.name != "startIndex") && field.value??>
                <input type="hidden" name="${field.name}" value="${field.value}"/>
              </#if>
	    </#list>
	  </form>
        </div>
    </#if>
    <script>
        var facetsOpen = 0;
        $("div.searchTOC-box").corner();
        $(".search-facets-head").click(function() {
            if (facetsOpen) {
                if (facetsOpen == this) {
                    $(this).next("tr").hide();
                    $(this).find(".search-facets-toggle").html('+');
                    facetsOpen = 0;
                } else {
                    $(facetsOpen).next("tr").hide();
                    $(facetsOpen).find(".search-facets-toggle").html('+');
                    $(this).next("tr").show();
                    $(this).find(".search-facets-toggle").html('-');
                    facetsOpen = this;
                }
            } else {
                $(this).next("tr").show();
                $(this).find(".search-facets-toggle").html('-');
                facetsOpen = this;
            }
        });
    </script>
    <#-- Search results -->
    <div style="float: right; text-align: left; width: 60%;">
        <ul class="searchhits">
            <#list individuals as individual>
                <li>                        
                    <@shortView uri=individual.uri viewContext="search" />
                </li>
            </#list>
        </ul>
        <#-- Paging controls -->
        <#if (pagingLinks?size > 0)>
            <div class="searchpages">
                Pages: 
                <#if prevPage??><a class="prev" href="${prevPage}" title="${i18n().previous}">${i18n().previous}</a></#if>
                <#list pagingLinks as link>
                    <#if link.url??>
                        <a href="${link.url}" title="${i18n().page_link}">${link.text}</a>
                    <#else>
                        <span>${link.text}</span> <#-- no link if current page -->
                    </#if>
                </#list>
                <#if nextPage??><a class="next" href="${nextPage}" title="${i18n().next_capitalized}">${i18n().next_capitalized}</a></#if>
            </div>
        </#if>
    </div>
    <br />
    <#-- VIVO OpenSocial Extension by UCSF -->
    <#if openSocial??>
        <#if openSocial.visible>
        <h3>OpenSocial</h3>
            <script type="text/javascript" language="javascript">
                // find the 'Search' gadget(s).
                var searchGadgets = my.findGadgetsAttachingTo("gadgets-search");
                var keyword = '${querytext}';
                // add params to these gadgets
                if (keyword) {
                    for (var i = 0; i < searchGadgets.length; i++) {
                        var searchGadget = searchGadgets[i];
                        searchGadget.additionalParams = searchGadget.additionalParams || {};
                        searchGadget.additionalParams["keyword"] = keyword;
                    }
                }
                else {  // remove these gadgets
                    my.removeGadgets(searchGadgets);
                }
            </script>

            <div id="gadgets-search" class="gadgets-gadget-parent" style="display:inline-block"></div>
        </#if>
    </#if>
</div> <!-- end contentsBrowseGroup -->

${stylesheets.add('<link rel="stylesheet" href="//code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css" />',
  				  '<link rel="stylesheet" href="${urls.base}/css/search.css" />')}

${headScripts.add('<script src="//code.jquery.com/ui/1.10.3/jquery-ui.js"></script>',
				  '<script type="text/javascript" src="${urls.base}/js/jquery_plugins/qtip/jquery.qtip-1.0.0-rc3.min.js"></script>',
                  '<script type="text/javascript" src="${urls.base}/js/tiny_mce/tiny_mce.js"></script>'
                  )}

${scripts.add('<script type="text/javascript" src="${urls.base}/js/searchDownload.js"></script>')}
