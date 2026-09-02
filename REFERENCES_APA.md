# HazRob — Research References and Source Provenance

This document is the research-reference baseline for **HazRob 0.8.0**. It records manufacturer documentation, authoritative occupational-safety data, standards, software/model provenance, and peer-reviewed literature used to design, interpret, validate, or discuss the HazRob sensing platform. References are formatted as closely as practicable to **APA 7th edition**.

> **Research-integrity rule:** manufacturer sensitivity graphs, literature-derived regressions, community curve fits, and sensor-response-derived targets are research evidence; they are not substitutes for traceable calibration of the individual physical sensor. HazRob therefore separates chemical identity/safety knowledge, sensor affinity, research curves, experimental calibration, and validated quantitative PPM.

## 1. Primary MQ sensor manufacturer documentation

Zhengzhou Winsen Electronics Technology Co., Ltd. (2021). *Flammable gas sensor (Model MQ-2): Manual* (Version 1.6; valid from July 1, 2021). https://www.winsen-sensor.com/d/files/manual/mq-2.pdf

Zhengzhou Winsen Electronics Technology Co., Ltd. (2018). *Alcohol gas sensor (Model MQ-3B): Manual* (Version 1.5; valid from April 1, 2018). https://www.winsen-sensor.com/d/files/manual/mq-3b.pdf

Zhengzhou Winsen Electronics Technology Co., Ltd. (2024). *Flammable gas sensor (Model MQ-4): Manual* (Version 1.7; valid from January 22, 2024). https://www.winsen-sensor.com/d/files/manual/mq-4.pdf

Zhengzhou Winsen Electronics Technology Co., Ltd. (2021). *Flammable gas sensor (Model MQ-5): Manual* (Version 1.6; valid from July 1, 2021). https://www.winsen-sensor.com/d/files/manual/mq-5.pdf

Zhengzhou Winsen Electronics Technology Co., Ltd. (2021). *Carbon monoxide gas sensor (Model MQ-7B): Manual* (Version 1.6; valid from July 1, 2021). https://www.winsen-sensor.com/d/files/manual/mq-7b.pdf

Zhengzhou Winsen Electronics Technology Co., Ltd. (2021). *Flammable gas sensor (Model MQ-8): Manual* (Version 1.6; valid from July 1, 2021). https://www.winsen-sensor.com/d/files/manual/mq-8.pdf

Zhengzhou Winsen Electronics Technology Co., Ltd. (2021). *Toxic gas sensor (Model MQ-9B): Manual* (Version 1.6; valid from July 1, 2021). https://www.winsen-sensor.com/d/files/manual/mq-9b.pdf

Zhengzhou Winsen Electronics Technology Co., Ltd. (2021). *Air quality gas sensor (Model MQ135): Manual* (Version 1.6; valid from July 1, 2021). https://www.winsen-sensor.com/d/files/manual/mq135.pdf

### Manufacturer product pages used for target-gas and range cross-checking

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ-2 semiconductor sensor for smoke & flammable gas*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq-2.html

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ-3B semiconductor sensor for alcohol gas*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq-3b.html

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ-4 MOS flammable gas sensor for methane CH4 detector*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq-4.html

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ-5 semiconductor sensor for flammable gas*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq-5.html

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ-7B semiconductor sensor for carbon monoxide*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq-7b.html

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ-8 hydrogen H2 gas sensor*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq-8.html

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ-9B MOS sensor for carbon monoxide CO gas*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq-9b.html

Zhengzhou Winsen Electronics Technology Co., Ltd. (n.d.). *MQ135 semiconductor sensor for air quality*. Retrieved August 28, 2026, from https://www.winsen-sensor.com/product/mq135.html

## 2. MQ curve extraction, calibration software, and model provenance

Califa Urquiza, M. A., Contreras Contreras, G., & Carrillo Amado, Y. R. (2019). *miguel5612/MQSensorsLib: Arduino Preview V1.03* (Version 1.0.3) [Computer software]. Zenodo. https://doi.org/10.5281/zenodo.3384301

