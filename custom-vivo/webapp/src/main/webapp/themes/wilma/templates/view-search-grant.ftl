<#assign grant = grantDetails[0]>
<div class="publication-short-view" style="margin-bottom:2ex;"> 
    <div class="publication-short-view-details" style="margin-left:32px;">
    <h5 style="padding: 0 0 0 0;">
    <#if grant.p??>
    <a href="${individual.profileUrl}"">
    </#if>
    <#if grant.title??>
      ${grant.title!}
    </#if>
    <#if grant.p??>
      </a>&nbsp;
    </#if>
    </h6>
    <p style="font-size:0.8em;margin-bottom:0;">
    <#if grant.startYear??>
       ${grant.startYear}-
    </#if>
    <#if grant.endYear??>
       ${grant.endYear},
    </#if>
    <#if grant.funder??>
        <#assign funder = ""»
        <#list grantDetails as grantDetail>
          <#if grantDetail.funder?has_content && grantDetail.funder != funder>${grantDetail.funder},
	    <#assign funder = grantDetail.funder>
	  </#if>
	</#list>
    </#if>
    ${grant.typeLabel!}
    </p>
    </div>
</div>
