# P4 — HazRob Master Deck Scientific Storyboard

Status: **COMPLETE — storyboard frozen for P5 visual production**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Baseline: HazRob 0.10.2 / HR069 RC2

> Internal production IDs below are for workflow tracking only. The actual presentation must show **no slide numbering**.

## Global slide rules
- Arabic RTL.
- One scientific message per slide.
- Titles normally 30–36 pt or larger; body 22–28 pt; tables ≥18–22 pt where practical.
- Prefer 1 dominant visual + 1 concise explanation.
- Source IDs in a compact footer or speaker notes.
- Every technical claim is tagged by status where useful: IMPLEMENTED / RESEARCH-SUPPORTED / VALIDATION PENDING / PROPOSED / FUTURE WORK.
- Patent-oriented slides use `عائلة ابتكارية مرشحة للدراسة والحماية` rather than legal novelty language.

---

# SECTION A — Opening, mission and global/Yemen need

## ST-A01 — الغلاف
**Core message:** HazRob is a robotic, multi-sensor, evidence-governed hazardous-gas sensing and chemical-inference platform.
**Content:** title + subtitle + project identity + presenters: شهاب الصهباني، مجدي ذيبان.
**Visual:** high-quality robot render + subtle system-layer icons.
**Sources:** INT-01, INT-02, INT-03.
**Status:** IMPLEMENTED platform / research prototype.
**Disclosure:** PUBLIC-SAFE.

## ST-A02 — HazRob في جملة واحدة
**Core message:** `روبوت متحرك + مصفوفة حساسات + ChemCore + تحليل تكيفي + قرار قائم على الأدلة`.
**Visual:** five-node horizontal architecture.
**Sources:** INT-01–INT-03.
**Status:** IMPLEMENTED.
**Disclosure:** PUBLIC-SAFE.

## ST-A03 — أصحاب المشروع / مقدمو العرض
**Core message:** intentionally left light for later bios/photos.
**Content:** placeholders only: شهاب الصهباني | مجدي ذيبان.
**Visual:** two empty portrait frames + role placeholders.
**Sources:** project owner input.
**Status:** PLACEHOLDER.
**Disclosure:** PUBLIC-SAFE.

## ST-A04 — لماذا نحتاج HazRob؟ حماية الأرواح والممتلكات والبيئة
**Core message:** hazardous atmospheres create human, asset and environmental consequences; distance and early information matter.
**Visual:** triangle `الإنسان – الممتلكات – البيئة` with robot between hazard and worker.
**Sources:** AUTH-01–AUTH-04, YEM context.
**Status:** CONTEXT.
**Disclosure:** PUBLIC-SAFE.

## ST-A05 — العبء العالمي للسلامة والصحة المهنية
**Core message:** work-related injury/disease remains a major global burden.
**Content:** ILO: ~2.93M work-related deaths/year; ~395M non-fatal work injuries/year; separate disease vs accident figures.
**Visual:** three large-number tiles; no dense text.
**Sources:** AUTH-03, AUTH-04.
**Caution:** do not attribute all deaths to chemicals.
**Disclosure:** PUBLIC-SAFE.

## ST-A06 — العبء الصحي للتعرضات الكيميائية
**Core message:** selected chemical exposures cause substantial preventable health loss worldwide.
**Content:** WHO estimate ~2M lives and ~53M DALYs lost in 2019 from selected chemical exposures; explicitly note incomplete coverage.
**Visual:** human silhouette + chemical hazard icons + DALY concept.
**Sources:** AUTH-02.
**Disclosure:** PUBLIC-SAFE.

## ST-A07 — تلوث الهواء: خطر صحي وبيئي واسع
**Core message:** environmental air quality makes continuous/remote monitoring strategically important.
**Content:** WHO 99% guideline exceedance context; 6.7M combined ambient/household premature deaths; 4.2M ambient in 2019.
**Visual:** world map + pollution plume + 2–3 number tiles.
**Sources:** AUTH-01.
**Disclosure:** PUBLIC-SAFE.

## ST-A08 — السياق اليمني: فجوات السلامة والصحة المهنية
**Core message:** published Yemen research documents institutional/education and prevention gaps relevant to hazardous exposure management.
**Visual:** Yemen silhouette + OSH system gap diagram.
**Sources:** AUT-01, AUT-03.
**Presenter note:** optionally mention presenter’s published research background without turning slide into biography.
**Disclosure:** PUBLIC-SAFE.

