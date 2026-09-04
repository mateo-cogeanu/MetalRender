# Repository workflow

These instructions apply to the entire repository.

For every completed source, native-code, resource, build, or configuration
change set:

1. Update `CHANGELOG.md` with a concise user-facing description of the change.
2. Bump `mod_version` in `gradle.properties`. Use a patch bump by default;
   use a minor or major bump when the scope warrants it or the user requests it.
3. Keep all displayed and packaged versions derived from that canonical
   `mod_version`. Do not change `minVersion` in `metalrender.mixins.json`; that
   field describes the required Mixin subsystem version, not the mod version.
4. Run the most relevant verification. At minimum, run `./gradlew build` for
   code or packaging changes. Recompile the native library first when its
   Objective-C++ or Metal sources change.
5. Review the diff and do not commit generated caches, run directories, logs,
   secrets, `.DS_Store`, or unrelated user changes.
6. Commit the completed change set with a descriptive Git commit message.
7. Use the Git CLI, not a skill, to push the commit to
   `https://github.com/mateo-cogeanu/MetalRender.git`. Fetch and reconcile
   remote history non-destructively before pushing. Never force-push unless the
   user explicitly requests it. If authentication, networking, or a remote
   conflict blocks the push, report the exact blocker.

Treat a completed change set as one coherent task, rather than bumping the
version and pushing after every intermediate file edit.
