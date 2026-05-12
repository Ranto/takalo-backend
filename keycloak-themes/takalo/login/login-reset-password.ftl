<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=true; section>
    <#if section = "header">
        <p class="realm-line">${msg("emailForgotTitle")!"Réinitialisation"}</p>
        <h1>${msg("emailForgotTitle")}</h1>
        <p class="subtitle">${msg("emailInstruction")}</p>

    <#elseif section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post" novalidate>
            <div class="form-row">
                <label for="username">
                    <#if !realm.loginWithEmailAllowed>${msg("username")}
                    <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                    <#else>${msg("email")}
                    </#if>
                </label>
                <input type="text" id="username" name="username"
                       class="input <#if messagesPerField.existsError('username')>error</#if>"
                       autofocus
                       value="${(auth.attemptedUsername!'')}"
                       autocomplete="username"
                       placeholder="${msg("emailPlaceholder")!"vous@exemple.com"}"
                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
                <#if messagesPerField.existsError('username')>
                    <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
                </#if>
            </div>

            <button class="btn btn-primary" type="submit">${msg("doSubmit")}</button>
            <p class="legal">
                <a class="link-muted" href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a>
            </p>
        </form>

    <#elseif section = "info">
        <#if realm.duplicateEmailsAllowed>
            ${msg("emailInstructionUsername")}
        <#else>
            ${msg("emailInstruction")}
        </#if>
    </#if>
</@layout.registrationLayout>
