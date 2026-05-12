<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html lang="${(locale.currentLanguageTag)!"fr"}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <meta name="robots" content="noindex, nofollow">
    <title>${msg("loginTitle", (realm.displayName!""))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/logo.svg" type="image/svg+xml">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>
<body class="${bodyClass}">
<div class="auth-shell">

    <#if realm.internationalizationEnabled?? && realm.internationalizationEnabled && locale?? && locale.supported?? && locale.supported?size gt 1>
        <div class="locale-switcher">
            <select aria-label="${msg("languages")}"
                    onchange="window.location.href=this.value">
                <#list locale.supported as l>
                    <option value="${l.url}"<#if (locale.currentLanguageTag)?? && locale.currentLanguageTag == l.languageTag> selected</#if>>${l.label}</option>
                </#list>
            </select>
        </div>
    </#if>

    <main class="auth-card" role="main">
        <div class="brand">
            <span class="brand-logo" aria-hidden="true">T</span>
            <span class="brand-name">Takalo</span>
        </div>

        <#-- Header (title) -->
        <#nested "header">

        <#-- Form / body -->
        <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="alert alert-${message.type}" role="alert" aria-live="polite">
                <svg class="alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <#if message.type = 'success'><polyline points="20 6 9 17 4 12"/>
                    <#elseif message.type = 'warning'><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
                    <#elseif message.type = 'error'><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                    <#else><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
                    </#if>
                </svg>
                <span class="alert-text">${kcSanitize(message.summary)?no_esc}</span>
            </div>
        </#if>

        <#nested "form">

        <#if auth?has_content && auth.showTryAnotherWayLink() && showAnotherWayIfPresent>
            <form action="${url.loginAction}" method="post" id="kc-select-try-another-way-form">
                <input type="hidden" name="tryAnotherWay" value="on"/>
                <p class="try-another-way">
                    <a href="#" id="try-another-way"
                       onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">
                        ${msg("doTryAnotherWay")}
                    </a>
                </p>
            </form>
        </#if>

        <#nested "socialProviders">

        <#if displayInfo>
            <div class="legal">
                <#nested "info">
            </div>
        </#if>
    </main>
</div>

<script>
    // Password reveal toggles
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-password-toggle]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var input = document.getElementById(btn.getAttribute('data-password-toggle'));
                if (!input) return;
                var isPwd = input.type === 'password';
                input.type = isPwd ? 'text' : 'password';
                btn.setAttribute('aria-label', isPwd ? '${msg("hidePassword")!"Masquer"}' : '${msg("showPassword")!"Afficher"}');
                btn.querySelector('.eye-open').style.display = isPwd ? 'none' : '';
                btn.querySelector('.eye-closed').style.display = isPwd ? '' : 'none';
            });
        });
    });
</script>
</body>
</html>
</#macro>
