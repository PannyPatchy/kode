# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `kode doctor`: check the environment (`.kode.json` validity, Kotlin LSP
  resolution, Java runtime) and report as JSON; `--install-lsp` downloads the
  pinned JetBrains kotlin-lsp build into `~/.kode/lsp/` when none resolves
- `kode mcp`: run kode as an MCP server over stdio, exposing the analysis
  commands as tools (`kode_tree`, `kode_errors`, `kode_symbols`, `kode_refs`,
  `kode_test`) for AI agents such as Claude Code
- The LSP binary resolution now falls back to the kode-managed install
  (`~/.kode/lsp/`) after `KODE_LSP_PATH`, `.kode.json`, and `PATH`
- `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1) and this changelog

### Changed

- Documentation is now English-first with Japanese siblings (`README.ja.md`,
  `CONTRIBUTING.ja.md`)
- The automated release PR uses an English title ("Production release") and body
- `SECURITY.md` now directs vulnerability reports to GitHub private
  vulnerability reporting instead of public issues

### Planned

- Prebuilt native binaries (macOS/Linux) published via GitHub Releases
