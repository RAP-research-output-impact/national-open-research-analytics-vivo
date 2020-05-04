<#-- $This file is distributed under the terms of the license in LICENSE$ -->

</header>

<#include "developer.ftl">

<nav role="navigation">
    <ul id="main-nav" role="list">
        <#list menu.items as item>
	    <#if item.linkText == "Search Page Placeholder">
              <li role="listitem"><a href="${urls.base}/search?searchMode=all" title="search ${i18n().menu_item}" <#if searchMode?has_content> class="selected" </#if>>Search</a></li>
	    <#else>
              <li role="listitem"><a href="${item.url}" title="${item.linkText} ${i18n().menu_item}" <#if item.active> class="selected" </#if>>${item.linkText}</a></li>
	    </#if>
        </#list>
    </ul>
</nav>

<div id="wrapper-content" role="main">
    <#if flash?has_content>
        <#if flash?starts_with(i18n().menu_welcomestart) >
            <section  id="welcome-msg-container" role="container">
                <section  id="welcome-message" role="alert">${flash}</section>
            </section>
        <#else>
            <section  id="flash-msg-container" role="container">
                <section id="flash-message" role="alert">${flash}</section>
            </section>
        </#if>
    </#if>

    <!--[if lte IE 8]>
    <noscript>
        <p class="ie-alert">This site uses HTML elements that are not recognized by Internet Explorer 8 and below in the absence of JavaScript. As a result, the site will not be rendered appropriately. To correct this, please either enable JavaScript, upgrade to Internet Explorer 9, or use another browser. Here are the <a href="http://www.enable-javascript.com"  title="java script instructions">instructions for enabling JavaScript in your web browser</a>.</p>
    </noscript>
    <![endif]-->
