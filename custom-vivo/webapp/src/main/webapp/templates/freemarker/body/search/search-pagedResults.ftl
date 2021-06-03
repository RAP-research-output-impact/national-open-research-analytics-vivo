<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Template for displaying paged search results -->

<script src="${urls.theme}/js/jquery.corner.js"></script>

<h2 class="searchHeading">NORA Search</h2>

<#if typeCounts??>

<div class="searchResultTypes">

  <div class="searchResultsTypeHeader"><p>Results: <p></div>

  <#assign total = 0>
  <#list typeCounts as typeCount>
    <#assign total = total + typeCount.count>
  </#list>

  <#if allRecordsSelected?? && allRecordsSelected>
    <a href="${allTypesLink.url}"><div class="searchResultsType searchResultsTypeNonzero searchResultsTypeActive"><p class="searchResultsTypeName">All types</p><p>${total!0}</p></div></a>
  <#else>
    <a href="${allTypesLink.url}"><div class="searchResultsType searchResultsTypeNonzero"><p class="searchResultsTypeName">All types</p><p>${total!0}</p></div></a>
  </#if>
    <div class="searchResultsTypeSpacer"><div class="searchResultsTypeSpacerLine"></div></div>
 
  <#list typeCounts as typeCount>
    <#if (typeCount.count > 0) && typeCount.selected>
      <a href="${typeCount.url}"/search?><div class="searchResultsType searchResultsTypeActive searchResultsTypeNonzero"><p class="searchResultsTypeName">${typeCount.text}</p><p>${typeCount.count}</p></div></a>
    <#elseif (typeCount.count > 0) && (!typeCount.selected)>
      <a href="${typeCount.url}"/search?><div class="searchResultsType searchResultsTypeNonzero"><p class="searchResultsTypeName">${typeCount.text}</p><p>${typeCount.count}</p></div></a>
    <#else>
      <div class="searchResultsType searchResultsTypeZero"><p>${typeCount.text}</p><p>0</p></div>
    </#if>
    <div class="searchResultsTypeSpacer"><div class="searchResultsTypeSpacerLine"></div></div>
  </#list>
    
  <a href="${urls.base}/search?searchMode=all"><div style="width:3em;" class="searchResultsType searchResultsTypeNonzero"><p style="margin-top:0.1em;line-height:1.3em;color:#2485AE;" lass="searchResultsTypeName newsearch">New Search</p></div></a>

</div> <!-- searchResultTypes -->

</#if>

<div class="contentsBrowseGroup">
    <#-- Refinement links -->
    <div class="searchTOC-box">
        <table border="1" class="searchTOC">
            <#if commonFacets?has_content>
	        <tr class="search-facets-head">
                  <td style="text-align:left;">GENERAL FILTERS:</td>
		</tr>
                <#list commonFacets as facet>
		  <#if facet.displayInSidebar>
                    <tr class="search-facets-head">
                        <td style="align: left">
                            <h4 class="search-facets-title"><div class="search-facets-toggle">+</div>${facet.publicName}</h4>
                        </td>
                    </tr>
                    <tr class="search-facets" style="display: none;">
                        <td>
			  <@facetCategories facet />
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
                  <td style="text-align:left;">${recordTypeLabel} FILTERS:</td>
		</tr>
                <#list additionalFacets as facet>
		  <#if facet.displayInSidebar && facet.categories?has_content>
                    <tr class="search-facets-head">
                        <td style="align: left">
                            <h4 class="search-facets-title"><div class="search-facets-toggle">+</div>${facet.publicName}</h4>
                        </td>
                    </tr>
                    <tr class="search-facets" style="display: none;">
                        <td>
			  <@facetCategories facet />
                        </td>
                    </tr>
		  </#if>
                </#list>
            </#if>
        </table>
    </div>
    <div id="nora-search-form">
        <form action="" method="GET">
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
	    <#if searchMode?has_content>
                <input type="hidden" name="searchMode" value="${searchMode}" />
	    </#if>
            <input id="nora-search-submit" type="submit" value="Search"/>
        </form>
        <!--
        <span id="searchHelp" style="text-align: right;"><a href="${urls.base}/searchHelp" title="${i18n().search_help}">${i18n().not_expected_results}</a></span>
        -->
    </div>
    <div style="width: 60%; float: right;">
        <#list noraQueryReduce as link>
            <div class="qr-box">
                <span class="qr-text">${link.text}</span>
                <span class="qr-link" onClick="window.location.href='${link.url}';"><a href="${link.url}">X</a></span>
            </div>
        </#list>
    </div>
    <#if sortFormHiddenFields?? && commonFacets?has_content>
        <div style="width: 60%; float: right; text-align: right;">
          Sort by <form style="display: inline;" action="${urls.base}/search" method="GET">
            <select name="sortField" onchange="this.form.submit()">
              <option <#if sortField?? && sortField = "sort_year_s|ASC">selected="selected"</#if> value="sort_year_s|ASC">year (ascending)</option>
              <option <#if sortField?? && sortField = "sort_year_s|DESC">selected="selected"</#if> value="sort_year_s|DESC">year (descending)</option>
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
    <div style="float: right; text-align: left; width: 63%;">
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