## ST-A09 — القياسات البيئية والمهنية في اليمن: الحاجة إلى بيانات ميدانية أفضل
**Core message:** Yemen studies document particulate, gaseous/air, radon, industrial pollution and occupational chemical-exposure contexts.
**Visual:** Yemen map with 5 measurement callouts: PM, CO/SO2/O3, radon, benzene, industrial pollution.
**Sources:** YEM-01–YEM-06.
**Caution:** contextual only; not HazRob validation.
**Disclosure:** PUBLIC-SAFE.

## ST-A10 — لماذا الروبوت بدل إرسال الإنسان؟
**Core message:** remote sensing can move exposure from the worker to the machine.
**Visual:** split scene: human entering hazard vs robot entering while operator remains remote.
**Sources:** PA-01, PA-02, commercial robot sources.
**Disclosure:** PUBLIC-SAFE.

## ST-A11 — المشكلة التقنية الحقيقية
**Core message:** `رصد استجابة ≠ تحديد مادة ≠ تقدير تركيز ≠ تفسير خطر`.
**Visual:** four-stage staircase with failure points.
**Sources:** INT-03, SCI literature.
**Disclosure:** PUBLIC-SAFE.

---

# SECTION B — Previous work, products and the gap

## ST-B01 — تطور المجال: من الحساس المفرد إلى الروبوت الشمّي
**Core message:** HazRob sits in an established evolutionary chain rather than appearing from a vacuum.
**Visual:** timeline: single sensor → e-nose → mobile robot olfaction → open-set → adaptive/evidence-governed systems.
**Sources:** PA-01–PA-08.
**Disclosure:** PUBLIC-SAFE.

## ST-B02 — الدراسات السابقة: الأنف الإلكتروني متعدد الحساسات
**Core message:** multi-sensor classification and concentration prediction are established and scientifically productive.
**Visual:** paper tiles for Wang 2022, Paredes 2016, Oshovsky 2026.
**Sources:** SCI-01, SCI-03, SCI-10.
**Disclosure:** PUBLIC-SAFE.

## ST-B03 — الدراسات السابقة: الروبوتات واستشعار/خرائط الغاز
**Core message:** robotic gas sensing, discrimination, mapping and source localization are established prior art.
**Visual:** literature landscape with robot + plume + map.
**Sources:** PA-01, PA-02, PA-03.
**Disclosure:** PUBLIC-SAFE.

## ST-B04 — الدراسات السابقة: Open-Set ورفض الغاز غير المعروف
**Core message:** unknown-class rejection in e-noses is prior art; HazRob must differentiate by integration and governance.
**Visual:** known clusters + unknown outside boundary.
**Sources:** PA-04, PA-05.
**Disclosure:** PUBLIC-SAFE.

## ST-B05 — الدراسات السابقة: الخلائط والتحليل متعدد المهام
**Core message:** mixture/VOC multi-task methods exist; HazRob currently uses a mixture/interference guard rather than claiming full deconvolution.
**Visual:** pure A/B vs A+B mixture decision diagram.
**Sources:** SCI-02 / PA-07, INT-03.
**Status:** mixture decomposition FUTURE; guard IMPLEMENTED.
**Disclosure:** PUBLIC-SAFE.

## ST-B06 — منتجات قريبة: كواشف الغاز والمراقبة المهنية
**Core message:** mature professional solutions exist for configured gas measurement and area monitoring.
**Visual:** product class cards: Dräger X-am 8000, Honeywell AreaRAE Plus.
**Sources:** CP-01, CP-02.
**Disclosure:** PUBLIC-SAFE.

## ST-B07 — منتجات قريبة: التصوير البصري والروبوتات الصناعية
**Core message:** OGI and autonomous industrial inspection solve adjacent parts of the problem.
**Visual:** FLIR GF77 + ANYmal X + ExR icons/images.
**Sources:** CP-03–CP-05.
**Disclosure:** PUBLIC-SAFE.

## ST-B08 — التكلفة المنشورة للحلول المتخصصة
**Core message:** published examples span thousands to tens of thousands of USD, while industrial robots can be quote/service based.
**Visual:** logarithmic or categorical cost-band chart; avoid false apples-to-apples comparison.
**Sources:** P2_COMPARABLE_PRODUCTS_COSTS, CP-01–CP-08.
**Presenter note:** say `أمثلة أسعار منشورة` not `متوسط السوق`.
**Disclosure:** PUBLIC-SAFE.

