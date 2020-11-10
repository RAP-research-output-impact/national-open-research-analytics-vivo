<#-- $This file is distributed under the terms of the license in LICENSE$ -->

<#--
    Individual profile page template for vivo:University individuals. This is the default template for universities
    in the Wilma theme and should reside in the themes/wilma/templates directory.
-->

<#--
NORA: Don't call individual-setup as it retrieves the whole grouped property list, which is very
slow for universities and will not be used.  Instead, copy the relevant lines from
individual-setup below. 
include "individual-setup.ftl" -->

<#import "lib-list.ftl" as l>
<#import "lib-properties.ftl" as p>

<#assign editable = individual.editable>

<#assign core = "http://vivoweb.org/ontology/core#">

<#import "lib-vivo-properties.ftl" as vp>
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

<section style="width: auto; float: left;" id="individual-intro" class="vcard person" role="region">

    <section id="share-contact" role="region" style="width:auto;padding-bottom:2em;">
        <!-- Image -->
        <#assign individualImage>
	  <#--
            <@p.image individual=individual
                      propertyGroups=propertyGroups
                      namespaces=namespaces
                      editable=editable
                      showPlaceholder="always" />
          -->
	  <#if universityMeta?? && universityMeta[0].abbreviation??>
	    <#assign abbr = universityMeta[0].abbreviation>
	    <#if "CBS" == abbr || "SDU" == abbr || "DTU" == abbr>
	      <#assign logoFilename = abbr + ".png">
	    <#else>
	      <#assign logoFilename = abbr + ".jpg">
	    </#if>
	    <#if "ITU" == abbr>
              <#assign logoHeight = 150>
	    <#elseif "CBS" == abbr>
              <#assign logoHeight = 48>
	    <#elseif "RUC" == abbr || "AU" == abbr>
              <#assign logoHeight = 120>
	    <#elseif "KU" == abbr>
              <#assign logoHeight = 275>
	    <#elseif "SDU" == abbr>
              <#assign logoHeight = 75>
	    <#else>
              <#assign logoHeight = 200>
	    </#if>
            <img height="${logoHeight}" src="${urls.theme}/images/${logoFilename}" alt="${abbr} logo" />
	  </#if>
        </#assign>

        ${individualImage}
        
    </section>

    <section id="individual-info" ${infoClass!} role="region" style="width:auto;margin-right:4em;">
        
	<header>
            <#if relatedSubject??>
                <h2>${relatedSubject.relatingPredicateDomainPublic} ${i18n().indiv_foafperson_for} ${relatedSubject.name}</h2>
                <p><a href="${relatedSubject.url}" title="${i18n().indiv_foafperson_return}">&larr; ${i18n().indiv_foafperson_return} ${relatedSubject.name}</a></p>
            <#else>
                <h1 class="foaf-person">
                    <#-- Label -->
                    <span itemprop="name" class="fn"><@p.label individual false labelCount localesCount/></span>
                </h1>
            </#if>
	</header>
    
        <#if universityMeta?has_content && universityMeta[0]?has_content>
	  <#if universityMeta[0].street??><p>${universityMeta[0].street}</p></#if>
	  <#if universityMeta[0].country??><p>Country:  ${universityMeta[0].country}</p></#if>
	  <#if universityMeta[0].continent??><p>Continent: ${universityMeta[0].continent}</p></#if>
	  <#if universityMeta[0].web??><p style="margin-top:1em;">Web: <a href="${universityMeta[0].web}" target="external">${universityMeta[0].web}</a></p></#if>
	  <#if universityMeta[0].grid??><p>GRID: <a href="https://www.grid.ac/institutes/${universityMeta[0].grid}" target="external">https://www.grid.ac/institutes/${universityMeta[0].grid}</a></p></#if> 
	  <#if universityMeta[0].ror??><p>ROR: <a href="${universityMeta[0].ror}" target="external">${universityMeta[0].ror}</a></p></#if>
	  <#if universityMeta[0].isni??><p>ISNI: <a href="${universityMeta[0].isni}" target="external">${universityMeta[0].isni}</a><p></#if>
	  <#if universityMeta[0].noraOrganisationType??><p style="margin-top:1em;">NORA organisation type: ${universityMeta[0].noraOrganisationType}</p></#if>
		  
        </#if>

    </section>

<section style="float:left;">

<p style="width:15em;"><strong>Records linked to this university for the years 2014 to 2019</strong></p>

