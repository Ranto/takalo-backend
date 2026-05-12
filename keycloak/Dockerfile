# Image Keycloak custom Takalo : Keycloak officiel + thème "takalo" embarqué.
#
# Contexte de build : backend/ (pour pouvoir COPY keycloak-themes/takalo).
# Construit via scripts/build-push-keycloak.sh.

FROM quay.io/keycloak/keycloak:26.0 AS builder

# Settings affectant la phase build : doivent être fixés avant kc.sh build
# pour que `start --optimized` au runtime ne réclame pas une nouvelle augmentation.
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

# Thème custom (login)
COPY keycloak-themes/takalo /opt/keycloak/themes/takalo

# Précompile la distribution (caches Quarkus, providers, themes index)
RUN /opt/keycloak/bin/kc.sh build

# ---------------------------------------------------------------------------

FROM quay.io/keycloak/keycloak:26.0

COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Le hostname, les credentials DB et les credentials admin restent injectés
# au runtime via docker-compose.prod.yml (envs KC_*).
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
