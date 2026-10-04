# P8 — Patent / Public-Disclosure Audit for HazRob Master Deck

Status: COMPLETE — master build screened for disclosure posture
Tracking issue: #9
Branch: `docs/hazrob-master-deck`

> This is an engineering disclosure audit, not legal advice, a novelty search, a freedom-to-operate opinion, or a patentability conclusion.

## Audit objective
Keep the deck persuasive for patent/invention discussions, conferences and funders while avoiding accidental claims that exceed the technical evidence or disclose unnecessary implementation constants before intellectual-property strategy is finalized.

## Public/high-level content judged suitable
- the global/Yemen need for safer remote chemical/environmental inspection;
- the integrated HazRob architecture at block level;
- eight-channel multi-sensor concept;
- Rs/R0/log-space normalization concept;
- spatial and temporal fingerprint concept;
- multiple analytical-method concept inside ChemCore;
- broad chemical-hypothesis library concept;
- M0–M4 evidence ladder;
- fail-closed outcomes;
- high-level Open-Set/OOD reject option;
- high-level AMCI closed-loop adaptive-analysis concept;
- exposure/hazard-context linkage;
- remote driving/video/telemetry concept;
- experiment/calibration laboratory workflow;
- controlled scientific update/provenance pipeline;
- low-cost prototype target, with scope disclaimer;
- candidate invention-family map at high level;
- applications and roadmap.

## Master-only / do-not-expand in public derivative without review
- exact AMCI thresholds, repeat/extension constants and state-policy numbers;
- exact inverse-solver grid/refinement constants;
- exact evidence-fusion weights, bonuses and caps;
- exact OOD numerical threshold and training/validation artifact details;
- unpublished M4 calibration recipes, concentrations, acceptance criteria and results;
- security-sensitive remote-control details, credentials, service endpoints or network internals;
- claim-boundary implementation details that unnecessarily reveal code-level architecture;
- unpublished device-transfer, drift or environmental correction parameters.

## Language controls — PASS
The deck uses wording such as `جوهر الابتكار`, `عائلات ابتكارية مرشحة للدراسة والحماية`, and `هدف تكلفة` rather than claiming:
- `patented`;
- `first in the world`;
- `unique` as a legal conclusion;
- `novel/non-obvious` as an established patent-law finding;
- `certified` where no certification exists.

## Invention-core framing
The strongest engineering narrative is the **integration** of:
1. coordinated multi-method chemical analysis;
2. broad chemical-hypothesis coverage;
3. evidence-governed result authority;
4. exposure/hazard-context linkage;
5. remote robotic measurement;
6. experimentation/calibration workflow;
7. controlled knowledge/model updates;
8. low-cost implementation target.

Individual known elements (MQ sensors, robotics, e-nose concepts, least-squares fitting, OOD, QSPR, remote control) should not be presented as individually novel without focused prior-art work. The candidate protectable space is more plausibly in the system coupling, decision gates, adaptive measurement orchestration, and evidence authority architecture.

## Conference derivative recommendation
Before public disclosure where patent filing status is uncertain, create a derivative that:
- preserves architecture/benefit diagrams;
- removes exact numerical policy constants;
- avoids unpublished calibration protocol specifics;
- summarizes code internals at block level;
- retains explicit `validation pending` labels;
- sends any proposed patent claim language to patent counsel before use.

## P8 decision
PASS for master/private invention and funding discussions. A separate public-conference derivative should be produced before broad public disclosure if patent filing has not already secured the intended priority position.
