<#-- $This file is distributed under the terms of the license in LICENSE$  -->

<@widget name="login" include="assets" />

<#--
        With release 1.6, the home page no longer uses the "browse by" class group/classes display.
        If you prefer to use the "browse by" display, replace the import statement below with the
        following include statement:

            <#include "browse-classgroups.ftl">

        Also ensure that the homePage.geoFocusMaps flag in the runtime.properties file is commented
        out.
-->
<#import "lib-home-page.ftl" as lh>

<!DOCTYPE html>
<html lang="en">
    <head>
        <#include "head.ftl">
        <#if geoFocusMapsEnabled >
            <#include "geoFocusMapScripts.ftl">
        </#if>
        <script async type="text/javascript" src="${urls.base}/js/homePageUtils.js?version=x"></script>
    </head>

    <body class="${bodyClasses!}" onload="${bodyOnload!}">
    <#-- supplies the faculty count to the js function that generates a random row number for the search query -->
        <@lh.facultyMemberCount  vClassGroups! />
        <#include "identity.ftl">

        <#include "menu.ftl">

<div class="n_main width-l">
  <h1 class="n_hero-heading">NORA &mdash; National Open Research Analytics is a platform developed as part of the Open Research Analytics (OPERA) project, funded by the Danish Agency of Higher Education & Science. NORA is built with Dimensions data related to Danish Universities and University Hospitals from 2014-2017 and enhanced with data from Danish Research Indicators.</h1>
  <div class="n_cards">
    <article class="n_card">
      <a href="${urls.base}/search?searchMode=all" class="n_c-link-img">
        <img class="n_c-img" height="280" width="600" src="https://www.biosustain.dtu.dk/-/media/Andre_Universitetsenheder/Bibliotek/foto/Boeger2.ashx?mw=700&hash=1F7FE26E4903121CD49458449F8AD2BD686444F2" alt="search"> 
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/search?searchMode=all"><h3>Search</h3></a>
        <p>Search for publications, datasets, grants, patents and clincal trials</p>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/SDGdash" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/SDGs.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/SDGdash"><h3>Sustainable Development Goals Dashboard</h3></a>
        <p>An exploration of the connections between Danish scientific publications and the Sustainable Development Goals (SDGs)</p>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/intlcollabs" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/international_collabs.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="{urls.base}/intlcollabs"><h3>International Collaborations Dashboard</h3></a>
        <p>An overview of the top international co-authorship collaborations between Danish universities and their partners abroad.</p>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/dkcollabsash" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/national_collabs.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/dkcollabsash"><h3>National Collaboration Dashboard</h3></a>
        <p>An overview of the top national co-authorship collaborations between Danish universities and their national partners.</p>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/individuallist?vclassId=http%3A%2F%2Fvivoweb.org%2Fontology%2Fcore%23University" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/univ_profiles.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/individuallist?vclassId=http%3A%2F%2Fvivoweb.org%2Fontology%2Fcore%23University"><h3>University Profiles</h3></a>
        <p>Summary of key information to characterise the scientific production of Danish universities.</p>
      </div>
    </article>
    <!-- end .n_card -->

  </div>
  <!-- end .n-cards -->


</div>
<!-- end .n_main -->




        

        <#include "footer.ftl">
        <#-- builds a json object that is used by js to render the academic departments section -->
        <@lh.listAcademicDepartments />
    <script>
        var i18nStrings = {
            researcherString: '${i18n().researcher}',
            researchersString: '${i18n().researchers}',
            currentlyNoResearchers: '${i18n().currently_no_researchers}',
            countriesAndRegions: '${i18n().countries_and_regions}',
            countriesString: '${i18n().countries}',
            regionsString: '${i18n().regions}',
            statesString: '${i18n().map_states_string}',
            stateString: '${i18n().map_state_string}',
            statewideLocations: '${i18n().statewide_locations}',
            researchersInString: '${i18n().researchers_in}',
            inString: '${i18n().in}',
            noFacultyFound: '${i18n().no_faculty_found}',
            placeholderImage: '${i18n().placeholder_image}',
            viewAllFaculty: '${i18n().view_all_faculty}',
            viewAllString: '${i18n().view_all}',
            viewAllDepartments: '${i18n().view_all_departments}',
            noDepartmentsFound: '${i18n().no_departments_found}'
        };
        // set the 'limmit search' text and alignment
        if  ( $('input.search-homepage').css('text-align') == "right" ) {
             $('input.search-homepage').attr("value","${i18n().limit_search} \u2192");
        }
    </script>
    </body>
</html>
