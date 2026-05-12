<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true displayInfo=true; section>
    <#if section = "header">
        <p class="realm-line">${msg("emailVerifyTitle")}</p>
        <h1>${msg("emailVerifyTitle")}</h1>
        <p class="subtitle">${msg("emailVerifyInstruction1",user.email!"")}</p>

    <#elseif section = "form">
        <p class="kc-info-message">${msg("emailVerifyInstruction2")}</p>
        <p class="kc-info-message"><a class="link-muted" href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailVerifyInstruction3")}</p>
    </#if>
</@layout.registrationLayout>
