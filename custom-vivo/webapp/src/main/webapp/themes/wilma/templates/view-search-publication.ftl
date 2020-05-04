<#assign publication = publication[0]>
<div class="publication-short-view" style="margin-bottom:2ex;"> 
    <div class="open-access" style="height:50px;width:32px;float:left;">
        <#if publication.openAccess??>
            <img src="${urls.theme}/images/open-access.svg" width="20" alt="open access publication"/>
        </#if>
    </div>
    <div class="publication-short-view-details" style="margin-left:32px;">
    <h4 style="padding: 0 0 0 0;">
    <#if publication.p??>
    <a href="${individual.profileUrl}"">
    </#if>
    <#if publication.title??>
      ${publication.title!}
    </#if>
    <#if publication.p??>
      </a>&nbsp;
    </#if>
    </h4>
    <#if publication.authorList??>
       <p>
       <#assign count = 0>
       <#assign authors = publication.authorList?split(";")>
       <#assign authorLength = authors?size>
       <#list authors as author>
           <#assign count = count + 1>
	   <#if (count == 4) && (authorLength gt 4) >
              [et al.]
	   <#elseif count == 1>${author}<#elseif count lt 5>, ${author}</#if>
       </#list>
       </p>
    </#if>
    <p style="font-size:0.8em;margin-bottom:0;">
    <#if publication.year??>
       ${publication.year},
    </#if>
    <#if publication.journal??>
        ${publication.journal},
    </#if>
    ${publication.typeLabel!}
    </p>
    </div>
</div>
