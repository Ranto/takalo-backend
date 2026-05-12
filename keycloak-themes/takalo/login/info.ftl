<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            <h1>${messageHeader}</h1>
        <#else>
            <h1>${message.summary?no_esc}</h1>
        </#if>

    <#elseif section = "form">
        <div class="alert alert-${message.type!'info'}" role="status" aria-live="polite">
            <svg class="alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            <span class="alert-text">
                ${message.summary?no_esc}
                <#if requiredActions??>
                    <#list requiredActions>: <#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></#list>
                </#if>
            </span>
        </div>

        <#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <p class="legal"><a class="btn btn-ghost" href="${pageRedirectUri}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
            <#elseif actionUri?has_content>
                <p class="legal"><a class="btn btn-primary" href="${actionUri}">${kcSanitize(msg("proceedWithAction"))?no_esc}</a></p>
            <#elseif client?? && client.baseUrl?has_content>
                <p class="legal"><a class="btn btn-ghost" href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
            </#if>
        </#if>
    </#if>
</@layout.registrationLayout>
