rootProject.name = "centsible"

include(
    "centsible-api",
    "centsible-jooq",
    "centsible-core",
    "centsible-rest",
    "centsible-proto",
    "centsible-export",
    "centsible-integrations:manual",
    "centsible-imports:core",
    "centsible-imports:csv",
    "centsible-imports:ofx",
)
