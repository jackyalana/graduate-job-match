# Third-party notices and provenance

## Imported application

Lan-Recruitment, https://github.com/lanseqingling/lan-recruitment

Reference commit: `cd755d13443ea4b4c98be86e6c692238ed566d31`.

The `backend/`, `frontend/`, original `scripts/dev.*`, original Chinese docs, README, images, AGENTS.md and LICENSE originate from that repository. Git history and original MIT license/copyright are retained. No code was copied from however-yir/campus-recruitment-recommendation-system or RuoYi. References in the preparation package are research provenance, not imported implementation.

M0 additions: explicit `application-demo.yml`; `scripts/m0/` local tooling, synthetic account bootstrap and real HTTP probes; baseline/reuse/API/ADR reports and evidence. Existing business Java, tests, frontend source, dependency manifests and lockfile remain unchanged at M0. Root README only adds a local M0 handoff pointer. AI-assisted implementation and verification were used; all original business work must be attributed to Lan-Recruitment. This is not a claim of wholly original software or of an independently invented cosine algorithm.

Upstream sample account strings and SQL fixture examples are upstream demonstration defaults, not user credentials, and were not used as the local demo seed. New random credentials, private DB, runtime tools, files and tokens are excluded from Git. The upstream sample people/company/avatar assets have not been independently licensed as personal/media content; the running M0 uses freshly synthesized labels, no external avatars, and does not reuse them as thesis evidence. Before broader distribution, review images, fonts, upstream fixture provenance and all transitive dependencies individually.

### Original license (also preserved verbatim in LICENSE)

```text
MIT License

Copyright (c) 2026 蓝色蜻蛉

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Principal dependencies

Versions identify the actual M0 POM/lockfile, not an upgrade recommendation. npm license metadata and Maven POM license declarations are evidence of declarations, not a completed transitive compliance audit.

| Package | Version | Declared license / verification boundary |
| --- | --- | --- |
| Spring Boot | 2.7.18 | Apache-2.0 (resolved Maven metadata) |
| MyBatis-Plus | 3.5.6 | Apache-2.0 (resolved POM) |
| Sa-Token | 1.38.0 | Apache-2.0 (sa-token-core resolved POM) |
| MySQL Connector/J | 8.0.33 | GPL v2 with Universal FOSS Exception v1.0 (resolved POM); separate from application MIT |
| Hutool | 5.8.27 | License inherited / detailed dependency notice review pending |
| Lombok | 1.18.38 | Detailed distribution notice review pending |
| vue | 3.5.27 | MIT (installed package metadata) |
| vue-router | 4.6.4 | MIT (installed package metadata) |
| vite | 5.4.21 | MIT (installed package metadata) |
| @vitejs/plugin-vue | 5.2.4 | MIT (installed package metadata) |
| element-plus | 2.13.2 | MIT (installed package metadata) |
| @element-plus/icons-vue | 2.3.2 | MIT (installed package metadata) |
| axios | 1.13.4 | MIT (installed package metadata) |

M0 private tools are not checked into the application repository: Azul Zulu OpenJDK (see its bundled LICENSE/ASSEMBLY_EXCEPTION), Apache Maven (bundled LICENSE/NOTICE), MySQL Community Server (bundled LICENSE). They retain their own terms; the application MIT license does not relicense these binaries. No claim is made that the complete bundled/transitive license inventory is finished. M4 distribution must preserve applicable dependency notices and produce an inventory appropriate to the delivered artifact.
