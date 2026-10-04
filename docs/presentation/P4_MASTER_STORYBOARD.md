# P4 — HazRob Master Deck Scientific Storyboard

Status: **COMPLETE — 75-slide master storyboard frozen for P5**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Baseline: HazRob 0.10.2 / HR069 RC2

> IDs S01–S75 are internal production identifiers only. The delivered presentation must show **no visible slide numbering**.

## Global production rules
- Arabic RTL; 16:9; no slide numbers.
- One scientific message per slide.
- Titles normally 30–36 pt+; body 22–28 pt; tables normally 18–22 pt+.
- Prefer one dominant visual with concise explanatory text.
- Use status tags only where useful: IMPLEMENTED / RESEARCH-SUPPORTED / VALIDATION PENDING / PROPOSED / FUTURE WORK.
- Do not present M3 as M4, ranking weight as probability, or literature/reference PPM as HR069 device-validated PPM.
- Patent language: `عائلة ابتكارية مرشحة للدراسة والحماية`; no legal novelty conclusion.
- Public/master-only sensitivity is recorded below.

## A. Opening, need and context

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S01 | HazRob — منظومة روبوتية ذكية لتحليل الغازات والمواد الكيميائية الخطرة | المشروع منظومة متكاملة وليست حساسًا منفردًا | Hero system visual: robot + sensor array + ChemCore + report | RC2 / PUBLIC |
| S02 | أصحاب المشروع / مقدمو العرض | شهاب الصهباني، مجدي ذيبان | شريحة تعريفية فارغة نسبيًا بصورتين Placeholder | USER / PUBLIC |
| S03 | لماذا نحتاج HazRob؟ | تقليل دخول الإنسان إلى البيئات الخطرة مع رفع جودة القرار | إنسان خارج منطقة خطر وروبوت داخلها | WHO/ILO + project rationale |
| S04 | العبء العالمي للسلامة والصحة المهنية | الحوادث والأمراض المرتبطة بالعمل تمثل عبئًا عالميًا كبيرًا | أرقام ILO كبيرة + pictograms | ILO |
| S05 | عبء تلوث الهواء والملوثات | التلوث الهوائي مرتبط بملايين الوفيات المبكرة | خريطة/Infographic WHO | WHO |
| S06 | الخطر لا يقتصر على الأرواح | حماية الأرواح والممتلكات والبيئة هدف واحد مترابط | مثلث Lives–Assets–Environment | authoritative context |
| S07 | السياق اليمني للصحة والسلامة المهنية | فجوات السلامة المحلية تزيد قيمة أدوات القياس والوقاية | خريطة اليمن + مقتطفات بحثية موجزة | Al-Sohpany publications |
| S08 | القياسات البيئية في اليمن | توجد حاجة محلية موثقة لقياسات ميدانية للملوثات والتعرضات | أمثلة PM/CO/SO2/benzene/radon | Yemen studies |
| S09 | المشكلة التقنية | الكشف ≠ التعرف ≠ التقدير الكمي ≠ تفسير الخطر | أربع طبقات متتابعة | scientific framing |
| S10 | لماذا لا يكفي حساس MQ منفرد؟ | الحساسية المتصالبة تجعل القراءة الواحدة غير انتقائية | response-overlap graphic | manufacturer/literature |

