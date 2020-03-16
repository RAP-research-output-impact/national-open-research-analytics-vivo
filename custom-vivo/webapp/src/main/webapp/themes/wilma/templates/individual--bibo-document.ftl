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
  <span class="pub_meta"><a href="${profileUrl(publishedIn.statements[0].object)}">${publishedIn.statements[0].label}</a>, </span>
</#if>
<#if pubMeta[0].publisher??>
  <span class="pub_meta"><a href="${profileUrl(pubMeta[0].publisher)}">${pubMeta[0].publisherName}</a>, </span>
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
  <#if doi??>
    <span class="pub_meta-value">DOI:<a href="http://doi.org/${doi}" title="Full Text via DOI" target="external">${doi}</a>, </span>
  </#if>
  <#if dimensionsid??>
    <span class="pub_meta-value">Dimensions: <a href="https://app.dimensions.ai/details/publication/${dimensionsid}" title="Publication details from Dimensions" target="external">${dimensionsid}</a>, </span>
  </#if>
  <#if pmcid??>
    <span class="pub_meta-value">PMC: ${pmcid}, </span>
  </#if>
  <#if pmid??>
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
<#if authorAffiliations??>
<ol style="margin-top:2ex;">
<#list authorAffiliations as authorAffiliation>
  <li>(${authorAffiliation?index + 1}) <a href="${profileUrl(authorAffiliation.affiliation)}">${authorAffiliation.affiliationName}</a></li>  
</#list>
</ol>
</#if>

<!-- abstract -->
<div class="pub_abstract">
  <p>${abstract}</p>
</div>

<!-- Other details -->
<div class="pub_other-details">
  <h3>Funders and Grants</h3>
  <ul class="pub_meta-list">
    <#if pg.getProperty(vivo + "relatedBy", vivo + "Grant")??>
      <@p.objectProperty pg.getProperty(vivo + "relatedBy", vivo + "Grant") false />
    </#if>
  </ul>

</div>
<!-- end other-details -->


<!-- categories/classification -->
<div class="pub_categories">
  <h3>Research Categories</h3>

<#-- Research areas do not appear to be present in the RDF data 2019-05-13 -->
 <#if pg.getProperty(vivo + "hasSubjectArea")??>
  <div class="pub_keywords-enumeration clearfix">
    <h4>Research Areas:</h4>
    <ul class="one-line-list">
      <@p.objectProperty pg.getProperty(vivo + "hasSubjectArea") false />
    </ul>
  </div>
 </#if>

</div>
<!-- end .pub_categories -->

</div>
<!-- end .pub-v-main -->
  
<div class="pub-v-sidebar">

<h2>Metrics & Indicators</h2>
    <div class="pv-metrics">
      <p class='pv-metrics-src'><img src="https://38h6q83kpel22aipe0iux4i1-wpengine.netdna-ssl.com/wp-content/themes/dimensions-2019/dist/images/dimensions-logo-400x80.png" alt="Dimensions" width="150"/></p>
      <h3>Publication Metrics</h3>
      <#if pubMeta[0].timesCited??>
        <p>Times Cited: <span>${pubMeta[0].timesCited}</span></p>
      </#if>
      <#if pubMeta[0].relativeCitationRatio??>
        <p>Relative Citation ratio (RCR): <span>${pubMeta[0].relativeCitationRatio}</span></p>
      </#if>
      <#if pubMeta[0].fieldCitationRatio??>
        <p>Field Citation Ratio (FCR): <span>${pubMeta[0].fieldCitationRatio}</span></p>
      </#if>
    </div>
    <!-- end pv-metrics -->

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