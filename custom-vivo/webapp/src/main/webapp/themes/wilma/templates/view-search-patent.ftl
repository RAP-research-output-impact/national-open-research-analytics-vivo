<#if patent?has_content>
<#assign patent = patentDetails[0]>
<div class="publication-short-view" style="margin-bottom:2ex;"> 
    <div class="publication-short-view-details" style="margin-left:32px;">
    <h4 style="padding: 0 0 0 0;">
    <#if patent.p??>
    <a href="${individual.profileUrl}"">
    </#if>
    <#if patent.title??>
      ${patent.title!}
    </#if>
    <#if patent.p??>
      </a>&nbsp;
    </#if>
    </h4>
    <p>
    <#if patent.status??>
       ${patent.status}, 
    </#if>
    ${patent.typeLabel!}
    </p>
    </div>
</div>
</#if>