MQSensorsLib project repository: https://github.com/miguel5612/MQSensorsLib

abcdaaaaaaaaa. (2026). *MQDataScience* (Version 6.0.0) [Computer software]. GitHub. https://github.com/abcdaaaaaaaaa/MQDataScience

**Pinned MQDataScience snapshot used by HazRob:** commit `e88f876140587a708ee84a4d4d3acbd5720274ee`. The complete upstream catalog of **76 models is adopted** by HazRob 0.8. Runtime implementation is tracked separately as `UPSTREAM-VERIFIED`, `APPROXIMATION`, or `PORT PENDING`; adoption of a model does not imply that it is already executed natively on Android.

## 3. Peer-reviewed MQ/MOX calibration, measurement, sensing, IoT, and sensor-fusion literature

Carrillo-Amado, Y. R., Califa-Urquiza, M. A., & Ramón-Valencia, J. A. (2020). Calibration and standardization of air quality measurements using MQ sensors. *Respuestas, 25*(1), 70–77. https://doi.org/10.22463/0122820X.2408

Kaunkid, S., Aurasopon, A., Khamsen, W., Takeang, C., Piladaeng, N., & Lloret, J. (2024). Resistive measurement method for MQ sensors based on ADCs of microcontrollers. *IEEE Access, 12*, 144364–144376. https://doi.org/10.1109/ACCESS.2024.3472697

Honeycutt, W. T., Ley, M. T., & Materer, N. F. (2019). Precision and limits of detection for selected commercially available, low-cost carbon dioxide and methane gas sensors. *Sensors, 19*(14), 3157. https://doi.org/10.3390/s19143157

Easterline, L. M., Putri, A. A.-Z. R., Atmaja, P. S., Dewi, A. L., & Prasetyo, A. (2024). Smart air monitoring with IoT-based MQ-2, MQ-7, MQ-8, and MQ-135 sensors using NodeMCU ESP32. *Procedia Computer Science, 245*, 815–824. https://doi.org/10.1016/j.procs.2024.10.308

Sari, I. P., Yulitha, K. R., Lailatushifa, Saputra, M. A., Nadia Ita P., Wahyuni, A., & Hayat, M. (2026). Detecting potential gas hazards in laboratory environments with Arduino Uno and MQ multigas sensors. *Journal of Natural Sciences and Applications, 1*(1), 1–12. https://doi.org/10.58421/jnsa.v1i1.1263

> The published author string **“Nadia Ita P”** is retained because the source does not make the family-name parsing sufficiently clear to normalize it more aggressively without risking a citation error.

Poeta, E., Núñez-Carmona, E., & Sberveglieri, V. (2025). A review: Applications of MOX sensors from air quality monitoring to biomedical diagnosis and agro-food quality control. *Journal of Sensor and Actuator Networks, 14*(3), 50. https://doi.org/10.3390/jsan14030050

Zong, B., Wu, S., Yang, Y., Li, Q., Tao, T., & Mao, S. (2025). Smart gas sensors: Recent developments and future prospective. *Nano-Micro Letters, 17*, 54. https://doi.org/10.1007/s40820-024-01543-w

Nasution, T. I., Asrosa, R., & Nainggolan, I. (2018). Application of MQ-138 semiconductor sensor for breath acetone detection. *Journal of Physics: Conference Series, 1116*, 032023. https://doi.org/10.1088/1742-6596/1116/3/032023

Mousa, A. J., ALsobhani, A., & Abdul-Rahaim, L. A. (2026). Sensor-response-based multi-gas classification using low-cost MQ sensors and a lightweight neural network. *Results in Engineering, 32*, 112284. https://doi.org/10.1016/j.rineng.2026.112284

Ahmad, A., Sendra, S., Lloret, J., Bai, J., Rosskopf, E., & Di Gioia, F. (2026). The promise of low-cost metal-oxide semiconductor gas sensors for precision agriculture. *Advanced Sensor Research, 5*, e00112. https://doi.org/10.1002/adsr.202500112