## ST-B09 — المقارنة العادلة: كل أداة تحل جزءًا مختلفًا
**Core message:** portability, mobility, selectivity, imaging, automation and analytical inference are different capabilities.
**Visual:** wide capability matrix: portable detector / area monitor / OGI / industrial robot / HazRob.
**Sources:** P2 comparator matrix + INT-02/03.
**Caution:** prominently show HazRob not yet Ex-certified / incomplete M4.
**Disclosure:** PUBLIC-SAFE.

## ST-B10 — ما هو معروف بالفعل؟ وما الفجوة التي يستهدفها HazRob؟
**Core message:** candidate differentiation lies in the **governed integrated analytical loop**, not individual known components.
**Visual:** left `Known`: robot, e-nose, OOD, mapping; right `HazRob integration`: adaptive interrogation, inverse solver, evidence tiers, lineage, fail-closed reportability.
**Sources:** P2_PRIOR_ART_GAP_MAP, P3.
**Disclosure:** PUBLIC-SAFE with candidate wording.

---

# SECTION C — HazRob system architecture

## ST-C01 — الفكرة المركزية لمنظومة HazRob
**Core message:** turn a mobile cross-sensitive sensor array into an auditable chemical-inference system.
**Visual:** environment → robot → array → ChemCore → evidence → decision.
**Sources:** INT-01–INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-C02 — طبقات المنظومة
**Core message:** HazRob is a layered cyber-physical system.
**Visual:** seven-layer stack: physical sensing, acquisition, control, communication, data, chemical intelligence, decision/reporting.
**Sources:** INT-02.
**Disclosure:** PUBLIC-SAFE.

## ST-C03 — المعمارية الشاملة End-to-End
**Core message:** one traceable path connects environmental exposure to final report.
**Visual:** full architecture diagram with ESP32, ESP32-CAM, Android, storage, ChemCore, reports.
**Sources:** INT-01, INT-02.
**Disclosure:** PUBLIC-SAFE high-level.

## ST-C04 — البنية الفيزيائية HR069
**Core message:** sensing, control, mobility and video are integrated on the robot.
**Visual:** exploded robot view: ESP32, camera, L298N, ADS1115×2, MQ array, BME, relay, fan, IR, ultrasonic, motors/servos.
**Sources:** INT-01, INT-02.
**Status:** IMPLEMENTED hardware baseline.
**Disclosure:** PUBLIC-SAFE.

## ST-C05 — مصفوفة الحساسات الغازية
**Core message:** eight cross-sensitive MQ channels create a response pattern rather than eight independent selective analyzers.
**Visual:** ring of MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135 around a central `chemical fingerprint`.
**Sources:** INT-03, INT-05.
**Disclosure:** PUBLIC-SAFE.

## ST-C06 — غرفة أخذ العينة وتدفق الهواء
**Core message:** controlled airflow and sensor exposure are part of measurement quality.
**Visual:** chamber schematic: two inlets → sensor array → rear fan → outlet; dimensions annotated if desired.
**Sources:** INT-02, project hardware data.
**Disclosure:** PUBLIC-SAFE schematic.

## ST-C07 — الحركة والتحكم
**Core message:** robot mobility enables remote approach, repositioning and future spatial confirmation.
**Visual:** 4WD differential drive / spin-in-place / controls.
**Sources:** INT-01, INT-02.
**Disclosure:** PUBLIC-SAFE.

## ST-C08 — طبقة السلامة الروبوتية
**Core message:** collision/cliff sensing, E-stop/watchdog and analytical motion interlocks are distinct safety layers.
**Visual:** safety shields around robot: Echo, IR, watchdog, E-stop, motion interlock.
**Sources:** INT-02, RC2 source/tests.
**Caution:** note current IR/ultrasonic automatic-stop policy divergence in limitations, not imply certification.
**Disclosure:** PUBLIC-SAFE high-level.

## ST-C09 — الاتصال المباشر والبعيد
**Core message:** two operating modes support local direct control and remote Internet operation.
**Visual:** split architecture: Direct AP vs Remote Internet/server path.
**Sources:** INT-02, RC2.
**Disclosure:** PUBLIC-SAFE high-level; omit credentials/internal endpoints.

## ST-C10 — دورة البيانات والتخزين
**Core message:** measurements are not ephemeral; sessions, sites, telemetry and reports form a traceable data lifecycle.
**Visual:** telemetry → session store → analysis → site/session library → PDF/XLSX/ZIP.
**Sources:** INT-02, app architecture.
**Disclosure:** PUBLIC-SAFE.

