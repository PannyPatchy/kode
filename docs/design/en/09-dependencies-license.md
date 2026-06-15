# 09. Dependencies & License Review

> Translation: [日本語](../ja/09-dependencies-license.md) ／ [Back to index](../README.md)

This repository will be published as **public OSS**, so every dependency is restricted to ones that are **fine to publish and distribute as OSS**. This chapter reviews each dependency's license and records the rationale for adoption and alternatives considered.

> As of: 2026-06. Re-check each project's latest license as needed (the Kotlin LSP especially changes fast while in Alpha).

## 1. Summary

| Purpose | Choice | SPDX | Publishable | Rationale |
|---------|--------|------|-------------|-----------|
| CLI | **Clikt** | `Apache-2.0` | ✅ | Reflection-free, great native-image fit |
| JSON | **kotlinx.serialization** | `Apache-2.0` | ✅ | Compile-time generation, native-image ✓ |
| Gradle integration | **Gradle Tooling API** | `Apache-2.0` | ✅ | Official, stable, bundlable |
| LSP client | **Eclipse LSP4J** | `EPL-2.0 OR EDL-1.0` | ✅ | Permissive via EDL (BSD-3) |
| Kotlin LSP server | **JetBrains Kotlin LSP** | `Apache-2.0` (**partially closed**) | ⚠️ **not bundled** | Proprietary parts + Alpha. Used as external process |
| Native Image tool | **GraalVM CE** | `GPLv2 WITH Classpath-exception-2.0` | ✅ | Output binary does not propagate copyleft |
| Native Image plugin | **org.graalvm.buildtools.native** | `Apache-2.0` | ✅ | Build-time only |
| DI | **Pure DI (hand-written)** | — | ✅ | No dependency; native-image-safe |
| Test | **JUnit 5 / MockK** | `EPL-2.0` / `Apache-2.0` | ✅ | Run on JVM; not in the distributed artifact |

**Project license: Apache-2.0** (continuing the existing `/LICENSE`).

## 2. Details per Dependency

### Clikt (Apache-2.0)
- A Kotlin-native CLI framework. No annotations/reflection dependence, so native-image needs minimal config.
- **Alternatives considered**:
  - *picocli* (Apache-2.0): Java, feature-rich, but annotation + reflection centric. Needs special handling on native-image and pushes type errors to runtime. → Not adopted.
  - *kotlinx-cli* (Apache-2.0): official but stalled and limited. → Not adopted.
- **Conclusion**: keep Clikt.

### kotlinx.serialization (Apache-2.0)
- Generates serializers at compile time via `@Serializable`. No reflection; ideal for native-image.
- **Alternatives considered**: Jackson/Gson (heavy reflection, more native-image config). → Not adopted.
- **Conclusion**: adopt.

### Gradle Tooling API (Apache-2.0)
- The official API for introspecting Gradle projects / fetching models. Can be bundled into the `kode` binary.
- Caveat: native-image compatibility (covered by the [08](08-native-image.md) §5 fallback).
- **Conclusion**: adopt.

### Eclipse LSP4J (EPL-2.0 OR EDL-1.0)
- A mature library providing LSP JSON-RPC framing/types. Fewer bugs than rolling our own.
- **Dual-licensed**: the recipient may choose **EDL-1.0 (Revised BSD-style, permissive)**. No issue for OSS distribution.
- **Alternatives considered**: implement JSON-RPC ourselves (fewer deps, but more maintenance and a bug magnet). → Not adopted in the first version.
- **Conclusion**: adopt (note in NOTICE that distribution selects EDL-1.0).

### JetBrains Kotlin LSP (Apache-2.0 but partially closed / Alpha) ⚠️
- The official `Kotlin/kotlin-lsp` repo is Apache-2.0, but its README states it is "**partially closed-source**, based on proprietary parts of IntelliJ IDEA / Fleet / Air." Stage: **Alpha**.
- Distribution: VS Code extension, standalone ZIP, Homebrew (`brew install JetBrains/utils/kotlin-lsp`).
- **Policy**: to avoid pulling closed parts and stability risk into `kode`'s artifact, **do not bundle/redistribute**. Launch a user-installed copy as an **external process** ([04](04-lsp.md) §1). This keeps the OSS distribution's licensing clean.

### GraalVM Native Image (CE: GPLv2 + Classpath Exception)
- Used **only as a build tool**. Under GraalVM's terms, native-image **output is treated as an "unmodified Program"**, so GPL does not propagate to the artifact.
- Hence the produced `kode` can be distributed under **Apache-2.0**.
- Oracle GraalVM (GFTC) is also free but has conditions on redistribution, so OSS builds standardize on **CE**.
- **Conclusion**: adopt CE.

### DI: Pure DI (hand-written Composition Root)
- No external DI framework; wire manually in `bootstrap` ([01](01-architecture.md) §4).
- Reasons: avoid reflection/dynamic proxies on native-image, fewer deps, explicit wiring.
- **Alternatives considered**: Koin (runtime resolution needs extra native-image work) / Dagger (Java/KAPT; native-compatible but overkill for small Clean-Architecture wiring). → Not adopted.

### Test: JUnit 5 (EPL-2.0) / MockK (Apache-2.0)
- Tests run on the JVM and are **not included in the distributed binary**, so a different license category does not affect the published artifact.
- Optionally combine with Kotest (Apache-2.0).

## 3. OSS Publication Checklist

- [ ] Every dependency **included in the distributed artifact** is permissive or non-propagating copyleft (table above).
- [ ] **Do not redistribute the JetBrains Kotlin LSP server** (stick to external-process usage).
- [ ] The Native Image artifact's license is consistent with Apache-2.0 (verify GraalVM CE's Classpath Exception).
- [ ] Record each dependency's copyright and license in `NOTICE` / `THIRD-PARTY-LICENSES` (note that LSP4J selects EDL-1.0).
- [ ] When bumping versions in `gradle/libs.versions.toml`, check for license changes (a CI license check is recommended).
- [ ] No sensitive data in `.kode.json`, etc. ([06](06-config-kode-json.md)).

## 4. References (primary sources)

- Kotlin LSP: `Kotlin/kotlin-lsp` (GitHub) / the Kotlin LSP docs on kotlinlang.org.
- Clikt: ajalt.github.io/clikt.
- Eclipse EDL-1.0: the Eclipse Distribution License on eclipse.org.
- GraalVM license: graalvm.org / oracle/graal LICENSE, the clause about Native Image output.

> Licenses can change. Integrate a **license scan** into CI (e.g. extract dependency SPDX and check against an allowlist) and re-verify before publication.
