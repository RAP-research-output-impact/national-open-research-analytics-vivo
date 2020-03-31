<#assign patent = patentDetails[0]>
<div class="publication-short-view" style="margin-bottom:2ex;"> 
    <div class="publication-short-view-details" style="margin-left:32px;">
    <h5 style="padding: 0 0 0 0;">
    <#if patent.p??>
    <a href="${individual.profileUrl}"">
    </#if>
    <#if patent.title??>
      ${patent.title!}
    </#if>
    <#if patent.p??>
      </a>&nbsp;
    </#if>
    </h6>
    <p style="font-size:0.8em;margin-bottom:0;">
    <#if patent.status??>
       ${patent.status}, 
    </#if>
    ${patent.typeLabel!}
    </p>
    </div>
</div>