## ST-C11 — رحلة القياس كاملة
**Core message:** HazRob is a pipeline from physical sample to explainable output.
**Visual:** single horizontal process with 10 nodes from gas inlet to SmartReport.
**Sources:** INT-02, INT-03.
**Disclosure:** PUBLIC-SAFE.

---

# SECTION D — Measurement physics and feature engineering

## ST-D01 — كيف يعمل حساس MOX/MOS؟
**Core message:** gas-surface interactions alter semiconductor resistance; cross-sensitivity is both limitation and array-information source.
**Visual:** simplified SnO2 surface + adsorbed oxygen + reducing/oxidizing gas interaction.
**Sources:** scientific/manufacturer background.
**Disclosure:** PUBLIC-SAFE.

## ST-D02 — من الجهد إلى المقاومة Rs
**Core message:** electrical acquisition is converted into physically interpretable sensor resistance.
**Visual:** divider circuit + formula `Rs = RL × (Vc/Vout − 1)`.
**Sources:** INT-03, sensor fundamentals.
**Disclosure:** PUBLIC-SAFE.

## ST-D03 — ما معنى R0؟
**Core message:** R0 is a reference resistance for normalization—not zero and not the maximum reading.
**Visual:** clean-air/reference baseline diagram.
**Sources:** INT-03, FieldR0Baseline.
**Status:** device-specific field baseline; not M4 gas calibration.
**Disclosure:** PUBLIC-SAFE.

## ST-D04 — لماذا Rs/R0؟
**Core message:** normalization makes sensor responses more comparable across channels/sessions than raw resistance alone.
**Visual:** raw Rs curves vs normalized Rs/R0 curves.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-D05 — لماذا log10(Rs/R0)؟
**Core message:** log-space supports multiplicative sensor behavior, model fitting and residual comparison.
**Visual:** nonlinear ratio domain → log-space transform.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-D06 — جودة القياس قبل الذكاء
**Core message:** ChemCore distinguishes configuration readiness, capture readiness and environmental/calibration readiness.
**Visual:** three traffic-light gates.
**Sources:** ChemCoreLabQuality, INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-D07 — التسخين ودورات MQ7/MQ9
**Core message:** sensor heater state can determine whether a channel is valid for quantitative/reference use.
**Visual:** heater-cycle waveform + valid/invalid windows.
**Sources:** INT-03, sensor policy.
**Status:** IMPLEMENTED gating / hardware validation limitations remain.
**Disclosure:** PUBLIC-SAFE.

## ST-D08 — الاستجابة الزمنية ليست نقطة واحدة
**Core message:** baseline, rise, peak, steady-state and recovery contain discriminative information.
**Visual:** annotated response curve.
**Sources:** INT-03, SCI-02, SCI-10.
**Disclosure:** PUBLIC-SAFE.

## ST-D09 — استخراج 17 ميزة لكل حساس
**Core message:** ChemCore converts each time series into interpretable kinetic/statistical features.
**Visual:** response curve with callouts for baseline, steady, delta, peak, slopes, t10/t50/t90, AUC, noise, SNR.
**Sources:** ChemCoreFeaturePipeline, INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-D10 — من 8 حساسات إلى فضاء تحليلي يصل إلى 136 ميزة
**Core message:** the array becomes a high-dimensional spatial-temporal fingerprint.
**Visual:** 8×17 heatmap / feature cube.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

---

# SECTION E — ChemCore analytical intelligence

## ST-E01 — ما هو ChemCore؟
**Core message:** ChemCore is an inference + evidence-governance engine, not merely a gas database.
**Visual:** four quadrants: models, fingerprints, inference, evidence governance.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-E02 — خط معالجة ChemCore الكامل
**Core message:** multiple analytical methods converge under common decision gates.
**Visual:** `Rs/R0 → quality → features → candidates → inverse solver/fingerprint/QSPR → OOD/conflicts → evidence → outcome`.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-E03 — مكتبة المعرفة: 144 مقابل 652
**Core message:** operational reporting targets and molecular knowledge are deliberately separate scopes.
**Visual:** nested circles: 144 operational targets vs 652 molecular records, with resolved/unresolved note if used.
**Sources:** INT-03, project library.
**Disclosure:** PUBLIC-SAFE.

## ST-E04 — مصادر نموذج الاستجابة
**Core message:** ChemCore distinguishes manufacturer, literature, research-transfer, QSPR and device-validated evidence.
**Visual:** evidence-source hierarchy feeding response models.
**Sources:** INT-03, INT-04/05.
**Disclosure:** PUBLIC-SAFE.

