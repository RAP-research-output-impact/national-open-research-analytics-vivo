<#-- Publication pages -->
<#include "individual-setup.ftl">
<#import "lib-microformats.ftl" as mf>
<#import "lib-properties.ftl" as p>

<#global pg=propertyGroups>

<#-- helper to get data properties -->
<#function gdp prop>
    <#assign val = pg.getProperty(prop)!>
    <#if val?has_content>
        <#if val.statements[0]??>
            <#return val.statements[0].value>
        </#if>
    </#if>
</#function>

<#assign bibo = "http://purl.org/ontology/bibo/">
<#assign vivo = "http://vivoweb.org/ontology/core#">
<#assign wos = "http://webofscience.com/ontology/wos#">
<#assign abstractp = bibo + "abstract">
<#assign abstract = gdp(abstractp)!>
<#assign volumep = bibo + "volume">
<#assign volume = gdp(volumep)!>
<#assign issuep = bibo + "issue">
<#assign issue = gdp(issuep)!>
<#assign pageStartp = bibo + "pageStart">
<#assign pageStart = gdp(pageStartp)!>
<#assign pageEndp = bibo + "pageEnd">
<#assign pageEnd = gdp(pageEndp)!>
<#assign issnp = bibo + "issn">
<#assign issn = gdp(issnp)!>
<#assign doip = "http://purl.org/ontology/bibo/doi">
<#assign pmidp = "http://purl.org/ontology/bibo/pmid">
<#assign pmcidp = "http://vivoweb.org/ontology/core#pmcid">
<#assign dimensionsid = "http://vivoweb.org/ontology/core#identifier">
<#assign wosp = "http://webofscience.com/ontology/wos#wosId">
<#assign refp = "http://webofscience.com/ontology/wos#referenceCount">
<#assign citep = "http://webofscience.com/ontology/wos#citationCount">

<#--Number of labels present-->
<#if !labelCount??>
    <#assign labelCount = 0 >
</#if>
<#--Number of available locales-->
<#if !localesCount??>
    <#assign localesCount = 1>
</#if>
<#--Number of distinct languages represented, with no language tag counting as a language, across labels-->
<#if !languageCount??>
    <#assign languageCount = 1>
</#if>

<#-- Default individual profile page template -->
<#--@dumpAll /-->

<div class="n_main width-l">
<div class="pub-view">
  <div class="pub-v-main">

<section id="individual-intro" class="vcard" role="region" <@mf.sectionSchema individual/>>
    <section id="share-contact" role="region">
        <#-- No images -->
    </section>
    <!-- start section individual-info -->
    <section id="individual-info" ${infoClass!} role="region">
        <#include "individual-adminPanel.ftl">

        <#if individualProductExtensionPreHeader??>
            ${individualProductExtensionPreHeader}
        </#if>

        <p>
	${pubMeta[0].typeLabel!}
        <#if pubMeta[0].openAccess??>
          <img src="${urls.theme}/images/open-access.svg" width="20" alt="open access publication"/>
	</#if>
	</p>
        <header>
            <#if relatedSubject??>
                <h2>${relatedSubject.relatingPredicateDomainPublic} for ${relatedSubject.name}</h2>
                <p><a href="${relatedSubject.url}" title="${i18n().return_to(relatedSubject.name)}">&larr; ${i18n().return_to(relatedSubject.name)}</a></p>
            <#else>
                <h1 itemprop="name">
                    <#-- Label -->
                    <@p.label individual editable labelCount localesCount languageCount/>
                </h1>
            </#if>
        </header>

    <#if individualProductExtension??>
        ${individualProductExtension}
    <#else>
            </section> <!-- individual-info -->
        </section> <!-- individual-intro -->
    </#if>

<#assign nameForOtherGroup = "${i18n().other}">

<#assign doi=gdp(doip)!>
<#assign pmid=gdp(pmidp)!>
<#assign pmcid=gdp(pmcidp)!>
<#assign dimensionsid=gdp(dimensionsid)!>
<#assign wosId=gdp(wosp)!>
<#assign refs=gdp(refp)!>
<#assign cites=gdp(citep)!>

<p>
<!-- journal -->
<#assign publishedIn = pg.getProperty(vivo + "hasPublicationVenue")!>
<#if publishedIn?? && publishedIn.statements?? && publishedIn.statements[0]??>
  <span class="pub_meta"><a href="${urls.base}/search?searchMode=publications&facet_journal_ss=${publishedIn.statements[0].object}">${publishedIn.statements[0].label}</a>, </span>
</#if>
<#if pubMeta[0].publisher??>
  <span class="pub_meta"><a href="${urls.base}/search?searchMode=publications&facet_publisher_ss=${pubMeta[0].publisher}">${pubMeta[0].publisherName}</a>, </span>
</#if>
<#if pubMeta[0].issn??>
  <span class="pub_meta">ISSN ${pubMeta[0].issn}</span>
</#if>
</p>

<p>
<#if volume?has_content>
    <span class="pub_meta-value">Volume ${volume}, </span>
