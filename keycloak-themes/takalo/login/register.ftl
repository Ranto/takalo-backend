<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm') displayRequiredFields=false; section>
    <#if section = "header">
        <p class="realm-line">${msg("registerTitle")}</p>
        <h1>${msg("registerTitle")}</h1>
        <p class="subtitle">${msg("registerIntro")!"Créez votre compte Takalo pour gérer vos budgets en quelques clics."}</p>

    <#elseif section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post" novalidate>

            <div class="form-row">
                <label for="firstName">${msg("firstName")}</label>
                <input type="text" id="firstName" class="input <#if messagesPerField.existsError('firstName')>error</#if>"
                       name="firstName" value="${(register.formData.firstName!'')}"
                       autocomplete="given-name"
                       aria-invalid="<#if messagesPerField.existsError('firstName')>true</#if>"/>
                <#if messagesPerField.existsError('firstName')>
                    <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('firstName'))?no_esc}</span>
                </#if>
            </div>

            <div class="form-row">
                <label for="lastName">${msg("lastName")}</label>
                <input type="text" id="lastName" class="input <#if messagesPerField.existsError('lastName')>error</#if>"
                       name="lastName" value="${(register.formData.lastName!'')}"
                       autocomplete="family-name"
                       aria-invalid="<#if messagesPerField.existsError('lastName')>true</#if>"/>
                <#if messagesPerField.existsError('lastName')>
                    <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('lastName'))?no_esc}</span>
                </#if>
            </div>

            <div class="form-row">
                <label for="email">${msg("email")}</label>
                <input type="email" id="email" class="input <#if messagesPerField.existsError('email')>error</#if>"
                       name="email" value="${(register.formData.email!'')}"
                       autocomplete="email"
                       placeholder="${msg("emailPlaceholder")!"vous@exemple.com"}"
                       aria-invalid="<#if messagesPerField.existsError('email')>true</#if>"/>
                <#if messagesPerField.existsError('email')>
                    <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('email'))?no_esc}</span>
                </#if>
            </div>

            <#if !realm.registrationEmailAsUsername>
                <div class="form-row">
                    <label for="username">${msg("username")}</label>
                    <input type="text" id="username" class="input <#if messagesPerField.existsError('username')>error</#if>"
                           name="username" value="${(register.formData.username!'')}"
                           autocomplete="username"
                           aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
                    <#if messagesPerField.existsError('username')>
                        <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
                    </#if>
                </div>
            </#if>

            <#if passwordRequired??>
                <div class="form-row">
                    <label for="password">${msg("password")}</label>
                    <div class="password-wrap">
                        <input type="password" id="password" class="input <#if messagesPerField.existsError('password','password-confirm')>error</#if>"
                               name="password" autocomplete="new-password"
                               aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"/>
                        <button type="button" class="password-toggle" data-password-toggle="password"
                                aria-label="${msg("showPassword")!"Afficher le mot de passe"}">
                            <svg class="eye-open" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                            </svg>
                            <svg class="eye-closed" style="display:none" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                                <line x1="1" y1="1" x2="23" y2="23"/>
                            </svg>
                        </button>
                    </div>
                    <#if messagesPerField.existsError('password')>
                        <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
                    </#if>
                </div>

                <div class="form-row">
                    <label for="password-confirm">${msg("passwordConfirm")}</label>
                    <input type="password" id="password-confirm" class="input <#if messagesPerField.existsError('password-confirm')>error</#if>"
                           name="password-confirm" autocomplete="new-password"
                           aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"/>
                    <#if messagesPerField.existsError('password-confirm')>
                        <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</span>
                    </#if>
                </div>
            </#if>

            <#if recaptchaRequired??>
                <div class="form-row">
                    <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                </div>
            </#if>

            <button class="btn btn-primary" type="submit">${msg("doRegister")}</button>
            <p class="legal">
                <a class="link-muted" href="${url.loginUrl}">${msg("backToLogin")?no_esc}</a>
            </p>
        </form>
    </#if>
</@layout.registrationLayout>
