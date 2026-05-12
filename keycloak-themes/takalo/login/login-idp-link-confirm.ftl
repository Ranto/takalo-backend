<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <p class="realm-line">${msg("confirmLinkIdpTitle")}</p>
        <h1>${msg("confirmLinkIdpTitle")}</h1>
        <p class="subtitle">${msg("federatedIdentityConfirmLinkMessage",idpDisplayName,idpAlias)}</p>

    <#elseif section = "form">
        <form id="kc-register-form" action="${url.loginAction}" method="post">
            <button class="btn btn-ghost" type="submit" name="submitAction" id="updateProfile" value="updateProfile">
                ${msg("confirmLinkIdpReviewProfile")}
            </button>
            <button class="btn btn-primary" type="submit" name="submitAction" id="linkAccount" value="linkAccount">
                ${msg("confirmLinkIdpContinue", idpDisplayName)}
            </button>
        </form>
    </#if>
</@layout.registrationLayout>
