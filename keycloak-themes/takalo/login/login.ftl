<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        <p class="realm-line">${msg("loginAccountTitle")}</p>
        <h1>${msg("doLogIn")}</h1>
        <p class="subtitle">${msg("loginIntro")!"Connectez-vous pour accéder à vos budgets et achats."}</p>

    <#elseif section = "form">
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate>
                <#if !usernameHidden??>
                    <div class="form-row">
                        <label for="username">
                            <#if !realm.loginWithEmailAllowed>${msg("username")}
                            <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                            <#else>${msg("email")}
                            </#if>
                        </label>
                        <input id="username"
                               class="input <#if messagesPerField.existsError('username','password')>error</#if>"
                               name="username"
                               value="${(login.username!'')}"
                               type="text"
                               autofocus
                               autocomplete="username"
                               aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                               placeholder="${msg("emailPlaceholder")!"vous@exemple.com"}"/>

                        <#if messagesPerField.existsError('username','password')>
                            <span id="input-error" class="field-error" aria-live="polite">
                                ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                        </#if>
                    </div>
                </#if>

                <div class="form-row">
                    <label for="password">${msg("password")}</label>
                    <div class="password-wrap">
                        <input id="password"
                               class="input <#if messagesPerField.existsError('username','password')>error</#if>"
                               name="password"
                               type="password"
                               autocomplete="current-password"
                               aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                               placeholder="••••••••"/>
                        <button type="button"
                                class="password-toggle"
                                data-password-toggle="password"
                                aria-label="${msg("showPassword")!"Afficher le mot de passe"}">
                            <svg class="eye-open" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                <circle cx="12" cy="12" r="3"/>
                            </svg>
                            <svg class="eye-closed" style="display:none" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                                <line x1="1" y1="1" x2="23" y2="23"/>
                            </svg>
                        </button>
                    </div>
                </div>

                <div class="form-inline">
                    <#if realm.rememberMe && !usernameHidden??>
                        <label class="checkbox">
                            <input id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>>
                            <span>${msg("rememberMe")}</span>
                        </label>
                    <#else>
                        <span></span>
                    </#if>
                    <#if realm.resetPasswordAllowed>
                        <a class="link-muted" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
                    </#if>
                </div>

                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

                <button class="btn btn-primary" name="login" id="kc-login" type="submit">
                    ${msg("doLogIn")}
                </button>
            </form>
        </#if>

    <#elseif section = "socialProviders">
        <#if realm.password && social.providers??>
            <div class="idp-divider"><span>${msg("identity-provider-login-label")}</span></div>
            <ul class="idp-list">
                <#list social.providers as p>
                    <li>
                        <a class="idp-btn" id="social-${p.alias}" href="${p.loginUrl}">
                            <span class="idp-icon">
                                <#if p.providerId?lower_case = "google" || p.alias?lower_case = "google">
                                    <svg viewBox="0 0 48 48" aria-hidden="true">
                                        <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3c-1.6 4.6-6 8-11.3 8-6.6 0-12-5.4-12-12s5.4-12 12-12c3 0 5.8 1.1 7.9 3l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z"/>
                                        <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.6 16 18.9 13 24 13c3 0 5.8 1.1 7.9 3l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/>
                                        <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.3l-6.2-5.2c-2 1.5-4.5 2.5-7.2 2.5-5.2 0-9.7-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z"/>
                                        <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.3 4.3-4.1 5.5l6.2 5.2C41.4 35.1 44 30 44 24c0-1.3-.1-2.4-.4-3.5z"/>
                                    </svg>
                                <#elseif p.providerId?lower_case = "microsoft" || p.alias?lower_case = "microsoft">
                                    <svg viewBox="0 0 24 24" aria-hidden="true">
                                        <rect x="2" y="2" width="9" height="9" fill="#F25022"/>
                                        <rect x="13" y="2" width="9" height="9" fill="#7FBA00"/>
                                        <rect x="2" y="13" width="9" height="9" fill="#00A4EF"/>
                                        <rect x="13" y="13" width="9" height="9" fill="#FFB900"/>
                                    </svg>
                                <#elseif p.providerId?lower_case = "github" || p.alias?lower_case = "github">
                                    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                                        <path d="M12 .5A11.5 11.5 0 0 0 .5 12a11.5 11.5 0 0 0 7.86 10.94c.58.1.79-.25.79-.56v-2c-3.2.7-3.87-1.36-3.87-1.36-.52-1.33-1.28-1.69-1.28-1.69-1.04-.71.08-.7.08-.7 1.15.08 1.76 1.18 1.76 1.18 1.03 1.76 2.7 1.25 3.36.96.1-.75.4-1.25.73-1.54-2.55-.29-5.24-1.28-5.24-5.69 0-1.26.45-2.28 1.18-3.09-.12-.29-.51-1.46.11-3.04 0 0 .97-.31 3.18 1.18a11 11 0 0 1 5.79 0c2.21-1.49 3.18-1.18 3.18-1.18.62 1.58.23 2.75.11 3.04.73.81 1.18 1.83 1.18 3.09 0 4.42-2.7 5.39-5.27 5.68.41.36.78 1.06.78 2.14v3.17c0 .31.21.67.8.56A11.5 11.5 0 0 0 23.5 12 11.5 11.5 0 0 0 12 .5z"/>
                                    </svg>
                                <#elseif p.providerId?lower_case = "apple" || p.alias?lower_case = "apple">
                                    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                                        <path d="M16.36 12.78c-.02-2.4 1.96-3.55 2.05-3.61-1.12-1.63-2.86-1.86-3.48-1.88-1.48-.15-2.89.87-3.64.87-.76 0-1.92-.85-3.16-.83-1.62.02-3.13.94-3.97 2.4-1.69 2.94-.43 7.28 1.22 9.66.8 1.16 1.76 2.47 3.02 2.42 1.22-.05 1.68-.78 3.15-.78 1.47 0 1.88.78 3.16.76 1.31-.02 2.13-1.18 2.93-2.36.92-1.35 1.31-2.65 1.33-2.72-.03-.01-2.54-.97-2.61-3.93zM13.93 5.4c.67-.81 1.13-1.94 1-3.06-.97.04-2.15.65-2.85 1.46-.63.71-1.18 1.85-1.03 2.95 1.08.08 2.19-.55 2.88-1.35z"/>
                                    </svg>
                                <#elseif p.providerId?lower_case = "facebook" || p.alias?lower_case = "facebook">
                                    <svg viewBox="0 0 24 24" aria-hidden="true">
                                        <path fill="#1877F2" d="M24 12a12 12 0 1 0-13.88 11.85V15.5H7.08V12h3.04V9.36c0-3 1.79-4.67 4.53-4.67 1.31 0 2.69.23 2.69.23v2.96h-1.52c-1.49 0-1.96.93-1.96 1.88V12h3.33l-.53 3.5h-2.8v8.35A12 12 0 0 0 24 12z"/>
                                    </svg>
                                <#else>
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <circle cx="12" cy="12" r="10"/>
                                        <path d="M12 8v8M8 12h8"/>
                                    </svg>
                                </#if>
                            </span>
                            <span>${p.displayName!p.alias}</span>
                        </a>
                    </li>
                </#list>
            </ul>
        </#if>

    <#elseif section = "info">
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            ${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a>
        </#if>
    </#if>
</@layout.registrationLayout>