## B. الدراسات السابقة والمنتجات والفجوة

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S11 | من الأنف الإلكتروني إلى الاستدلال متعدد الحساسات | الأدبيات تثبت قيمة الأنماط متعددة القنوات | timeline of e-nose research | P1/P2 |
| S12 | دراسات سابقة باستخدام مصفوفات MQ مشابهة | عدة دراسات استخدمت 6–8 قنوات متداخلة مع HR069 | matrix: study × sensors | mq source index |
| S13 | دراسات الخلائط والتمييز الديناميكي | السلوك الزمني يساعد على التمييز والخلائط | exposure/recovery curves | Mei et al. + others |
| S14 | Open-Set ورفض المجهول في استشعار الغاز | الأنظمة المغلقة قد تفرض أقرب تصنيف خاطئ | known cluster + unknown point | OOD literature |
| S15 | الروبوتات في فحص وتسربات الغاز | الحركة والقياس عن بعد موجودان كاتجاه صناعي وبحثي | product/research landscape | P2 |
| S16 | المنتجات المحمولة متعددة الغازات | قوية للقياس المباشر لكنها محدودة في الاستدلال الواسع | portable detector examples | product sources |
| S17 | Area Monitors وكاميرات كشف الغاز | حلول احترافية لكنها عالية التكلفة أو متخصصة | capability/cost cards | product sources |
| S18 | روبوتات التفتيش الصناعية | روبوتات قوية للحركة والاستشعار لكن بتكلفة كبيرة | ANYmal/ExRobotics examples | product sources |
| S19 | التكلفة كحاجز انتشار | الحلول المتقدمة قد تصل لآلاف أو عشرات الآلاف من الدولارات | logarithmic cost bar | sourced prices only |
| S20 | الفجوة التي يستهدفها HazRob | المطلوب دمج الحركة + تحليل متعدد الأساليب + توسيع النتائج + سياق الخطر + تكلفة منخفضة | gap matrix | P2 gap map |

## C. جوهر الابتكار المتكامل

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S21 | جوهر الابتكار | الابتكار في **المنظومة التحليلية المتكاملة متعددة الأساليب** | central integrated-invention diagram | P3 / IMPLEMENTED+ |
| S22 | ست ركائز مترابطة | تحليل متعدد الأساليب، تغطية واسعة، حدود تعرض، قيادة بعيدة، مختبر وتجارب، تحديثات | hex/radial map | P3 |
| S23 | من قراءة واحدة إلى منظومة فرضيات | HazRob لا يكتفي بقراءة قناة بل يقارن فرضيات كيميائية متعددة | sensor vector → candidate field | ChemCore |
| S24 | توسيع نطاق النتائج | المكتبة التشغيلية تسمح بفرز عشرات المركبات مع حجب الادعاء عند ضعف الدليل | 144 operational targets vs identified subset | RC2 |
| S25 | النتيجة مرتبطة بالسلامة | الهوية/التركيز المؤهلان يمكن ربطهما بحدود التعرض والخطر | chemical → ppm → exposure limits | report layer |
| S26 | القيادة والقياس عن بُعد | فصل الإنسان عن منطقة الخطر مع بقاء التحكم والقياس والبث | direct vs remote control | RC2 |
| S27 | مختبر التجارب داخل المنظومة | المعايرة والبصمات والتجارب جزء من دورة التطوير وليس عملًا منفصلًا | lab loop | RC2/Lab |
| S28 | منظومة قابلة للتحديث | المكتبات والمنحنيات والبصمات يمكن تحديثها تحت حوكمة | research → validation → update → runtime | harness |
| S29 | هدف تكلفة نموذج أولي أقل من 200 دولار | القيمة الاقتصادية عامل تمكين، لا ادعاء براءة مستقل | BOM donut + comparator cost axis | TARGET / BOM verification pending |

## D. البنية الكاملة للروبوت والمنصة

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S30 | المعمارية العليا لـHazRob | البيئة → الروبوت → القياس → التطبيق → ChemCore → القرار | end-to-end architecture | RC2 |
| S31 | المكونات المادية HR069 | ESP32/ESP32-CAM/ADS1115/MQ/BME/IR/Echo/motors/fan | exploded hardware view | RC2 |
| S32 | مصفوفة الحساسات | 8 قنوات MQ متكاملة ذات أنماط حساسية متقاطعة | array map with analyte affinities | RC2/manufacturer |
| S33 | غرفة أخذ العينة وتدفق الهواء | ثبات التدفق والحالة الفيزيائية يؤثر في صلاحية القياس | chamber airflow schematic | project design |
| S34 | الحركة والسلامة الروبوتية | قيادة، E-STOP، watchdog، IR/Echo وتحذيرات | vehicle safety block diagram | firmware |
| S35 | الاتصال المباشر والبعيد | نمطا AP المباشر والإنترنت يقدمان مرونة تشغيلية | dual-path network diagram | RC2 |
| S36 | البث المرئي والتحكم | الفيديو والتحكم يظلان منفصلين عن منطق ChemCore التحليلي | camera/control/data lanes | RC2 |
| S37 | التخزين والجلسات والمواقع | القياسات منظمة كسياق Site/Session قابل للتقرير والمراجعة | data hierarchy | app |

