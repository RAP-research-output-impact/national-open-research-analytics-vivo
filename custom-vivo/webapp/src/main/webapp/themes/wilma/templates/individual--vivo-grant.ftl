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
	<#if grantMeta[0].typeLabel??>
	    ${grantMeta[0].typeLabel!}
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

<#if grantMeta[0].funder??>
<p>
  <span class="pub_meta-value">Funder: <a href="${urls.base}/search?searchMode=all&facet_funder_ss=${grantMeta[0].funderOrg?url}">${grantMeta[0].funder}</a></span>
</p>
</#if>
<p>
  <#if dimensionsid??>
    <span class="pub_meta-value">Dimensions: <a href="https://app.dimensions.ai/details/grant/${dimensionsid}" title="Publication details from Dimensions" target="external">${dimensionsid}</a></span>
  </#if>
</p>

<#if grantInvestigators?has_content && grantInvestigators[0].investigator?has_content>
<!-- investigators -->
<div class="pub_authors-box">
  <h3>Investigators</h3>
  <#list grantInvestigators as grantInvestigator>
    <#if grantInvestigator.investigator?? && grantInvestigator.investigatorName??>
      <div class="pub_author-name">
        <a href="${urls.base}/search?searchMode=all&facet_contributor_ss=${grantInvestigator.investigator?url}">${grantInvestigator.investigatorName}</a>
      <#if grantInvestigator.rank?? && grantAffiliations??>
        <#list grantAffiliations as grantAffiliation>
          <#if grantAffiliation.ranks?has_content>
            <#list grantAffiliation.ranks?split(";") as rank>
              <#if rank == grantInvestigator.rank>
                (${grantAffiliation?index + 1})
              </#if>
            </#list>
          </#if>
        </#list>
      </#if>
      </div> <!-- pub_author-name -->
    </#if>
  </#list>
</div>
<!-- end .authors-box -->
</#if>

<!-- grant affiliations -->
<#if grantAffiliations?has_content && grantAffiliations[0].affiliation?has_content>
<h3>Affiliations</h3>
<p>Organisations</p>
<ol>
<#list grantAffiliations as grantAffiliation>
  <#if grantAffiliation.affiliation?has_content && grantAffiliation.affiliationName?has_content>
    <#if grantAffiliation.grid?has_content>
      <#if !grantAffiliation.type?has_content>
        <li>(${grantAffiliation?index + 1}) ${grantAffiliation.affiliationName}, ${grantAffiliation.grid}</li>
      <#else>
          <li>(${grantAffiliation?index + 1}) <a href="${urls.base}/search?searchMode=grants&facet_${grantAffiliation.type!organization}_ss=${grantAffiliation.affiliation?url}">${grantAffiliation.affiliationName}, ${grantAffiliation.grid}<#if grantAffiliation.affiliationAbbreviation??>, ${grantAffiliation.affiliationAbbreviation}</#if></a></li>
      </#if>
    <#else>
      <#if !grantAffiliation.type?has_content>
        <li>(${grantAffiliation?index + 1}) ${grantAffiliation.affiliationName}</li>
      <#else>
          <li>(${grantAffiliation?index + 1}) <a href="${urls.base}/search?searchMode=grants&facet_${grantAffiliation.type!organization}_ss=${grantAffiliation.affiliation?url}">${grantAffiliation.affiliationName}</a></li>
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
<h3>Abstract</h3>
<!-- abstract -->
<div class="pub_abstract">
  <p>${abstract}</p>
</div>

<script type="text/javascript">
  $('.pub_abstract').truncate({max_length: 750});
</script>
</#if>

<#if supportedPublications?? && supportedPublications[0]??>
<!-- Resulting publications -->
<div class="pub_other-details">
  <h3>Resulting publications</h3>
  <ul class="pub_meta-list">
  <#list supportedPublications as supportedPublication>
    <li style="margin-bottom: 2ex;">
        <p style="margin-bottom:0.1em;"><a href="${profileUrl(supportedPublication.publication)}">${supportedPublication.label}</a></p>
	<#if supportedPublication.year?? || supportedPublication.journal?? || supportedPublication.type??>
	  <p style="margin-bottom:0.1em;">
             <#if supportedPublication.year??><a href="${urls.base}/search?searchModel=all&facet_year_ss=${supportedPublication.year}">${supportedPublication.year}</a>, </#if> 
             <#if supportedPublication.journal??><a href="${urls.base}/search?searchModel=all&facet_journal_ss=${supportedPublication.journalObj?url}">${supportedPublication.journal}</a>, </#if> 
             <#if supportedPublication.type??><a href="${urls.base}/search?searchModel=all&facet_document-type_ss=${supportedPublication.type?url}">${supportedPublication.typeLabel}</a></#if>
	  </p>
	</#if>
    </li>
  </#list>
  </ul>
