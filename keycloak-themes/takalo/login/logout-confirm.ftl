<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password'); section>
    <#if section = "header">
        <p class="realm-line">${msg("logoutConfirmTitle")!"Déconnexion"}</p>
        <h1>${msg("logoutConfirmTitle")}</h1>
        <p class="subtitle">${msg("logoutConfirmHeader")}</p>

    <#elseif section = "form">
        <form action="${url.logoutConfirmAction}" method="POST">
            <input type="hidden" name="session_code" value="${logoutConfirm.code}">
            <button class="btn btn-primary" type="submit" id="kc-logout">${msg("doLogout")}</button>
            <#if client?? && client.baseUrl?has_content>
                <a class="btn btn-ghost" href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a>
            </#if>
        </form>
    </#if>
</@layout.registrationLayout>
