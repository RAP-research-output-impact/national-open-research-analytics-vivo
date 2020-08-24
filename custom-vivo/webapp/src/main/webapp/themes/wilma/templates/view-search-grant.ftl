<#-- TODO: figure out why the test for grantDetails is necessary here .  Suggests that this template is being applied to something that is not a grant .-->
<#if grantDetails?has_content>
<#assign grant = grantDetails[0]>
<div class="publication-short-view" style="margin-bottom:2ex;"> 
    <div class="publication-short-view-details" style="margin-left:32px;">
    <h4 style="padding: 0 0 0 0;">
    <#if grant.p??>
    <a href="${individual.profileUrl}"">
    </#if>
    <#if grant.title??>
      ${grant.title!}
    </#if>
    <#if grant.p??>
      </a>&nbsp;
    </#if>
    </h4>
    <p>
    <#if grant.startYear??>
       ${grant.startYear}-
    </#if>
    <#if grant.endYear??>
       ${grant.endYear},
    </#if>
    <#if grant.funder??>
        <#assign funderOrg = "">
        <#list grantDetails as grantDetail>
          <#if grantDetail.funder?has_content && grantDetail.funderOrg != funderOrg>${grantDetail.funder},
	    <#assign funderOrg = grantDetail.funderOrg>
	  </#if>
	</#list>
    </#if>
    ${grant.typeLabel!}
    </p>
    </div>
</div>
</#if>