## E. من الإشارة الخام إلى البصمة

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S38 | كيف يعمل حساس MOX/MQ؟ | تغير المقاومة نتيجة تفاعلات السطح هو أساس القياس | MOX surface illustration | literature/manufacturer |
| S39 | من ADC إلى Rs | الإشارة الكهربائية تتحول إلى مقاومة حساسة للغاز | circuit + equation | implementation |
| S40 | ما هو R0؟ | R0 مرجع تطبيع وليس صفرًا ولا أعلى قيمة | Rs vs R0 concept | ChemCore |
| S41 | لماذا Rs/R0؟ | التطبيع يقلل أثر اختلاف المقياس بين القنوات/الأجهزة | raw vs normalized chart | ChemCore |
| S42 | لماذا log10(Rs/R0)؟ | المجال اللوغاريتمي مناسب للنماذج والمنحنيات والمقارنة | transform plot | ChemCore |
| S43 | البصمة المكانية | الغاز يولد متجه استجابة عبر المصفوفة | radar plot | fingerprint engine |
| S44 | البصمة الزمنية | baseline/exposure/steady/recovery تحمل معلومات إضافية | time-series curves | feature pipeline |
| S45 | هندسة الميزات | حتى 17 ميزة لكل حساس تصف الشكل والاستقرار والزمن والضوضاء | feature extraction infographic | ChemCore |
| S46 | حتى 136 ميزة للمصفوفة | 8 حساسات × 17 ميزة توفر فضاء توصيف غنيًا | feature heatmap | derived |

## F. ChemCore — المحرك التحليلي متعدد الأساليب

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S47 | ما هو ChemCore؟ | محرك استدلال وإدارة أدلة، وليس قاعدة بيانات فقط | core architecture | RC2 |
| S48 | خط معالجة ChemCore الكامل | Quality → Features → Candidates → Inference → OOD → Evidence → Outcome | process map | RC2 |
| S49 | مكتبتان بوظيفتين مختلفتين | 144 هدفًا تشغيليًا مقابل 652 سجلًا جزيئيًا داعمًا | dual-library diagram | RC2 |
| S50 | مصادر الاستجابة الكيميائية | Manufacturer + literature + research + QSPR + device validation | evidence-source hierarchy | RC2 |
| S51 | المنهج الأول: المنحنيات المرجعية | Rs/R0 ↔ ppm كنماذج استجابة عند توفر دليل | power-law curves | literature |
| S52 | المنهج الثاني: البصمات متعددة القنوات | التطابق عبر عدة حساسات يرفع قوة الهوية | fingerprint matching | RC2 |
| S53 | المنهج الثالث: الحل العكسي المشترك | تركيز واحد يجب أن يفسر المصفوفة كلها | observed vs predicted + loss | RC2 |
| S54 | المنهج الرابع: QSPR | البنية الجزيئية تدعم الاستدلال عند غياب منحنيات مباشرة | molecule → descriptors → sensor priors | RC2 / M1 |
| S55 | المنهج الخامس: التحليل الزمني | شكل الاستجابة والاستقرار جزء من القرار | temporal feature overlay | RC2 |
| S56 | المنهج السادس: دمج الأدلة | الأدلة المستقلة والداعمة تُجمع مع منع العد المكرر | evidence fusion graphic | RC2 |

