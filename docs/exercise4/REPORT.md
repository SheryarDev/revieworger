# Exercise 4 — Metrics with SCC

Tool: SCC 4.1.0 (`brew install scc`; Linux arm64 binary in the Jenkins container).

## SCC options (identical for every measurement)

```
--include-ext java --exclude-dir build,dist,lib,lib-test,build-test,reports,tools,spotbugs,docs
```

Defined once as `SCC_OPTS` in the `Jenkinsfile`. Generated output and third-party material
(reports, PIT, JaCoCo, SpotBugs, jars) are excluded so only our own Java source is measured.
Without the filter, `scc .` reports 729 files / 216,696 lines, mostly generated HTML reports.

## Task 1 — Initial local result

| Metric | Initial result |
|---|---|
| Files | 44 |
| Lines | 7,791 |
| Code | 5,276 |
| Comments | 1,159 |
| Blanks | 1,356 |
| Complexity | 531 |
| Bytes | 221,818 |

Cost/years (COCOMO estimate, not real): ~$154,903, 6.77 months, 2.03 people.
Raw output: `scc-0-initial.txt`, `scc-0-initial.json`. Take the terminal screenshot with
`scc --include-ext java --exclude-dir build,dist,lib,lib-test,build-test,reports,tools,spotbugs,docs .`

## Task 2 — Jenkins integration

- SCC installed in container `jenkins` (Linux / aarch64) at `/usr/local/bin/scc`.
  Running it in `/var/jenkins_home/workspace/reviewOrger` gave the same numbers as locally.
  The install is manual: recreating the container loses it (add it to a custom Jenkins image for permanence).
- `Jenkinsfile`: new stage **Code Metrics - SCC** after Build writes
  `reports/scc/scc-report.txt`, `scc-report.json`, `scc-by-file.txt`.
  `reports/scc/**` was added to `REPORT_ARTIFACTS`, so the existing *Archive Results* stage archives them.

### Experiments

Measured with the exact SCC options above on a copy of `src/` + `test/`, adding
`src/logic/MetricsExperiment.java` step by step (reports in this folder). The Jenkins stage runs
the identical command on the checked-out workspace, so a build after each commit should show these
values; confirm against the archived `scc-report.txt` of each build.

| Metric | Initial | After new file (A) | After conditions (B) | After comments (C) |
|---|---|---|---|---|
| Files | 44 | 45 | 45 | 45 |
| Lines | 7,791 | 7,799 | 7,808 | 7,811 |
| Code | 5,276 | 5,282 | 5,290 | 5,290 |
| Comments | 1,159 | 1,159 | 1,159 | 1,162 |
| Blanks | 1,356 | 1,358 | 1,359 | 1,359 |
| Complexity | 531 | 531 | 537 | 537 |
| Bytes | 221,818 | 221,933 | 222,129 | 222,285 |

Observations:
- A: +1 file, +8 lines (6 code, 2 blank). Complexity unchanged: `add` has no branches.
- B: complexity +6 and code +8. SCC counts branching tokens such as `if`, `else if` and `else`.
- C: +3 comment lines. Code and complexity are unchanged, so comments do not affect complexity.
- Not every metric moves in every experiment; each change moves the metrics it should.

## Not done / needs the user

- No commits or `git push` were made, so there are no real Jenkins builds of A/B/C yet. Run the pipeline to confirm.
- Terminal screenshot (Task 1 step 7) must be taken by you.
- Bonus (SonarQube) not implemented.