## ST-E05 — توليد المرشحين: فرضيات لا أحكام نهائية
**Core message:** each candidate begins as a hypothesis that must explain the sensor array and pass gates.
**Visual:** candidate cards from library flowing to solver.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-E06 — فكرة الحل العكسي متعدد الحساسات
**Core message:** ask `ما التركيز الواحد الذي يجعل هذا المرشح يفسر جميع القنوات بأفضل صورة؟`.
**Visual:** one candidate/concentration predicting all sensors vs observations.
**Sources:** IF-02 / INT-03.
**Disclosure:** PUBLIC-SAFE concept.

## ST-E07 — المعادلة المشتركة في فضاء اللوغاريتم
**Core message:** standardized residuals combine response disagreement, uncertainty and evidence reliability.
**Visual:** simplified z-score and weighted loss equations with symbols explained.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE equation; keep implementation constants master-only.

## ST-E08 — البحث عن التركيز الأفضل
**Core message:** each candidate produces a loss-vs-concentration landscape; the minimum is the best joint hypothesis.
**Visual:** 3 colored loss curves for three chemicals over log ppm.
**Sources:** INT-03.
**Status:** illustrative chart unless generated from actual session data.
**Disclosure:** PUBLIC-SAFE if marked illustrative.

## ST-E09 — البواقي Residuals تكشف التناقض
**Core message:** a candidate can rank well overall yet fail on particular channels; residuals are retained for consistency/interference logic.
**Visual:** observed/predicted bar pairs + residual heatmap.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-E10 — Ranking Weight ≠ Probability ≠ Confidence
**Core message:** three semantics must remain distinct.
**Visual:** three separate gauges: relative ranking weight, analytical support/confidence index, future calibrated probability (not claimed).
**Sources:** INT-03, P1 claim matrix.
**Disclosure:** PUBLIC-SAFE and important credibility slide.

## ST-E11 — البصمة متعددة القنوات
**Core message:** chemical identity may emerge from the pattern across channels rather than a single sensor.
**Visual:** radar chart + heatmap for illustrative chemicals.
**Sources:** INT-03, SCI-01, SCI-10.
**Disclosure:** PUBLIC-SAFE; mark simulated/reference data appropriately.

## ST-E12 — محرك البصمات: Distance + Coverage + Separation
**Core message:** fingerprint matching uses distance, channel coverage and runner-up separation, not name matching alone.
**Visual:** observed vector vs reference templates.
**Sources:** FingerprintClassifier, AcceptedFingerprintIdentityEngine, INT-03.
**Disclosure:** PUBLIC-SAFE high-level.

## ST-E13 — Open-Set / OOD: متى يقول النظام «لا أعرف»؟
**Core message:** unknown rejection is a safety feature, not a failure.
**Visual:** known clusters + unknown point beyond boundary.
**Sources:** INT-03, PA-04.
**Status:** IMPLEMENTED; independent HR069 validation pending.
**Disclosure:** PUBLIC-SAFE with validation caveat.

## ST-E14 — Mahalanobis Space وتغطية الخصائص
**Core message:** acceptance depends on both distance from known domain and sufficient valid feature coverage.
**Visual:** covariance ellipse + point + coverage meter.
**Sources:** ChemCoreOpenSetGate, INT-03.
**Disclosure:** PUBLIC-SAFE concept; no unvalidated numeric threshold.

## ST-E15 — الخلائط والتداخل: الحارس قبل الادعاء
**Core message:** current system can flag poor pure-compound consistency; it does not yet claim arbitrary mixture decomposition.
**Visual:** single-candidate consistent vs ambiguous vs possible mixture/interference.
**Sources:** MixtureResidualGuard, INT-03, SCI-02.
**Status:** guard IMPLEMENTED / decomposition FUTURE.
**Disclosure:** PUBLIC-SAFE.

## ST-E16 — QSPR: عندما تساعد البنية الجزيئية
**Core message:** molecular descriptors can provide M1 transfer support only inside applicability-domain rules.
**Visual:** molecule → descriptors → sensor-response prior → M1 badge.
**Sources:** ChemCoreQsprRuntimeRegistry, INT-03.
**Caution:** QSPR cannot independently create M3/M4/PPM.
**Disclosure:** PUBLIC-SAFE.