## G. الذكاء المفتوح، AMCI، وعدم اليقين

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S57 | لماذا نحتاج Open-Set؟ | أقرب مرشح ليس بالضرورة المادة الصحيحة | forced-classification comparison | OOD |
| S58 | Mahalanobis وOOD | المسافة الإحصائية تحدد هل العينة داخل المجال المعروف | 2D ellipse plot | RC2 |
| S59 | Fail-Closed | ضعف التغطية أو OOD أو التعارض يمكن أن يوقف الهوية | gate diagram | RC2 |
| S60 | AMCI — التحليل الكيميائي التكيفي | النظام يستطيع أن يقرر القياس التالي بناء على الغموض والاستقرار | closed-loop wheel | RC2 research policy |
| S61 | آلة حالات ChemCore | الجلسة تنتقل من التهيئة حتى التقرير والاسترجاع | state machine | RC2 |
| S62 | التمديد والتكرار والاسترجاع | القياس ليس نافذة زمنية جامدة | adaptive timeline | AMCI |
| S63 | عدم اليقين ليس رقمًا واحدًا | residual/domain/evidence quality كلها تؤثر في تفسير النتيجة | uncertainty layers | RC2 |
| S64 | الخلائط والتداخلات | الحارس يمنع الادعاء الزائد عندما لا يفسر مرشح نقي البيانات جيدًا | pure vs ambiguous vs mixture | RC2 |

## H. سلطة الدليل والنتيجة النهائية

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S65 | سلم الأدلة M0–M4 | قوة الدليل تتدرج من فهرسي إلى تحقق جهاز HazRob | evidence pyramid | RC2 |
| S66 | M3 ليس M4 | هوية متسقة عبر المصفوفة لا تعني صلاحية PPM احترافية | split gate graphic | RC2 |
| S67 | متى يسمح بإظهار PPM؟ | QUANTIFIED يتطلب M4 + مجال صالح + صحة معايرة | decision gates | RC2 |
| S68 | Lineage-Aware Evidence Fusion | الأدلة المشتقة من المصدر نفسه لا تُعد براهين مستقلة | lineage tree | RC2 |
| S69 | Outcomes بدل إجبار النتيجة | Identified/Quantified/Screening/Unknown/Insufficient/Mixture/Invalid | outcome wheel | RC2 |
| S70 | من النتيجة إلى SmartReport | التقرير يحفظ الهوية/المرشحين/الأدلة/المبررات والحدود | result envelope → report | RC2 |

## I. القيمة الابتكارية والتطبيقية والخاتمة

| ID | عنوان الشريحة | الرسالة الأساسية | المرئي المقترح | المصدر/الحالة |
|---|---|---|---|---|
| S71 | خريطة العائلات الابتكارية المرشحة للحماية | جوهر الحماية في **تكامل المنظومة متعددة الأساليب** مع عناصرها الداعمة | patent candidate map | P3 / MASTER+PUBLIC-HIGHLEVEL |
| S72 | التطبيقات وقابلية التوسع | صناعات، مخازن، أماكن محصورة، تسربات، طوارئ، بيئة، بحث | application mosaic | project |
| S73 | خارطة التطوير والتحقق | OOD validation → M4 campaigns → mixtures → multi-device → productization | roadmap/TRL | project |
| S74 | المراجع والمصادر — APA 7 | توثيق المصادر العلمية والسلطوية والمنتجات | clean reference layout؛ قد تمتد بصريًا إلى ملحق عند الحاجة | P1/P2 |
| S75 | شكرًا لكم | HazRob — حماية الأرواح والممتلكات والبيئة عبر الاستشعار والتحليل الذكي | logo + robot hero image | PUBLIC |

## Presenter-slide note
S02 remains intentionally sparse. Do not populate biographies, qualifications, affiliations, photos or contact details until the project owners supply/approve them.

## Cost-slide note
The `<200 USD` target is a **prototype cost target** and must not be displayed as a verified final BOM until P5/P7 establishes what the number includes: chassis, sensors, controllers, motor driver, power regulation, chamber/fan, wiring/enclosure, phone excluded/included, labor, calibration gases, and communications hardware.

## Public-disclosure note
Slides S21–S29 and S71 should present the integrated inventive concept at high level. Exact solver constants, AMCI thresholds, evidence weights, OOD thresholds, security details, unpublished calibration recipes/results, and claim-boundary internals remain MASTER-ONLY pending P8.

## P4 completion decision
P4 is complete. The narrative is frozen at **75 master slides** and centered on the integrated innovation requested by the project owners:
`multi-method analysis → broad chemical hypothesis coverage → evidence-governed result → exposure/hazard linkage → remote operation → experimental laboratory/calibration → governed updates → low-cost target`.

**Next phase: P5 — visual specification and figure/diagram production.**