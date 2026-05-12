<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "header">
        <p class="realm-line">${msg("updatePasswordTitle")}</p>
        <h1>${msg("updatePasswordTitle")}</h1>
        <p class="subtitle">${msg("updatePasswordIntro")!"Choisissez un nouveau mot de passe pour sécuriser votre compte."}</p>

    <#elseif section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post" novalidate>
            <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly style="display:none"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none"/>

            <div class="form-row">
                <label for="password-new">${msg("passwordNew")}</label>
                <div class="password-wrap">
                    <input type="password" id="password-new" name="password-new"
                           class="input <#if messagesPerField.existsError('password')>error</#if>"
                           autofocus autocomplete="new-password"
                           aria-invalid="<#if messagesPerField.existsError('password')>true</#if>"/>
                    <button type="button" class="password-toggle" data-password-toggle="password-new" aria-label="${msg("showPassword")!"Afficher"}">
                        <svg class="eye-open" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                        <svg class="eye-closed" style="display:none" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    </button>
                </div>
                <#if messagesPerField.existsError('password')>
                    <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
                </#if>
            </div>

            <div class="form-row">
                <label for="password-confirm">${msg("passwordConfirm")}</label>
                <input type="password" id="password-confirm" name="password-confirm"
                       class="input <#if messagesPerField.existsError('password-confirm')>error</#if>"
                       autocomplete="new-password"
                       aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"/>
                <#if messagesPerField.existsError('password-confirm')>
                    <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</span>
                </#if>
            </div>

            <#if isAppInitiatedAction??>
                <div class="form-inline">
                    <label class="checkbox">
                        <input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked>
                        <span>${msg("logoutOtherSessions")}</span>
                    </label>
                </div>
            </#if>

            <button class="btn btn-primary" type="submit">${msg("doSubmit")}</button>

            <#if isAppInitiatedAction??>
                <button class="btn btn-ghost" type="submit" name="cancel-aia" value="true">${msg("doCancel")}</button>
            </#if>
        </form>
    </#if>
</@layout.registrationLayout>