## ST-E17 — البيئة والانجراف Drift
**Core message:** HazRob currently gates environmental domain and monitors drift rather than inventing unvalidated compensation coefficients.
**Visual:** T/RH domain box + drift timeline + recalibration flag.
**Sources:** ChemCoreEnvironmentDomainPolicy, ChemCoreAnchorDriftMonitor, INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-E18 — دمج الأدلة مع معرفة نسبها العلمي
**Core message:** one paper producing several derived artifacts does not become several independent proofs.
**Visual:** paper → curve/fingerprint/QSPR → de-duplication/lineage fusion.
**Sources:** ChemCoreEvidenceFusion, IF-05.
**Disclosure:** PUBLIC-SAFE principle.

## ST-E19 — سلم الأدلة M0–M4
**Core message:** knowledge, inference, literature, array consistency and device validation are different authorities.
**Visual:** five-level pyramid/ladder.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-E20 — M3 ليس M4
**Core message:** identification can be strong enough for M3 while professional quantitative PPM remains withheld.
**Visual:** identity gate vs quantitative gate.
**Sources:** INT-03, P1 claim matrix.
**Disclosure:** PUBLIC-SAFE, critical credibility slide.

---

# SECTION F — Adaptive operation, decisions and outputs

## ST-F01 — آلة الحالات للتحليل المتحكم به
**Core message:** chemical analysis is orchestrated as a deterministic state machine.
**Visual:** CONDITION → BASELINE → EXPOSURE → STABILITY → FEATURES → IDENTITY → OOD → QUANTIFICATION → CONSISTENCY → REPORT → RECOVERY.
**Sources:** ChemCoreSessionController, INT-03.
**Disclosure:** PUBLIC-SAFE high-level.

## ST-F02 — AMCI: التحليل الذي يقرر القياس التالي
**Core message:** the system can use current evidence to decide whether more measurement is needed.
**Visual:** closed loop `Measure → Evaluate → Decide → Measure again / finish`.
**Sources:** IF-01, AmciDecisionEngine.
**Disclosure:** PUBLIC-SAFE high-level.

## ST-F03 — مثال: مرشحان متشابهان
**Core message:** ambiguity can trigger more evidence rather than forced classification.
**Visual:** candidate A/B close scores → extend exposure/recovery → temporal separation → decision or Unknown.
**Sources:** AMCI policy + illustrative example.
**Status:** illustrative scenario.
**Disclosure:** PUBLIC-SAFE.

## ST-F04 — الروبوت جزء من صلاحية القياس
**Core message:** motion, heater validity, environment and session state can determine whether chemical inference is allowed.
**Visual:** robot-state gates feeding `analysis permitted?`.
**Sources:** IF-06, ChemCoreLabQuality, runtime coordinator.
**Disclosure:** PUBLIC-SAFE concept.

## ST-F05 — شجرة النتيجة النهائية
**Core message:** HazRob supports multiple honest outcomes rather than always returning a named gas.
**Visual:** outcome tree: QUANTIFIED / IDENTIFIED / SCREENING / UNKNOWN-OOD / INSUFFICIENT / MIXTURE-INTERFERENCE / INVALID.
**Sources:** ChemCoreInferenceEngine, INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-F06 — متى يسمح النظام بإظهار PPM؟
**Core message:** reportable PPM requires M4 quantitative support plus accepted identity/domain/calibration conditions.
**Visual:** four locks → PPM.
**Sources:** INT-03, ChemCoreLabQuality.
**Disclosure:** PUBLIC-SAFE.

## ST-F07 — SmartReport: من الاستدلال إلى تقرير قابل للتدقيق
**Core message:** output includes outcome, candidate evidence, residuals, quality, provenance and reportability—not only a number.
**Visual:** ResultEnvelope → charts/tables → PDF/XLSX.
**Sources:** ResultEnvelope/report architecture, INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-F08 — Sites / Sessions / تاريخ القياسات
**Core message:** field operations are organized by site and session, enabling session and aggregate reporting.
**Visual:** site tree → sessions → individual/aggregate reports.
**Sources:** INT-02 app architecture.
**Disclosure:** PUBLIC-SAFE.

---

# SECTION G — Innovation framing, applications and roadmap

## ST-G01 — سلسلة الابتكار في HazRob
**Core message:** project value comes from integration across hardware, sampling, analytics, evidence and decision.
**Visual:** innovation stack from physical robot to evidence-governed outcome.
**Sources:** P3, INT-02/03.
**Disclosure:** PUBLIC-SAFE.

