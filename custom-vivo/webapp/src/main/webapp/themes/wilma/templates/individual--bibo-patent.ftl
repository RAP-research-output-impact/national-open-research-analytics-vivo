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
  <span class="pub_meta"><a href="${urls.base}/search?searchMode=all&facet_journal_ss=${publishedIn.statements[0].object?url}">${publishedIn.statements[0].label}</a>, </span>
</#if>
<#if pubMeta[0].publisher??>
  <span class="pub_meta"><a href="${urls.base}/search?facet_content-type_ss=searchMode=all&facet_publisher_ss=${pubMeta[0].publisher?url}">${pubMeta[0].publisherName}</a>, </span>
</#if>


<p>
  <#if doi?has_content>
    <span class="pub_meta-value">DOI:<a href="http://doi.org/${doi}" title="Full Text via DOI" target="external">${doi}</a>, </span>
  </#if>
  <#if dimensionsid?has_content>
      <span class="pub_meta-value">Dimensions: <a href="https://app.dimensions.ai/details/patent/${dimensionsid}" title="Patent details from Dimensions" target="external">${dimensionsid}</a> </span>
  </#if>
</p>

<#if (assignees?has_content || assigneesOriginal?has_content) >
<div class="pub_authors-box">
  <h3>Assignee</h3>
  <#if assigneesOriginal?has_content>
   <p>Original:</p>
   <ul>
   <#list assigneesOriginal as assigneeOriginal>
     <li><a href="${urls.base}/search?searchMode=all&facet_original-assignee_ss=${assigneeOriginal.assigneeOriginal?url}">${assigneeOriginal.assigneeOriginalName}<#if assigneeOriginal.assigneeOriginalAbbreviation??>, ${assigneeOriginal.assigneeOriginalAbbreviation}</#if></a></li>
   </#list>
   </ul> 
  </#if>
  <#if assignees?has_content>
    <p>Current:</p>
    <ul>
      <#list assignees as assignee>
        <li>${assignee.assigneeName}<#if assignee.assigneeAbbreviation??>, ${assignee.assigneeAbbreviation}</#if></li>
      </#list>
    </ul>
  </#if>
</div>
</#if>

<!-- authors -->
<div class="pub_authors-box">
  <h3>Inventors</h3>
  <#if pg.getProperty(vivo + "relatedBy", vivo + "Authorship")??>
    <@p.objectProperty pg.getProperty(vivo + "relatedBy", vivo + "Authorship") false />
  </#if>
  <#if pubMeta?has_content && pubMeta[0].correspondingAuthorExists??><p>* Corresponding author</p></#if>
</div>
<!-- end .authors-box -->

<!-- author affiliations -->
<#if authorAffiliations?has_content>
<h3>Affiliations</h3>
<p>Organisations</p>
<ol>
<#list authorAffiliations as authorAffiliation>
  <#if authorAffiliation.affiliation?has_content && authorAffiliation.affiliationName?has_content>
    <#if authorAffiliation.grid?has_content>
      <#if !authorAffiliation.type?has_content>
        <li>(${authorAffiliation?index + 1}) ${authorAffiliation.affiliationName}, ${authorAffiliation.grid}</li>
      <#else>
          <li>(${authorAffiliation?index + 1}) <a href="${urls.base}/search?searchMode=all&facet_${authorAffiliation.type!organization}_ss=${authorAffiliation.affiliation?url}">${authorAffiliation.affiliationName}, ${authorAffiliation.grid}<#if authorAffiliation.affiliationAbbreviation??>, ${authorAffiliation.affiliationAbbreviation}</#if></a></li>
      </#if>
    <#else>
      <#if !authorAffiliation.type?has_content>
        <li>(${authorAffiliation?index + 1}) ${authorAffiliation.affiliationName}</li>
      <#else>
          <li>(${authorAffiliation?index + 1}) <a href="${urls.base}/search?searchMode=all&facet_${authorAffiliation.type!organization}_ss=${authorAffiliation.affiliation?url}">${authorAffiliation.affiliationName}</a></li>
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
<h3>Abstract</h3>
<div class="pub_abstract">
  <p>${abstract}</p>
</div>

<script type="text/javascript">
  $('.pub_abstract').truncate({max_length: 750});
</script>
</#if>

<#if supportingGrants?? && supportingGrants[0]??>
<!-- Other details -->
<div class="pub_other-details">
  <h3>Funders and Grants</h3>
  <ul class="pub_meta-list">
  <#list supportingGrants as supportingGrant>
    <li>
      <#if supportingGrant.funder??>
        <p style="margin-bottom:0.1em;"><a href="${urls.base}/search?searchMode=all&facet_funder_ss=${supportingGrant.funder?url}">${supportingGrant.funderLabel}</a></p>
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