### How these papers should be used in a HazRob publication

- **Carrillo-Amado et al. (2020)** supports discussion of Rs/R0-based mathematical calibration and conversion of manufacturer curves into computable models; it does not prove calibration of the individual HazRob sensors.
- **Kaunkid et al. (2024)** is directly relevant to resistance-measurement uncertainty, ADC error, reference resistance, and supply-voltage effects. It is especially useful when discussing the reason for using external ADC acquisition and careful electrical characterization.
- **Honeycutt et al. (2019)** supports controlled-gas calibration, precision, limit-of-detection, baseline drift, and the importance of environmental control for low-cost chemiresistive sensors.
- **Easterline et al. (2024)** and **Sari et al. (2026)** are system/application references for multi-MQ monitoring and microcontroller-based warning architectures. Claims in those papers about target gases should not replace the appropriate manufacturer manual.
- **Poeta et al. (2025)** and **Zong et al. (2025)** are broad contemporary review sources for MOX sensing, cross-sensitivity, temperature/humidity effects, smart sensor arrays, data integrity, AI and IoT integration.
- **Nasution et al. (2018)** is a useful example of an MQ/MOS sensor in a specialized acetone application, but MQ-138 is not part of the current HazRob eight-sensor array.
- **Mousa et al. (2026)** is particularly relevant to HazRob's sensor-fusion direction because it explicitly distinguishes manufacturer-curve-derived concentration targets from certified reference-grade concentrations.
- **Ahmad et al. (2026)** supports broader discussion of low-cost MOS sensor arrays, E-nose approaches, AI/ML, selectivity, drift, durability, signal stability, and the need for robust labeled datasets.

## 4. Statistical and machine-learning methodological references

Hoerl, A. E., & Kennard, R. W. (1970). Ridge regression: Biased estimation for nonorthogonal problems. *Technometrics, 12*(1), 55–67. https://doi.org/10.1080/00401706.1970.10488634

Pedregosa, F., Varoquaux, G., Gramfort, A., Michel, V., Thirion, B., Grisel, O., Blondel, M., Prettenhofer, P., Weiss, R., Dubourg, V., VanderPlas, J., Passos, A., Cournapeau, D., Brucher, M., Perrot, M., & Duchesnay, É. (2011). Scikit-learn: Machine learning in Python. *Journal of Machine Learning Research, 12*, 2825–2830. https://jmlr.org/papers/v12/pedregosa11a.html

Seabold, S., & Perktold, J. (2010). Statsmodels: Econometric and statistical modeling with Python. In *Proceedings of the 9th Python in Science Conference* (pp. 92–96). https://doi.org/10.25080/Majora-92bf1922-011

Chen, T., & Guestrin, C. (2016). XGBoost: A scalable tree boosting system. In *Proceedings of the 22nd ACM SIGKDD International Conference on Knowledge Discovery and Data Mining* (pp. 785–794). Association for Computing Machinery. https://doi.org/10.1145/2939672.2939785

Ke, G., Meng, Q., Finley, T., Wang, T., Chen, W., Ma, W., Ye, Q., & Liu, T.-Y. (2017). LightGBM: A highly efficient gradient boosting decision tree. In *Advances in Neural Information Processing Systems 30* (pp. 3146–3154). https://proceedings.neurips.cc/paper/2017/hash/6449f44a102fde848669bdd9eb6b76fa-Abstract.html

Prokhorenkova, L., Gusev, G., Vorobev, A., Dorogush, A. V., & Gulin, A. (2018). CatBoost: Unbiased boosting with categorical features. In *Advances in Neural Information Processing Systems 31*. https://proceedings.neurips.cc/paper/2018/hash/14491b756b3a51daac41c24863285549-Abstract.html

## 5. Occupational exposure limits and chemical identity

Centers for Disease Control and Prevention, National Institute for Occupational Safety and Health. (2020). *NIOSH Pocket Guide to Chemical Hazards*. https://www.cdc.gov/niosh/npg/default.html