## ST-G02 — خريطة العائلات الابتكارية المرشحة للدراسة والحماية
**Core message:** five front-stage candidate families form the integrated invention narrative.
**Visual:** central HazRob circle with five spokes: AMCI / inverse solver / OOD-M3-M4 / lineage fusion / robot-state validity; supporting ring M0–M4, fail-closed, provenance.
**Sources:** P3_PATENT_CANDIDATE_MAP.
**Disclosure:** MASTER + PUBLIC with disclaimer.

## ST-G03 — العائلة المرشحة: AMCI + الحل العكسي
**Core message:** two complementary mechanisms: decide what to measure next, and jointly explain what was measured.
**Visual:** left AMCI loop; right loss landscape; center feedback link.
**Sources:** IF-01, IF-02.
**Disclosure:** high-level PUBLIC; exact constants master-only.

## ST-G04 — العائلة المرشحة: OOD → Fingerprint → M3 → M4
**Core message:** unknown-aware identity is explicitly separated from device-validated quantification.
**Visual:** gated pipeline.
**Sources:** IF-03, IF-04.
**Disclosure:** PUBLIC with validation caveat.

## ST-G05 — العائلة المرشحة: Lineage + Fail-Closed + Robot-State Validity
**Core message:** trustworthy inference requires knowing where evidence came from, when measurement is valid, and when not to claim certainty.
**Visual:** three interlocking shields.
**Sources:** IF-05, IF-06, IF-08.
**Disclosure:** PUBLIC high-level.

## ST-G06 — كيف يتحول البحث المنشور إلى معرفة تشغيلية؟
**Core message:** HazRob uses a governed research-to-runtime provenance chain.
**Visual:** Paper/Dataset → source grade → sensor overlap → analyte mapping → curve/fingerprint → lineage → runtime evidence → report.
**Sources:** INT-04–INT-07, IF-07.
**Disclosure:** PUBLIC high-level.

## ST-G07 — التطبيقات المستهدفة
**Core message:** mobile hazardous-gas intelligence can serve multiple safety/environmental missions.
**Visual:** 6–8 icon wheel: confined spaces, fire/emergency, industrial leak inspection, warehouses, chemical incidents, environmental surveys, laboratories, research.
**Sources:** project goals + prior art context.
**Disclosure:** PUBLIC-SAFE; label as target applications where not field-validated.

## ST-G08 — قابلية التوسع لما بعد MQ
**Core message:** the architecture can conceptually incorporate electrochemical, PID, NDIR, BME688 and other sensor modalities.
**Visual:** plug-in sensor modalities around ChemCore interface.
**Sources:** target architecture / project roadmap.
**Status:** FUTURE/PROPOSED except current installed set.
**Disclosure:** PUBLIC-SAFE if labeled future.

## ST-G09 — نقاط القوة الحالية
**Core message:** integration, explainability, multiple evidence methods, fail-closed decisions, provenance and mobile operation are current strengths.
**Visual:** six concise strength tiles.
**Sources:** HazRob/ChemCore reports.
**Disclosure:** PUBLIC-SAFE.

## ST-G10 — القيود الحالية بوضوح
**Core message:** credibility increases when current gaps are stated explicitly.
**Content:** incomplete M4 campaigns; OOD external validation pending; MQ7/MQ9 heater-cycle limitations; mixture deconvolution future; no Ex/ATEX certification; environmental compensation intentionally not claimed; navigation safety-policy divergence to resolve.
**Visual:** limitation → mitigation table.
**Sources:** INT-02, INT-03, deferred OOD task.
**Disclosure:** PUBLIC-SAFE, exact engineering detail can be adjusted for venue.

## ST-G11 — خارطة التحقق العلمي
**Core message:** progress path from research prototype to validated quantitative platform is defined.
**Visual:** P0/P1 style roadmap: M4 reference gases → OOD known/unknown → environment → mixtures → longitudinal drift → multi-device reproducibility.
**Sources:** ChemCore validation plan.
**Disclosure:** PUBLIC-SAFE.

## ST-G12 — خارطة التطوير والجاهزية التقنية
**Core message:** transition from prototype to qualified field system requires validation, certification and deployment evidence.
**Visual:** TRL-style staged roadmap without inventing formal TRL number unless justified.
**Sources:** HazRob report recommendations.
**Disclosure:** PUBLIC-SAFE.

---

# SECTION H — Glossary, references and closing

## ST-H01 — المصطلحات والاختصارات الأساسية — القياس والتحليل
**Core message:** audience can decode core measurement language.
**Content:** Rs, R0, Rs/R0, ADC, PPM, MOX/MOS, SNR, AUC, log10.
**Visual:** two-column glossary cards; large type.
**Sources:** INT-03.
**Disclosure:** PUBLIC-SAFE.

