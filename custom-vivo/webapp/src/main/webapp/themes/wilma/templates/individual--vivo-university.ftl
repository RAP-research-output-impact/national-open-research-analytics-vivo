<#-- $This file is distributed under the terms of the license in LICENSE$ -->

<#--
    Individual profile page template for vivo:University individuals. This is the default template for universities
    in the Wilma theme and should reside in the themes/wilma/templates directory.
-->

<#include "individual-setup.ftl">
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

<section id="individual-intro" class="vcard person" role="region">

    <section id="share-contact" role="region">
        <!-- Image -->
        <#assign individualImage>
            <@p.image individual=individual
                      propertyGroups=propertyGroups
                      namespaces=namespaces
                      editable=editable
                      showPlaceholder="always" />
        </#assign>

        <#if ( individualImage?contains('<img class="individual-photo"') )>
            <#assign infoClass = 'class="withThumb"'/>
        </#if>

        <div id="photo-wrapper">${individualImage}</div>
        <!-- Contact Info -->
        <div id="individual-tools-people">
            <span id="iconControlsLeftSide">
                <img id="uriIcon" title="${individual.uri}" src="${urls.images}/individual/uriIcon.gif" alt="${i18n().uri_icon}"/>
  				<#if checkNamesResult?has_content >
					<img id="qrIcon"  src="${urls.images}/individual/qr_icon.png" alt="${i18n().qr_icon}" />
                	<span id="qrCodeImage" class="hidden">${qrCodeLinkedImage!}
						<a class="qrCloseLink" href="#"  title="${i18n().qr_code}">${i18n().close_capitalized}</a>
					</span>
				</#if>
            </span>
        </div>
    
    </section>

    <section id="individual-info" ${infoClass!} role="region">
    <section id="right-hand-column" role="region">
        <#if editable>
        
	</#if>
        </section>
        <#-- include "individual-adminPanel.ftl" -->

        <header>
            <#if relatedSubject??>
                <h2>${relatedSubject.relatingPredicateDomainPublic} ${i18n().indiv_foafperson_for} ${relatedSubject.name}</h2>
                <p><a href="${relatedSubject.url}" title="${i18n().indiv_foafperson_return}">&larr; ${i18n().indiv_foafperson_return} ${relatedSubject.name}</a></p>
            <#else>
                <h1 class="foaf-person">
                    <#-- Label -->
                    <span itemprop="name" class="fn"><@p.label individual editable labelCount localesCount/></span>
                </h1>
            </#if>
	</header>
    
        <p>Street N/A</p>
        <#if universityMeta?has_content && universityMeta[0]?has_content>
	  <#if universityMeta[0].country??><p>Country: ${universityMeta[0].country}</p></#if>
	  <#if universityMeta[0].continent??><p>Continent: ${universityMeta[0].continent}</p></#if>
	  <#if universityMeta[0].web??><p style="margin-top:1em;">Web: <a href="${universityMeta[0].web}" target="external">${universityMeta[0].web}</a></p></#if>
	  <#if universityMeta[0].grid??><p>GRID: <a href="https://www.grid.ac/institutes/${universityMeta[0].grid}" target="external">https://www.grid.ac/institutes/${universityMeta[0].grid}</a></p></#if> 
	  <#if universityMeta[0].ror??><p>ROR: <a href="${universityMeta[0].ror}" target="external">${universityMeta[0].ror}</a></p></#if>
	  <#if universityMeta[0].isni??><p>ISNI: <a href="${universityMeta[0].isni}" target="external">${universityMeta[0].isni}</a><p></#if>
	  <#if universityMeta[0].noraOrganisationType??><p style="margin-top:1em;">NORA organisation type: ${universityMeta[0].noraOrganisationType}</p></#if>
        </#if>

    </section>

</span></section>

<section>

<strong>Records linked to this university for the years 2104 to 2017</p>

<ul style="width:30%;list-style-type:disc;">
  <li style="width:100%;margin-left:5em;">Publications <span style="float:right;">9999</span</li>
  <li style="width:100%;margin-left:5em;">Datasets <span style="float:right;">999</li>
  <li style="width:100%;margin-left:5em;">Grants <span style="float:right;">99</li>
  <li style="width:100%;margin-left:5em">Patents <span style="float:right;">9</li>
  <li style="width:100%;margin-left:5em;">Clinical trials <span style="float:right;">9</li>
</ul>

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