</div>
<!-- end Resulting publications -->
</#if>

</div>
<!-- end .pub-v-main -->
  
<div class="pub-v-sidebar">

    <#if grantAffiliations?has_content>
      <#list grantAffiliations as grantAffiliation>
        <#if grantAffiliation.affiliation?has_content && grantAffiliation.affiliationName?has_content>
          <#if grantAffiliation.grid?has_content>
            <#if grantAffiliation.type == "university">
              <#assign universityExists = true />
            </#if>
          </#if>
        </#if>
      </#list>

      <#if universityExists??>
      <h3>NORA University Profiles</h3>
      <#list grantAffiliations as grantAffiliation>
        <#if grantAffiliation.affiliation?has_content && grantAffiliation.affiliationName?has_content>
          <#if grantAffiliation.grid?has_content>
            <#if grantAffiliation.type == "university">
              <p><a href="${profileUrl(grantAffiliation.affiliation)}">${grantAffiliation.affiliationName}</a></p>
            </#if>
          </#if>
        </#if>
      </#list>
      </#if>
    </#if>

    <div class="pv-metrics">
      <h3>Funding information</h3>
      <#if grantMeta[0].startYear??>
        <p>Funding period: 	
        <span class="pub_meta-value"><a href="${urls.base}/search?searchMode=grants&facet_start-year_ss=${grantMeta[0].startYear}">
	    ${grantMeta[0].startYear}</a><#if grantMeta[0].endYear??>-<a 
	    href="${urls.base}/search?searchMode=grants&facet_start-year_ss=${grantMeta[0].endYear}">${grantMeta[0].endYear}</a></#if>
	</span>
	</p>
      </#if>
      <#if grantMeta[0].fundingAmount??>
        <p>Funding amount: <span>EUR ${grantMeta[0].fundingAmount}</span></p>
      </#if>
      <#if grantMeta[0].grantNumber??>
        <p>Grant number: <span>${grantMeta[0].grantNumber}</span></p>
      </#if>
    </div>
    <!-- end pv-metrics -->

<#if mainSubjectAreas?has_content || researchCategoriesFOR?has_content || researchCategoriesSDG?has_content>
<!-- categories/classification -->
<div class="pub_categories">
  <h3>Research Categories</h3>

<#if mainSubjectAreas?has_content>
  <div class="pub_keywords-enumeration clearfix">
    <p>Main Subject Area</p>
    <ul class="one-line-list">
      <#list mainSubjectAreas as mainSubjectArea>
        <li role="list-item"><a href="${urls.base}/search?searchMode=all&facet_main-subject-area_ss=${mainSubjectArea.subjectArea?url}">${mainSubjectArea.name}</a></li>
      </#list>
    </ul>
  </div>
</#if>

 <#if researchCategoriesFOR?has_content>
  <div class="pub_keywords-enumeration clearfix">
    <p>Fields of Research</p>
    <ul class="one-line-list">
      <#list researchCategoriesFOR as researchCategory>
        <li role="list-item"><a href="${urls.base}/search?searchMode=grants&facet_research-category_ss=${researchCategory.researchCategory?url}">${researchCategory.researchCategoryName}</a></li>
      </#list>
    </ul>
  </div>
 </#if>

 <#if researchCategoriesSDG?has_content>
  <div class="pub_keywords-enumeration clearfix">
    <p>Sustainable Development Goals</p>
    <ul class="one-line-list">
      <#list researchCategoriesSDG as researchCategory>
        <li role="list-item"><a href="${urls.base}/search?searchMode=grants&facet_sdg_ss=${researchCategory.researchCategory?url}">${researchCategory.researchCategoryName}</a></li>
      </#list>
    </ul>
  </div>
 </#if>

</div>
<!-- end .pub_categories -->
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

${headScripts.add('<script type="text/javascript" src="${urls.base}/js/jquery_plugins/jquery.truncator.js"></script>')}

${scripts.add('<script type="text/javascript" src="${urls.base}/js/individual/moreLessController.js"></script>')}

<script type="text/javascript">
    i18n_confirmDelete = "${i18n().confirm_delete}"
</script>


${scripts.add('<script type="text/javascript" src="https://d1bxh8uas1mnw7.cloudfront.net/assets/embed.js"></script>')}