</#if>
<#if issue?has_content>
    <span class="pub_meta-value">${issue}, </span>
</#if>
<#if pageStart?has_content && pageEnd?has_content>
  <#if pageStart == pageEnd>
    <span class="pub_meta-value">Page ${pageStart}, </span>
  <#else>
    <span class="pub_meta-value">Pages ${pageStart}-${pageEnd}, </span>
  </#if>
</#if> 
<#if pubMeta[0].year??>
    <span class="pub_meta-value"><a href="${urls.base}/search?facet_publication-year_ss=${pubMeta[0].year}">${pubMeta[0].year}</a></span>
</#if>
</p>

<p>
  <#if doi?has_content>
    <span class="pub_meta-value">DOI:<a href="http://doi.org/${doi}" title="Full Text via DOI" target="external">${doi}</a>, </span>
  </#if>
  <#if dimensionsid?has_content>
    <#if dimensionsid?contains("pub.")>
      <span class="pub_meta-value">Dimensions: <a href="https://app.dimensions.ai/details/publication/${dimensionsid}" title="Publication details from Dimensions" target="external">${dimensionsid}</a>, </span>
    <#elseif dimensionsid?contains("-")>
      <span class="pub_meta-value">Dimensions: <a href="https://app.dimensions.ai/details/patent/${dimensionsid}" title="Patent details from Dimensions" target="external">${dimensionsid}</a>, </span>
    <#else>
      <span class="pub_meta-value">Dimensions: <a href="https://app.dimensions.ai/details/data_set/${dimensionsid}" title="Dataset details from Dimensions" target="external">${dimensionsid}</a>, </span>
    </#if>
  </#if>
  <#if pmcid?has_content>
    <span class="pub_meta-value">PMC: ${pmcid}, </span>
  </#if>
  <#if pmid?has_content>
    <span class="pub_meta-value">PMID: ${pmid}, </span>
  </#if>
</p>

<!-- authors -->
<div class="pub_authors-box">
  <h3>Authors</h3>
  <#if pg.getProperty(vivo + "relatedBy", vivo + "Authorship")??>
    <@p.objectProperty pg.getProperty(vivo + "relatedBy", vivo + "Authorship") false />
  </#if>
</div>
<!-- end .authors-box -->

<!-- author affiliations -->
<#if authorAffiliations?has_content>
<ol style="margin-top:2ex;">
<#list authorAffiliations as authorAffiliation>
  <#if authorAffiliation.affiliation?has_content && authorAffiliation.affiliationName?has_content>
    <#if authorAffiliation.grid?has_content>
      <#if !authorAffiliation.type?has_content>
        <li>(${authorAffiliation?index + 1}) ${authorAffiliation.affiliationName}, ${authorAffiliation.grid}</li>
      <#else>
          <li>(${authorAffiliation?index + 1}) <a href="${urls.base}/search?searchMode=publications&facet_${authorAffiliation.type!organization}_ss=${authorAffiliation.affiliation}">${authorAffiliation.affiliationName}, ${authorAffiliation.grid}</a></li>
      </#if>
    <#else>
      <#if !authorAffiliation.type?has_content>
        <li>(${authorAffiliation?index + 1}) ${authorAffiliation.affiliationName}</li>
      <#else>
          <li>(${authorAffiliation?index + 1}) <a href="${urls.base}/search?searchMode=publicationss&facet_${authorAffiliation.type!organization}_ss=${authorAffiliation.affiliation}">${authorAffiliation.affiliationName}</a></li>
      </#if>
    </#if>
  </#if>
</#list>
</ol>
</#if>

<#if countries?has_content || continents?has_content>
<div class="pub_categories">
  <#if countries?has_content>
  <h4>Countries</h4>
    <#list countries as country>
      <p><a href="${urls.base}/search?searchMode=all&facet_country_ss=${country.country?url}">${country.countryLabel}</a></p>
    </#list>
  </#if>
  <#if continents?has_content>
  <h4>Continents</h4>
    <#list continents as continent>
      <p><a href="${urls.base}/search?searchMode=all&facet_continent_ss=${continent.continent?url}">${continent.continentLabel}</a></p>
    </#list>
  </#if>
</div>
</#if>

<#if abstract?has_content>
<!-- abstract -->
<h3>Description</h3>
<div class="pub_abstract">
  <p>${abstract}</p>
</div>
</#if>

<#if supportedPublications?? && supportedPublications[0]??>
<!-- Associated publications -->
<div class="pub_other-details">
  <h3>Associated publications</h3>
  <ul class="pub_meta-list">
  <#list supportedPublications as supportedPublication>
    <li style="margin-bottom: 2ex;">
        <p style="margin-bottom:0.1em;"><a href="${profileUrl(supportedPublication.publication)}">${supportedPublication.label}</a></p>
    <#if supportedPublication.year?? || supportedPublication.journal?? || supportedPublication.type??>
      <p style="margin-bottom:0.1em;">
             <#if supportedPublication.year??><a href="${urls.base}/search?searchModel=all&facet_year_ss=${supportedPublication.year}">${supportedPublication.year}</a>, </#if> 
             <#if supportedPublication.journal??><a href="${urls.base}/search?searchModel=all&facet_journal_ss=${supportedPublication.journalObj}">${supportedPublication.journal}</a>, </#if> 
             <#if supportedPublication.type??><a href="${urls.base}/search?searchModel=all&facet_document-type_ss=${supportedPublication.type}">${supportedPublication.typeLabel}</a></#if>
      </p>
    </#if>
    </li>
  </#list>
  </ul>