Centers for Disease Control and Prevention, National Institute for Occupational Safety and Health. (n.d.). *Immediately Dangerous to Life or Health (IDLH) values*. Retrieved August 28, 2026, from https://www.cdc.gov/niosh/idlh/default.html

Occupational Safety and Health Administration. (n.d.). *Permissible exposure limits—OSHA annotated table Z-1*. Retrieved August 28, 2026, from https://www.osha.gov/annotated-pels/table-z-1

Occupational Safety and Health Administration. (n.d.). *Permissible exposure limits—OSHA annotated table Z-2*. Retrieved August 28, 2026, from https://www.osha.gov/annotated-pels/table-z-2

Occupational Safety and Health Administration. (n.d.). *Air contaminants, 29 C.F.R. § 1910.1000*. Retrieved August 28, 2026, from https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1000

National Institute of Standards and Technology. (n.d.). *NIST Chemistry WebBook, SRD 69*. https://webbook.nist.gov/chemistry/

National Library of Medicine. (n.d.). *PubChem*. https://pubchem.ncbi.nlm.nih.gov/

HazRob's `VERY_SAFE / SAFE / ALERT / DANGER / CRITICAL` bands are project-defined screening categories. They must not be described as OSHA or NIOSH categories.

## 6. Environmental sensor documentation

Bosch Sensortec GmbH. (2016). *BME280 combined humidity and pressure sensor: Final data sheet* (Revision 1.3; Document BST-BME280-DS001-12). Bosch Sensortec. https://www.bosch-sensortec.com/products/environmental-sensors/humidity-sensors-bme280/

## 7. ESP32 / ESP32-CAM implementation references

Espressif Systems. (n.d.). *Arduino-ESP32 CameraWebServer example* [Source code]. GitHub. Retrieved August 28, 2026, from https://github.com/espressif/arduino-esp32/tree/master/libraries/ESP32/examples/Camera/CameraWebServer

Espressif Systems. (n.d.). *CameraWebServer.ino* [Source code]. GitHub. Retrieved August 28, 2026, from https://github.com/espressif/arduino-esp32/blob/master/libraries/ESP32/examples/Camera/CameraWebServer/CameraWebServer.ino

Espressif Systems. (n.d.). *Arduino-ESP32 WebServer examples* [Source code]. GitHub. Retrieved August 28, 2026, from https://github.com/espressif/arduino-esp32/tree/master/libraries/WebServer/examples

The HazRob robot-control API (`/drive`, `/stop`, `/diag`, `/measure`, `/relay`, `/cfg`) is project-specific firmware and is not an Espressif standard API.

## 8. Android and report-format references

Google. (n.d.). *ConnectivityManager* [Android API reference]. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/reference/android/net/ConnectivityManager

Google. (n.d.). *Access documents and other files from shared storage*. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/training/data-storage/shared/documents-files

Google. (n.d.). *PdfDocument* [Android API reference]. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/reference/android/graphics/pdf/PdfDocument

Google. (n.d.). *SQLiteOpenHelper* [Android API reference]. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper

Google. (n.d.). *Sign your app*. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/studio/publish/app-signing

Ecma International. (2021). *ECMA-376: Office Open XML file formats* (5th ed.). https://ecma-international.org/publications-and-standards/standards/ecma-376/

## 9. Build and continuous-integration references

GitHub. (n.d.). *actions/upload-artifact* [Computer software]. GitHub. Retrieved August 28, 2026, from https://github.com/actions/upload-artifact

GitHub. (n.d.). *GitHub Actions documentation*. Retrieved August 28, 2026, from https://docs.github.com/actions

## 10. Engineering, calibration, and hazardous-area standards

International Organization for Standardization. (2010). *ISO 12100:2010 Safety of machinery—General principles for design—Risk assessment and risk reduction*. https://www.iso.org/standard/51528.html

International Electrotechnical Commission. (2021). *IEC 60204-1:2016+AMD1:2021 CSV: Safety of machinery—Electrical equipment of machines—Part 1: General requirements* (Edition 6.1). https://webstore.iec.ch/en/publication/71256