<ul style="list-style-type:none;">
  <li>Publications <a id="publications_count" style="float:right;" href="${urls.base}/search?facet_content-type_ss=publications&facet_organization-all_ss=${individual.uri?url}">0</a></li>
  <li>Datasets <a id="datasets_count" style="float:right;" href="${urls.base}/search?facet_content-type_ss=datasets&facet_organization-all_ss=${individual.uri?url}">0</a></li>
  <li>Grants <a id="grants_count" style="float:right;" href="${urls.base}/search?facet_content-type_ss=grants&facet_organization-all_ss=${individual.uri?url}">0</a></li>
  <li>Patents <a id="patents_count" style="float:right;" href="${urls.base}/search?facet_content-type_ss=patents&facet_organization-all_ss=${individual.uri?url}">0</a></li>
  <li>Clinical trials <a id="clinical_trials_count" style="float:right;" href="${urls.base}/search?facet_content-type_ss=clinical_trials&facet_organization-all_ss=${individual.uri?url}">0</a></li>
</ul>

<script type="text/javascript">

  fetchFacetJson("${urls.base}/search?json=1&&facet_organization-all_ss=${individual.uri?url}");

  function updateSidebar(json) {
    for(var i = 0; i < json.length; i++) {
      var cat = json[i];
      if("publications" === cat.value || "datasets" === cat.value || "grants" === cat.value 
          || "patents" === cat.value || "clinical_trials" === cat.value) {
        $("#" + cat.value + "_count").text(cat.count);
      }
    }
   }

   function fetchFacetJson(fetchUrl) {
     return fetch(fetchUrl)
        .catch(err => console.error('request failed: ', err))
     .then(rToJson)
     .then(updateSidebar);
   }

   function rToJson(r) {
     if (r.ok) return r.json()
       else throw new Error('Network response was not ok.');
   }

</script>

</section>

<section style="clear:both;padding-top:1em;">
  <#if universityMeta?? && universityMeta[0].abbreviation??>
    <#include "${universityMeta[0].abbreviation}.ftl">
  </#if>
</section>

<#assign nameForOtherGroup = "${i18n().other}">

<!-- Property group menu or tabs -->
<#--
     With release 1.6 there are now two types of property group displays: the original property group
     menu and the horizontal tab display, which is the default. If you prefer to use the property
     group menu, simply substitute the include statement below with the one that appears after this
     comment section.

     <#include "individual-property-group-menus.ftl">
-->

<#-- include "individual-property-group-tabs.ftl" -->

<#assign rdfUrl = individual.rdfUrl>

<#if rdfUrl??>
    <script>
        var individualRdfUrl = '${rdfUrl}';
    </script>
</#if>
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

${stylesheets.add('<link rel="stylesheet" href="${urls.base}/css/individual/individual.css" />',
                  '<link rel="stylesheet" href="${urls.base}/css/individual/individual-vivo.css" />',
                  '<link rel="stylesheet" href="${urls.base}/js/jquery-ui/css/smoothness/jquery-ui-1.12.1.css" />',
                  '<link rel="stylesheet" type="text/css" href="${urls.base}/css/jquery_plugins/qtip/jquery.qtip.min.css" />')}

${headScripts.add('<script type="text/javascript" src="${urls.base}/js/tiny_mce/tiny_mce.js"></script>',
                  '<script type="text/javascript" src="${urls.base}/js/jquery_plugins/qtip/jquery.qtip.min.js"></script>',
                  '<script type="text/javascript" src="${urls.base}/js/jquery_plugins/jquery.truncator.js"></script>')}

${scripts.add('<script type="text/javascript" src="${urls.base}/js/individual/individualUtils.js"></script>',
              '<script type="text/javascript" src="${urls.base}/js/individual/individualQtipBubble.js"></script>',
              '<script type="text/javascript" src="${urls.base}/js/individual/individualUriRdf.js"></script>',
			  '<script type="text/javascript" src="${urls.base}/js/individual/moreLessController.js"></script>',
              '<script type="text/javascript" src="${urls.base}/js/jquery-ui/js/jquery-ui-1.12.1.min.js"></script>',
              '<script type="text/javascript" src="${urls.base}/js/imageUpload/imageUploadUtils.js"></script>',
              '<script async type="text/javascript" src="https://d1bxh8uas1mnw7.cloudfront.net/assets/embed.js"></script>',
              '<script async type="text/javascript" src="//cdn.plu.mx/widget-popup.js"></script>')}

<script type="text/javascript">
    i18n_confirmDelete = "${i18n().confirm_delete}";
</script>
