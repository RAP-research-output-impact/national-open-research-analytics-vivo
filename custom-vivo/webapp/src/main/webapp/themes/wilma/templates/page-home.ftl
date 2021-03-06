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
  <div class="n_cards">
    <article class="n_card">
      <a href="${urls.base}/search?searchMode=all" class="n_c-link-img">
        <img class="n_c-img" height="225" width="300" src="${urls.theme}/images/search.png" alt="search"> 
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/search?searchMode=all"><h3>Search</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/individuallist?vclassId=http%3A%2F%2Fvivoweb.org%2Fontology%2Fcore%23University" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/univ_profiles.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/individuallist?vclassId=http%3A%2F%2Fvivoweb.org%2Fontology%2Fcore%23University"><h3>University Profiles</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/SDGdash" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/SDGs2.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/SDGdash"><h3>Sustainable Development Goals</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/intlcollabs" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/international_collabs2.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="{urls.base}/intlcollabs"><h3>International Collaborations</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/dkcollabsash" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/national_collabs.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/dkcollabsash"><h3>National Collaboration</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/sciencemap" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/map_of_science.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/sciencemap"><h3>Map of Science</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/collabmap" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/collaboration_map.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/collabmap"><h3>Collaboration Map</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/open_researcher_profiles.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href=""><h3>Open Researcher Profile &mdash; Prototype</h3></a>
      </div>
    </article>
    <!-- end .n_card -->

    <article class="n_card">
      <a href="${urls.base}/about" class="n_c-link-img">
        <img class="n_c-img" src="${urls.theme}/images/about_nora.png" alt="">
      </a>
      <div class="n_c-content">
        <a class="n_c-linkTitle" href="${urls.base}/about"><h3>About NORA</h3></a>
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