<script>

  let baseServiceURL = window.location.href.replace('#', '');
  let firstFacetCategory = new Map();

  function nextFacet(fieldName, markupType, nextPrev) {
    currentFirstCategory = firstFacetCategory[fieldName];
    if(currentFirstCategory === undefined) {
      currentFirstCategory = 0;
    }
    if("next" === nextPrev) {
      currentFirstCategory += 15;
    } else {
      if(currentFirstCategory < 15) {
        currentFirstCategory = 0;
      } else {
        currentFirstCategory -= 15;
      }
    }
    firstFacetCategory[fieldName] = currentFirstCategory;
    if("checkboxes" === markupType) {
      if(currentFirstCategory < $("#" + fieldName + "-categories").children('li').length) {
        reshowCheckboxes($("#" + fieldName + "-categories").children('li'), currentFirstCategory, currentFirstCategory + 15);
      } else {
        fetchFacetJson(fieldName, currentFirstCategory, function(json) { updateFacetCheckboxes(json, fieldName, markupType)});
      }
    } else {
      fetchFacetJson(fieldName, currentFirstCategory, function(json) { updateFacetLinks(json, fieldName, markupType)});
    }
    if(currentFirstCategory > 0) {
      $("#" + fieldName + "-prev").show();
    } else {
      $("#" + fieldName + "-prev").hide();
    }
    return false;
  }

  function reshowCheckboxes(children, firstVisible, lastVisible) {
    children.each(function(i, input) {
      if(i >= firstVisible && i < lastVisible) {
        $(input).show();
      } else {
        $(input).hide();
      }
    });
  }

  function updateFacetCheckboxes(json, fieldName) {
    $("#" + fieldName + "-categories").children('li').each(function(i, input) {
       $(input).hide();
    });
    for(var i = 0; i < json.length; i++) {
        var cat = json[i];
        $("#" + fieldName + "-categories").append("<li><input type=\"checkbox\" name=\"" + fieldName +  "\" value=\"" + cat.value + "\"/><a href=\"" + cat.url + "\">" + cat.text + "</a> <span>(" + cat.count + ")</span></li>");
    }
    if(json.length < 15) {
      $("#" + fieldName + "-next").hide();
    }
  }

  function updateFacetLinks(json, fieldName) {
    $("#" + fieldName + "-categories").children('li').each(function(i, input) {
       $(input).remove();
    });
    for(var i = 0; i < json.length; i++) {
        var cat = json[i];
        $("#" + fieldName + "-categories").append("<li><a href=\"" + cat.url + "\">" + cat.text + "</a> <span>(" + cat.count + ")</span></li>");
    }
    if(json.length < 15) {
      $("#" + fieldName + "-next").hide();
    }
  }

  function fetchFacetJson(fieldName, currentFirstCategory, callback) {
    var serviceURL = baseServiceURL + "&json=1&jsonFacet=" + fieldName + "&facetOffset=" + currentFirstCategory;
    //alert(serviceURL);
    return fetch(serviceURL)
        .catch(err => console.error('request failed: ', err))
	.then(rToJson)
	.then(callback);
  }

  function rToJson(r) {
    if (r.ok) return r.json()
        else throw new Error('Network response was not ok.');
  }


