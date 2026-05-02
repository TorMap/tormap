When reviewing pull requests from branches starting with `renovate/`:

1. Identify the packages being updated and the exact version ranges (from → to).
2. Read the changelog/release notes included in the PR description by Renovate.
3. Assess whether any breaking changes affect this specific codebase by examining:
    - Direct usages of the updated package's public API in the source code
    - Configuration files that reference the package
    - Any deprecated methods/classes that are now removed
    - For Java/Kotlin dependencies, check if Spring Boot compatibility is affected
4. If you determine that there are breaking changes, evaluate the impact on the codebase:
    - If the breaking change can be easily fixed, implement the necessary code changes in the PR and comment on the fix.
    - If the breaking change is significant and requires a major refactor, comment only a high-level plan for a fix.
5. Conclude with one of: SAFE TO MERGE / BREAKING CHANGE FIXED / NEEDS MANUAL REVIEW