<#if pubMeta?has_content && pubMeta[0].openAccessLabel?has_content>
<div class="pub_categories">
  <h3>Open access</h3>
  <p>
    <#if vcardUrl?has_content>
      <a href="${vcardUrl[0].url}" title="link to ${pubMeta[0].openAccessLabel} open-access full text">${pubMeta[0].openAccessLabel}</a>
    <#else>
      ${pubMeta[0].openAccessLabel}
    </#if>
  </p>
</div>
</#if>

<#if pubMeta?has_content && pubMeta[0].retrieval??>

<div class="pub_categories">
  <h3>Retrieval</h3>
  <#if pubMeta[0].retrieval == "dim-ddf"><#assign retrievalLabel = "Dimensions and DDF"></#if>
  <#if pubMeta[0].retrieval == "only-dim"><#assign retrievalLabel = "Dimensions only"></#if>
  <#if pubMeta[0].retrieval == "only-ddf"><#assign retrievalLabel = "DDF only"></#if>
  <#if retrievalLabel??>
    <p><a href="${urls.base}/search?searchMode=all&facet_retrieval_ss=${pubMeta[0].retrieval}">${retrievalLabel}</a></p>
  </#if>
</div>

</#if>

</div>
<!-- end .pub-v-main -->
  
<div class="pub-v-sidebar">

    <#if authorAffiliations?has_content>
      <#list authorAffiliations as authorAffiliation>
        <#if authorAffiliation.affiliation?has_content && authorAffiliation.affiliationName?has_content>
          <#if authorAffiliation.grid?has_content>
            <#if authorAffiliation.type == "university">
              <#assign universityExists = true />
            </#if>
          </#if>
        </#if>
      </#list>

      <#if universityExists??>
      <h3>NORA University Profiles</h3>
      <#list authorAffiliations as authorAffiliation>
        <#if authorAffiliation.affiliation?has_content && authorAffiliation.affiliationName?has_content>
          <#if authorAffiliation.grid?has_content>
            <#if authorAffiliation.type == "university">
              <p><a href="${profileUrl(authorAffiliation.affiliation)}">${authorAffiliation.affiliationName}</a></p>
	    </#if>
          </#if>
        </#if>
      </#list>
    </#if>

      </#if>

     <#if (patentMeta?has_content && patentMeta[0].filingStatus??) || (patentMeta?has_content && patentMeta[0].legalStatus??) || (pubMeta?has_content && pubMeta[0].year??) || (patentMeta?has_content && patentMeta.yearIssued??)>
      <h2>Status</h2>
      <#if patentMeta[0]?has_content && patentMeta[0].yearIssued??>
        <p><span class="pub_meta-value">Granted year: <a href="${urls.base}/search?facet_publication-year_ss=${patentMeta[0].yearIssued}">${patentMeta[0].yearIssued}</a></span></p>
      </#if>
      <#if pubMeta?has_content && pubMeta[0].year??>
        <p><span class="pub_meta-value">Publication year: <a href="${urls.base}/search?facet_publication-year_ss=${pubMeta[0].year}">${pubMeta[0].year}</a></span></p>
      </#if>
      <p>&nbsp;</p> <#-- TODO: style extra space between sections -->
      <#if patentMeta?has_content && patentMeta[0].filingStatus??>
        <p>Filing status: ${patentMeta[0].filingStatusName}</p>
      </#if>
      <#if patentMeta?has_content && patentMeta[0].legalStatus??>
        <p>Legal status: ${patentMeta[0].legalStatusName}</p>
      </#if>
     </#if>
        
	<#if vcardUrl?has_content>
	  <h3>External Sources</h3>
          <p><a href="${vcardUrl[0].url}" title="access link">Access</a></p>
        </#if>
	
<#if researchCategoriesFOR?has_content || researchCategoriesSDG?has_content || mainSubjectAreas?has_content>
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
  
 <#if researchCategoriesFOR?has_content>
  <div class="pub_keywords-enumeration clearfix">
    <p>Fields of Research</p>
    <ul class="one-line-list">
      <#list researchCategoriesFOR as researchCategory>
        <li role="list-item"><a href="${urls.base}/search?searchMode=all&facet_research-category_ss=${researchCategory.researchCategory?url}">${researchCategory.researchCategoryName}</a></li>
      </#list>
    </ul>
  </div>
 </#if>
 
 <#if researchCategoriesSDG?has_content>
  <div class="pub_keywords-enumeration clearfix">
    <p>Sustainable Development Goals</p>
    <ul class="one-line-list">
      <#list researchCategoriesSDG as researchCategory>
        <li role="list-item"><a href="${urls.base}/search?searchMode=all&facet_sdg_ss=${researchCategory.researchCategory?url}">${researchCategory.researchCategoryName}</a></li>
      </#list>
    </ul>
  </div>
 </#if>
 
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