</script>

<#macro facetCategories facet>
  <#if facet.unionFacet>
    <form action="${urls.base}/search" method="GET">
      <p style="margin-bottom:0;text-align:left;"><em>
      Check one or more and </em>
      <input style="align: left;" type="submit" name="submit" value="SEARCH"/>
      </p>
      <#if facet.childFacets?has_content>
        <#list facet.childFacets as child>
          <h5 style="text-align:left;" class="search-facets-title">${child.publicName}</h5>
	  <@facetCategoriesCheckboxes child />
	</#list>
      <#else>
        <@facetCategoriesCheckboxes facet />
      </#if>
      <#list sortFormHiddenFields as field>
        <#if field.name?? && (field.name != "sortField") && (field.name != "startIndex") && field.value??>
          <input type="hidden" name="${field.name}" value="${field.value}"/>
        </#if>
      </#list>
    </form>
  <#else>
    <#if facet.childFacets?has_content>
      <#list facet.childFacets as child>
        <@facetCategoriesLinks child />
      </#list>
    <#else>
      <@facetCategoriesLinks facet />
    </#if>
  </#if>
</#macro>

<#macro facetCategoriesCheckboxes facet>
  <#assign showNextLink = false>
  <ul id="${facet.fieldName}-categories">
    <#list facet.categories as category>
      <#if category.text?has_content>
        <#if category?index == 14>
          <#assign showNextLink = true>
	</#if>
        <#if facet.parentFacet??>
          <#assign facetFieldName = facet.parentFacet.fieldName>
        <#else>
          <#assign facetFieldName = facet.fieldName>
        </#if>
        <li><input name="${facetFieldName}" value="${category.value!}" type="checkbox">
          <a href="${category.url}" title="${category.text}">${category.text}</a><span>(${category.count})</span>
        </li>
      </#if>
    </#list>
  </ul>
  <#if showNextLink>
    <a id="${facet.fieldName}-prev" href="#" onClick="return nextFacet('${facet.fieldName}', 'checkboxes', 'prev');">previous</a>
    <script> 
      $('#${facet.fieldName}-prev').hide();
    </script>
    <a style="margin-left:2em;" id="${facet.fieldName}-next" href="#" onClick="return nextFacet('${facet.fieldName}', 'checkboxes', 'next');">next</a>
  </#if>
</#macro>

<#macro facetCategoriesLinks facet>
  <#assign showNextLink = false>
  <ul id="${facet.fieldName}-categories">
    <#list facet.categories as category>
      <#if category.text?has_content>
        <#if category?index == 14>
          <#assign showNextLink = true>
	</#if>
        <li><a href="${category.url}" title="${category.text}">${category.text}</a><span>(${category.count})</span></li>
      </#if>
    </#list>
  </ul>	
  <#if showNextLink>
    <a id="${facet.fieldName}-prev" href="#" onClick="return nextFacet('${facet.fieldName}', 'links', 'prev');">previous</a>
    <script> 
      $('#${facet.fieldName}-prev').hide();
    </script>
    <a style="margin-left:2em;" id="${facet.fieldName}-next" href="#" onClick="return nextFacet('${facet.fieldName}', 'links', 'next');">next</a>
  </#if>
</#macro>

${stylesheets.add('<link rel="stylesheet" href="//code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css" />',
  				  '<link rel="stylesheet" href="${urls.base}/css/search.css" />')}

${headScripts.add('<script src="//code.jquery.com/ui/1.10.3/jquery-ui.js"></script>',
				  '<script type="text/javascript" src="${urls.base}/js/jquery_plugins/qtip/jquery.qtip-1.0.0-rc3.min.js"></script>',
                  '<script type="text/javascript" src="${urls.base}/js/tiny_mce/tiny_mce.js"></script>'
                  )}

${scripts.add('<script type="text/javascript" src="${urls.base}/js/searchDownload.js"></script>')}