International Electrotechnical Commission. (2013). *IEC 60529:1989+AMD1:1999+AMD2:2013 CSV: Degrees of protection provided by enclosures (IP Code)* (Edition 2.2). https://webstore.iec.ch/en/publication/2452

International Electrotechnical Commission. (2026). *IEC 60079-0:2026: Explosive atmospheres—Part 0: Equipment—General requirements* (Edition 8.0). https://webstore.iec.ch/en/publication/71519

International Electrotechnical Commission. (2020). *IEC 60079-10-1:2020: Explosive atmospheres—Part 10-1: Classification of areas—Explosive gas atmospheres* (Edition 3.0). https://webstore.iec.ch/en/publication/63327

International Electrotechnical Commission. (2023). *IEC 60079-11:2023: Explosive atmospheres—Part 11: Equipment protection by intrinsic safety “i”* (Edition 7.0). https://webstore.iec.ch/en/publication/60654

International Organization for Standardization, & International Electrotechnical Commission. (2017). *ISO/IEC 17025:2017 General requirements for the competence of testing and calibration laboratories* (3rd ed.). https://www.iso.org/standard/66912.html

International Organization for Standardization. (2015). *ISO 6142-1:2015 Gas analysis—Preparation of calibration gas mixtures—Part 1: Gravimetric method for Class I mixtures*. https://www.iso.org/standard/59631.html

International Organization for Standardization. (2019). *ISO 6145-1:2019 Gas analysis—Preparation of calibration gas mixtures using dynamic methods—Part 1: General aspects* (3rd ed.). https://www.iso.org/standard/53598.html

International Electrotechnical Commission. (2022). *IEC 62619:2022 Secondary cells and batteries containing alkaline or other non-acid electrolytes—Safety requirements for secondary lithium cells and batteries, for use in industrial applications* (Edition 2.0). https://webstore.iec.ch/en/publication/64073

International Organization for Standardization. (2023). *ISO 13849-1:2023 Safety of machinery—Safety-related parts of control systems—Part 1: General principles for design* (4th ed.). https://www.iso.org/standard/73481.html

> Referencing IEC 60079 does **not** mean that the current HazRob prototype is Ex-certified. Any hazardous-area claim requires the applicable protection concept, verified design, testing, documentation, and certification.

## 11. Internal project sources and software citation

The application contains historical HazRob/SERVOC chemical catalogs, physical-measurement definitions, MQ evidence/curve records, consensus logic and project-specific control/analysis algorithms. These are internal project artifacts, not independent scientific authorities.

Suggested software citation for the current baseline:

Sohpany. (2026). *HazRob Android application* (Version 0.8.0) [Computer software]. GitHub. https://github.com/sohpany/SERVOC-Android

## 12. Recommended citation hierarchy for a journal paper or conference paper

1. **Manufacturer manual** for the sensor's electrical conditions, heater protocol, nominal range and typical sensitivity behavior.
2. **Traceable calibration standard/method and actual experiment record** for quantitative concentration claims.
3. **NIOSH/OSHA primary sources** for occupational exposure values and IDLH information.
4. **ISO/IEC standards** for calibration, safety, enclosure, battery or hazardous-area design claims.
5. **Peer-reviewed experimental/review literature** for MOS/MQ measurement behavior, uncertainty, drift, sensor fusion and AI/ML methodology.
6. **MQSensorsLib / MQDataScience** for software implementation provenance and community-derived curve/model methods.
7. **HazRob source code and exact Git commit** for project-specific integration, data acquisition, analytical logic and UI/report behavior.

## 13. Version-control provenance

For reproducibility, publications should record the exact HazRob release and Git commit used for data acquisition and analysis. This reference baseline was updated for **HazRob v0.8.0** on **September 2, 2026**. MQDataScience is pinned to version **6.0.0**, commit **e88f876140587a708ee84a4d4d3acbd5720274ee**. The authoritative NIOSH/OSHA datasets bundled with the application include provenance manifests and SHA-256 integrity checks performed by CI before APK build.
