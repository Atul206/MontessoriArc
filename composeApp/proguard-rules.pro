# Task 9: R8 shrink/obfuscate keep rules.
#
# This app has no reflection-sensitive surface left to protect:
# - Navigation (Task 5) is a hand-rolled `Route` sealed interface with no
#   `kotlinx.serialization` involved (see Route.kt's own doc comment) — the
#   brief's original concern about `@Serializable` NavKey routes is moot.
# - Template content (Task 2) is hand-authored Kotlin (RegionSpec/Template
#   data classes built directly in code), not generated `svg-to-compose`
#   output consumed via a reflective DSL — that concern is moot too.
#
# No keep rules are added here speculatively; see task-9-report.md for what
# the R8 analysis actually found.
