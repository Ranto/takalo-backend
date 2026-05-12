<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <p class="realm-line">${msg("errorTitle")!"Erreur"}</p>
        <h1>${msg("errorTitleHtml")?no_esc}</h1>

    <#elseif section = "form">
        <div class="alert alert-error" role="alert">
            <svg class="alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span class="alert-text">${kcSanitize(message.summary)?no_esc}</span>
        </div>

        <#if skipLink??>
        <#else>
            <#if client?? && client.baseUrl?has_content>
                <p class="legal">
                    <a class="btn btn-ghost" href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a>
                </p>
            </#if>
        </#if>
    </#if>
</@layout.registrationLayout>
