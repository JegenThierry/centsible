package beer.thierry.centsible.api.model.setup

/** Whether the instance still needs its first-run setup (no users created yet). */
data class SetupStatusResponse(val needsSetup: Boolean)
