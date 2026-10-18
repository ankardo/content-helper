# Security

## Posture

This tool is local-first and holds no secrets. It sends nothing to a
third party: the model runs on a local server reached at
`http://localhost:1234/v1` with a dummy API key. There are no credentials
to leak and no data egress beyond the machine.

The one outward-facing action is publishing a post into the blog
repository, and that is gated behind an explicit `--go` on the `approve`
command (see `docs/adr/0003-approval-gate.md`). It cannot happen by
accident.

## Reporting a vulnerability

Please do not open a public issue for a security problem. Use GitHub's
private vulnerability reporting on the repository: the Security tab, then
"Report a vulnerability". That keeps the report private until it is fixed.

Include what you found, how to reproduce it, and the impact you expect.
This is a personal project with no support commitment, but reports are
welcome and will be looked at.

## Dependencies

Dependencies are checked two ways: Dependabot watches for updates, and a
scheduled OSV-Scanner run reports known vulnerabilities in the SBOM
(`.github/workflows/osv-scan.yml`). Both are advisory; see
`docs/adr/0006-vulnerability-scan.md`.