</div>
<!-- end Associated publications -->
</#if>

</div>
<!-- end .pub-v-main -->
  
<div class="pub-v-sidebar">

<#if funders?has_content>
  <h3>Funders</h3>
  <ul class="pub_meta-list">
    <#list funders as funder>
      <li><a href="${urls.base}/search?searchMode=all&facet_funder_ss=${funder.funder}">${funder.funderLabel}</a></li>
    </#list>
  </ul>
</#if>

<#if supportingGrants?? && supportingGrants[0]??>
<!-- Other details -->
<div class="pub_other-details">
  <h3>Supporting grants</h3>
  <ul class="pub_meta-list">
  <#list supportingGrants as supportingGrant>
    <li>
      <#if supportingGrant.funder??>
        <p style="margin-bottom:0.1em;"><a href="${urls.base}/search?searchMode=&facet_funder_ss=${supportingGrant.funder}">${supportingGrant.funderLabel}</a></p>
      </#if>
        <p style="margin-bottom:0.1em;"><a href="${profileUrl(supportingGrant.grant)}">${supportingGrant.grantLabel}</a></p>
      <#if supportingGrant.grantNumber??>
        <p style="margin-bottom:0.1em;">Grant number ${supportingGrant.grantNumber}</p>
      </#if>
      <#if supportingGrant.awardAmountEur??>
        <p style="margin-bottom:0.1em;">&euro;${supportingGrant.awardAmountEur}</p>
      </#if>
    </li>
  </#list>
  </ul>
</div>
<!-- end other-details -->
</#if>

<#if researchCategoriesFOR?has_content>
<!-- categories/classification -->
<div class="pub_categories">
  <h3>Research Categories</h3>
 <#if researchCategoriesFOR?has_content>
  <div class="pub_keywords-enumeration clearfix">
    <p>Fields of Research</p>
    <ul class="one-line-list">
      <#list researchCategoriesFOR as researchCategory>
        <li role="list-item"><a href="${urls.base}/search?searchMode=all&facet_research-category_ss=${researchCategory.researchCategory}">${researchCategory.researchCategoryName}</a></li>
      </#list>
    </ul>
  </div>
 </#if>
</div>
<!-- end .pub_categories -->
</#if>

<#if pubMeta?has_content && pubMeta[0]?has_content && pubMeta[0].license?has_content>
  <h3>Licence</h3>
  <p>${pubMeta[0].license}</p>
</#if>

<#if vcardUrl?has_content>
  <h3>External sources</h3>
  <p>Access at <a href="${vcardUrl[0].url}">Figshare</a></p>
</#if>

</div>
<!-- end .pub-v-sidebar -->

</div>
<!-- end pub-view  -->

</div>
<!-- end .n_main -->

<#assign skipThis = propertyGroups.pullProperty (citep)!>
<#assign skipThis = propertyGroups.pullProperty (refp)!>

<script>
    var imagesPath = '${urls.images}';
        var individualUri = '${individual.uri!}';
        var individualPhoto = '${individual.thumbNail!}';
        var exportQrCodeUrl = '${urls.base}/qrcode?uri=${individual.uri!}';
        var baseUrl = '${urls.base}';
    var i18nStrings = {
        displayLess: '${i18n().display_less}',
        displayMoreEllipsis: '${i18n().display_more_ellipsis}',
        showMoreContent: '${i18n().show_more_content}',
        verboseTurnOff: '${i18n().verbose_turn_off}',
        researchAreaTooltipOne: '${i18n().research_area_tooltip_one}',
        researchAreaTooltipTwo: '${i18n().research_area_tooltip_two}'
    };
    var i18nStringsUriRdf = {
        shareProfileUri: '${i18n().share_profile_uri}',
        viewRDFProfile: '${i18n().view_profile_in_rdf}',
        closeString: '${i18n().close}'
    };
</script>

${stylesheets.add('<link rel="stylesheet" href="${urls.base}/css/individual/individual.css" />')}

${headScripts.add('<script type="text/javascript" src="${urls.base}/js/jquery_plugins/qtip/jquery.qtip-1.0.0-rc3.min.js"></script>',
                  '<script type="text/javascript" src="${urls.base}/js/tiny_mce/tiny_mce.js"></script>')}

${scripts.add('<script type="text/javascript" src="${urls.base}/js/individual/moreLessController.js"></script>')}

<script type="text/javascript">
    i18n_confirmDelete = "${i18n().confirm_delete}"
</script>


${scripts.add('<script type="text/javascript" src="https://d1bxh8uas1mnw7.cloudfront.net/assets/embed.js"></script>')}
