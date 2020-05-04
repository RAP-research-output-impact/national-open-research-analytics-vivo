<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Custom object property statement view for faux property "authors". See the PropertyConfig.n3 file for details.

     This template must be self-contained and not rely on other variables set for the individual page, because it
     is also used to generate the property statement during a deletion.
 -->

<#import "lib-sequence.ftl" as s>
<#import "lib-meta-tags.ftl" as lmt>

<@showAuthorship statement />

<#-- Use a macro to keep variable assignments local; otherwise the values carry over to the
     next statement --->
<#macro showAuthorship statement>
    <div class="pub_author-name">
    <#if statement.author??>
        <a href="${urls.base}/search?searchMode=all&facet_contributor_ss=${statement.uri("author")}" title="${i18n().author_name}">${statement.authorName!}</a> 
	<#if statement.corresponding??>*</#if>
	<#if statement.rank?? && authorAffiliations??>
          <#list authorAffiliations as authorAffiliation>
            <#if authorAffiliation.ranks?has_content>
              <#list authorAffiliation.ranks?split(";") as rank>
                <#if rank == statement.rank>
	          (${authorAffiliation?index + 1})
		</#if>
	      </#list>
	    </#if>
	  </#list>
	</#if>
    <#else>
        <#-- This shouldn't happen, but we must provide for it -->
        <a href="${profileUrl(statement.uri("authorship"))}" title="${i18n().missing_author}">${i18n().missing_author}</a>
    </#if>
    </div>
</#macro>