## ST-H02 — المصطلحات والاختصارات الأساسية — الذكاء والنظام
**Core message:** audience can decode inference/system language.
**Content:** M0–M4, OOD, Open-Set, AMCI, QSPR, UID/chemical_uid, UX, UI, API, MQTT, ESP32, Ranking Weight, Analytical Confidence.
**Visual:** two-column glossary cards.
**Sources:** INT-02/03.
**Disclosure:** PUBLIC-SAFE.

## ST-H03 — المراجع العلمية الرئيسية — APA 7
**Core message:** core methodology is traceable to peer-reviewed literature.
**Content:** selected high-priority SCI/PA references only; use readable font and continue next slide.
**Visual:** clean two-column reference layout.
**Sources:** P1_SOURCE_REGISTER_APA7.
**Disclosure:** PUBLIC-SAFE.

## ST-H04 — المصادر والسياق والمنتجات — APA 7 / Official sources
**Core message:** WHO/ILO, Yemen studies, project-owner publications and commercial evidence are traceable.
**Content:** selected AUT/YEM/AUTH sources + manufacturer/commercial source notes.
**Visual:** categorized reference blocks.
**Sources:** P1/P2 registers.
**Disclosure:** PUBLIC-SAFE.

## ST-H05 — شكرًا لكم
**Core message:** close with project identity and invitation for scientific/technical discussion.
**Content:** `شكرًا لكم` + HazRob logo + presenters.
**Visual:** robot render and logo; minimal text.
**Sources:** none.
**Disclosure:** PUBLIC-SAFE.

---

# Storyboard statistics

- Planned master slides: **75**.
- Opening/need: 11.
- Prior work/products/gap: 10.
- System architecture: 11.
- Measurement/feature engineering: 10.
- ChemCore analytical intelligence: 20.
- Adaptive decisions/outputs: 8.
- Innovation/applications/roadmap: 12.
- Glossary/references/closing: 5.

> Section counts overlap the production grouping above only as internal planning; the actual deck will not display section/slide numbers unless section dividers are later requested.

# P5 visual asset list generated from storyboard

Priority reusable visuals:
1. hero HazRob robot render;
2. life/assets/environment triangle;
3. ILO/WHO statistic tiles;
4. Yemen environmental-measurement map;
5. prior-art timeline;
6. prior-art known-vs-gap map;
7. commercial comparator matrix;
8. log-scale/categorical cost chart;
9. end-to-end HazRob architecture;
10. robot exploded hardware schematic;
11. MQ array ring;
12. chamber airflow schematic;
13. direct vs remote connectivity map;
14. telemetry/storage lifecycle;
15. MOX surface mechanism schematic;
16. ADC/Rs circuit diagram;
17. R0 / Rs/R0 / log10 transformation sequence;
18. heater-cycle diagram;
19. temporal response curve;
20. 17-feature annotated curve;
21. 8×17 / 136-feature heatmap;
22. ChemCore full pipeline;
23. 144 vs 652 library graphic;
24. response-model evidence hierarchy;
25. inverse-solver observed/predicted graphic;
26. loss-vs-concentration chart;
27. residual heatmap;
28. ranking-vs-confidence semantic graphic;
29. fingerprint radar/heatmap;
30. OOD feature-space graphic;
31. mixture/interference guard graphic;
32. QSPR molecule-to-prior diagram;
33. environment/drift graphic;
34. lineage-aware evidence tree;
35. M0–M4 ladder;
36. identity vs quantitative gate;
37. controlled-session state machine;
38. AMCI closed-loop graphic;
39. final outcome tree;
40. PPM four-lock gate;
41. SmartReport pipeline;
42. site/session tree;
43. innovation stack;
44. patent-candidate map;
45. research-to-runtime provenance chain;
46. applications wheel;
47. sensor expansion architecture;
48. limitations/mitigations matrix;
49. validation roadmap;
50. development/readiness roadmap.

# P4 completion decision

P4 exit criteria are met:
- master narrative is frozen at 75 slide concepts;
- every slide has a clear purpose, visual concept and evidence family;
- mandatory presenter/glossary/WHO-ILO/prior-art/products/references/thanks requirements are included;
- technical/patent disclosure caveats are embedded;
- P5 visual production backlog is explicit.

**Next phase: P5 — visual specification and figure production.**
