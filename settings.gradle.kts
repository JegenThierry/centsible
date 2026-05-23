rootProject.name = "centsible"

include(
    "centsible-api",
    "centsible-jooq",
    "centsible-core",
    "centsible-rest",
    "centsible-proto",
    "centsible-export",
    "centsible-integrations:support",
    "centsible-integrations:manual",
    "centsible-integrations:paypal",
    "centsible-integrations:banking-gocardless",
    "centsible-imports:core",
    "centsible-imports:csv",
    "centsible-imports:ofx",
)
