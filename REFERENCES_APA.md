# HazRob / SERVOC — Research References and Source Provenance

This document records the external sources, technical documentation, standards, software libraries, and research references used in the HazRob/SERVOC project. References are formatted as closely as practicable to **APA 7th edition**. Dynamic web resources include a retrieval date where useful. Versioned manuals and software are cited with the exact version used by the project when known.

> **Research-integrity note:** HazRob combines manufacturer documentation, public occupational-exposure databases, community-derived MQ response-curve fits, and project-specific algorithms. A manufacturer sensitivity graph or a community regression must not be presented as an individual-sensor calibration certificate. Likewise, HazRob's five-level color visualization is a project-defined screening layer; the underlying TWA/STEL/Ceiling/IDLH/LEL values are sourced separately below.

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

## 2. MQ curve extraction, regression, and community software sources

Califa Urquiza, M. A., Contreras Contreras, G., & Carrillo Amado, Y. R. (2019). *miguel5612/MQSensorsLib: Arduino Preview V1.03* (Version 1.0.3) [Computer software]. Zenodo. https://doi.org/10.5281/zenodo.3384301

MQSensorsLib project repository: https://github.com/miguel5612/MQSensorsLib

abcdaaaaaaaaa. (2026). *MQDataScience* (Version 6.0.0) [Computer software]. GitHub. https://github.com/abcdaaaaaaaaa/MQDataScience

**Pinned MQDataScience snapshot used by HazRob:** commit `e88f876140587a708ee84a4d4d3acbd5720274ee`. HazRob uses this source for comparison power-law curves, temperature/relative-humidity correction research models, and the catalog of 76 regression/machine-learning models. The majority of those 76 models are registered as upstream models and are not all native Android runtime implementations in HazRob v0.4.0.

## 3. Statistical and machine-learning methodological references

The following references are useful when describing the algorithms represented in HazRob and/or the upstream MQDataScience model catalog. They are methodological references rather than proof that every model is currently executed natively in the Android application.

Hoerl, A. E., & Kennard, R. W. (1970). Ridge regression: Biased estimation for nonorthogonal problems. *Technometrics, 12*(1), 55–67. https://doi.org/10.1080/00401706.1970.10488634

Pedregosa, F., Varoquaux, G., Gramfort, A., Michel, V., Thirion, B., Grisel, O., Blondel, M., Prettenhofer, P., Weiss, R., Dubourg, V., VanderPlas, J., Passos, A., Cournapeau, D., Brucher, M., Perrot, M., & Duchesnay, É. (2011). Scikit-learn: Machine learning in Python. *Journal of Machine Learning Research, 12*, 2825–2830. https://jmlr.org/papers/v12/pedregosa11a.html

Seabold, S., & Perktold, J. (2010). Statsmodels: Econometric and statistical modeling with Python. In *Proceedings of the 9th Python in Science Conference* (pp. 92–96). https://doi.org/10.25080/Majora-92bf1922-011

Chen, T., & Guestrin, C. (2016). XGBoost: A scalable tree boosting system. In *Proceedings of the 22nd ACM SIGKDD International Conference on Knowledge Discovery and Data Mining* (pp. 785–794). Association for Computing Machinery. https://doi.org/10.1145/2939672.2939785

Ke, G., Meng, Q., Finley, T., Wang, T., Chen, W., Ma, W., Ye, Q., & Liu, T.-Y. (2017). LightGBM: A highly efficient gradient boosting decision tree. In *Advances in Neural Information Processing Systems 30* (pp. 3146–3154). https://proceedings.neurips.cc/paper/2017/hash/6449f44a102fde848669bdd9eb6b76fa-Abstract.html

Prokhorenkova, L., Gusev, G., Vorobev, A., Dorogush, A. V., & Gulin, A. (2018). CatBoost: Unbiased boosting with categorical features. In *Advances in Neural Information Processing Systems 31*. https://proceedings.neurips.cc/paper/2018/hash/14491b756b3a51daac41c24863285549-Abstract.html

## 4. Occupational exposure limits, IDLH, and flammability screening

Centers for Disease Control and Prevention, National Institute for Occupational Safety and Health. (2020). *NIOSH Pocket Guide to Chemical Hazards*. https://www.cdc.gov/niosh/npg/default.html

Centers for Disease Control and Prevention, National Institute for Occupational Safety and Health. (n.d.). *Immediately Dangerous to Life or Health (IDLH) values*. Retrieved August 28, 2026, from https://www.cdc.gov/niosh/idlh/default.html

Occupational Safety and Health Administration. (n.d.). *Permissible exposure limits—OSHA annotated table Z-1*. Retrieved August 28, 2026, from https://www.osha.gov/annotated-pels/table-z-1

Occupational Safety and Health Administration. (n.d.). *Permissible exposure limits—OSHA annotated table Z-2*. Retrieved August 28, 2026, from https://www.osha.gov/annotated-pels/table-z-2

Occupational Safety and Health Administration. (n.d.). *Air contaminants, 29 C.F.R. § 1910.1000*. Retrieved August 28, 2026, from https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1000

### Important interpretation note

HazRob's `VERY_SAFE / SAFE / ALERT / DANGER / CRITICAL` color bands are **project-defined screening categories** calculated from referenced exposure/flammability values. They are not themselves OSHA or NIOSH categories and must not be cited as such. Regulatory or occupational-hygiene conclusions must be based on the original agency limits and an appropriately calibrated and validated measurement chain.

## 5. Environmental sensor documentation

Bosch Sensortec GmbH. (2016). *BME280 combined humidity and pressure sensor: Final data sheet* (Revision 1.3; Document BST-BME280-DS001-12). Bosch Sensortec. https://www.bosch-sensortec.com/products/environmental-sensors/humidity-sensors-bme280/

The BME280 is used in the robot firmware for ambient temperature, relative humidity, and barometric pressure. HazRob also derives an approximate altitude from barometric pressure for display; that derived altitude is not a direct BME280 measurement.

## 6. ESP32 / ESP32-CAM implementation references

Espressif Systems. (n.d.). *Arduino-ESP32 CameraWebServer example* [Source code]. GitHub. Retrieved August 28, 2026, from https://github.com/espressif/arduino-esp32/tree/master/libraries/ESP32/examples/Camera/CameraWebServer

Espressif Systems. (n.d.). *CameraWebServer.ino* [Source code]. GitHub. Retrieved August 28, 2026, from https://github.com/espressif/arduino-esp32/blob/master/libraries/ESP32/examples/Camera/CameraWebServer/CameraWebServer.ino

Espressif Systems. (n.d.). *Arduino-ESP32 WebServer examples* [Source code]. GitHub. Retrieved August 28, 2026, from https://github.com/espressif/arduino-esp32/tree/master/libraries/WebServer/examples

These sources informed compatibility with the standard ESP32-CAM MJPEG streaming/control pattern. The HazRob robot-control API itself (`/drive`, `/stop`, `/diag`, `/measure`, `/relay`, `/cfg`) is project-specific firmware rather than an Espressif standard API.

## 7. Android platform and application implementation references

Google. (n.d.). *ConnectivityManager* [Android API reference]. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/reference/android/net/ConnectivityManager

Google. (n.d.). *Access documents and other files from shared storage*. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/training/data-storage/shared/documents-files

Google. (n.d.). *PdfDocument* [Android API reference]. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/reference/android/graphics/pdf/PdfDocument

Google. (n.d.). *SQLiteOpenHelper* [Android API reference]. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper

Google. (n.d.). *Sign your app*. Android Developers. Retrieved August 28, 2026, from https://developer.android.com/studio/publish/app-signing

The Android signing reference is specifically relevant to HazRob's fixed prototype signing certificate and the requirement that later APK versions retain a compatible signing identity to install as updates over earlier HazRob versions.

## 8. XLSX / report-generation format reference

Ecma International. (2021). *ECMA-376: Office Open XML file formats* (5th ed.). https://ecma-international.org/publications-and-standards/standards/ecma-376/

HazRob creates `.xlsx` workbooks using the Office Open XML packaging/SpreadsheetML structure rather than exporting a CSV file with an `.xlsx` extension.

## 9. Build and continuous-integration references

GitHub. (n.d.). *actions/upload-artifact* [Computer software]. GitHub. Retrieved August 28, 2026, from https://github.com/actions/upload-artifact

GitHub. (n.d.). *GitHub Actions documentation*. Retrieved August 28, 2026, from https://docs.github.com/actions

HazRob's CI builds the signed APK, runs regression tests, verifies the APK signing certificate, and uploads the release artifact.

## 10. Engineering and calibration standards referenced in the broader HazRob system design

These standards are design/reference standards for the wider robot, calibration workflow, enclosure, electrical safety, battery system, and potential hazardous-area deployment. Compliance with a standard must not be claimed merely because the standard was consulted.

International Organization for Standardization. (2010). *ISO 12100:2010 Safety of machinery—General principles for design—Risk assessment and risk reduction*. https://www.iso.org/standard/51528.html

International Electrotechnical Commission. (2021). *IEC 60204-1:2016+AMD1:2021 CSV: Safety of machinery—Electrical equipment of machines—Part 1: General requirements* (Edition 6.1). https://webstore.iec.ch/en/publication/71256

International Electrotechnical Commission. (2013). *IEC 60529:1989+AMD1:1999+AMD2:2013 CSV: Degrees of protection provided by enclosures (IP Code)* (Edition 2.2). https://webstore.iec.ch/en/publication/2452

International Electrotechnical Commission. (2026). *IEC 60079-0:2026: Explosive atmospheres—Part 0: Equipment—General requirements* (Edition 8.0). https://webstore.iec.ch/en/publication/71519

International Electrotechnical Commission. (2020). *IEC 60079-10-1:2020: Explosive atmospheres—Part 10-1: Classification of areas—Explosive gas atmospheres* (Edition 3.0). https://webstore.iec.ch/en/publication/63327

International Electrotechnical Commission. (2023). *IEC 60079-11:2023: Explosive atmospheres—Part 11: Equipment protection by intrinsic safety “i”* (Edition 7.0; corrected version incorporating later corrigenda/interpretation sheets where applicable). https://webstore.iec.ch/en/publication/60654

International Organization for Standardization, & International Electrotechnical Commission. (2017). *ISO/IEC 17025:2017 General requirements for the competence of testing and calibration laboratories* (3rd ed.). https://www.iso.org/standard/66912.html

International Organization for Standardization. (2015). *ISO 6142-1:2015 Gas analysis—Preparation of calibration gas mixtures—Part 1: Gravimetric method for Class I mixtures*. https://www.iso.org/standard/59631.html

International Organization for Standardization. (2019). *ISO 6145-1:2019 Gas analysis—Preparation of calibration gas mixtures using dynamic methods—Part 1: General aspects* (3rd ed.). https://www.iso.org/standard/53598.html

International Electrotechnical Commission. (2022). *IEC 62619:2022 Secondary cells and batteries containing alkaline or other non-acid electrolytes—Safety requirements for secondary lithium cells and batteries, for use in industrial applications* (Edition 2.0). https://webstore.iec.ch/en/publication/64073

International Organization for Standardization. (2023). *ISO 13849-1:2023 Safety of machinery—Safety-related parts of control systems—Part 1: General principles for design* (4th ed.). https://www.iso.org/standard/73481.html

### Hazardous-area status note

The current prototype ESP32/ESP32-CAM/MQ/repeater/battery architecture is **not automatically Ex-certified** by referencing IEC 60079 documents. Any future claim of suitability for explosive atmospheres requires an appropriate protection concept, component selection, design verification, testing, documentation, and certification against the applicable current standards.

## 11. Internal project sources

The Android application also contains material migrated from the earlier SERVOC ESP32 firmware, including the original chemical catalog, physical measurement catalog, MQ curve records, consensus logic, exposure-limit table, sensor-acquisition contract, and HTTP control endpoints. These are **internal project artifacts**, not independent external scientific references. Where those internal files themselves derive data from external sources, the external source should be cited from the sections above.

Suggested software citation for the current application:

Sohpany. (2026). *HazRob Android application* (Version 0.4.0) [Computer software]. GitHub. https://github.com/sohpany/SERVOC-Android

## 12. Recommended citation hierarchy for papers/theses

When writing a research paper based on HazRob, prefer citations in this order:

1. **Manufacturer manual** for sensor target gas, electrical conditions, heater protocol, detection range, preheat, and typical sensitivity behavior.
2. **NIOSH/OSHA primary sources** for occupational exposure values and IDLH information.
3. **ISO/IEC/IEC standards** for calibration, machinery safety, electrical safety, enclosure/IP, battery, or explosive-atmosphere design claims.
4. **Peer-reviewed publications / canonical algorithm references** for statistical or machine-learning methods.
5. **MQSensorsLib / MQDataScience** for community digitization, fitted response equations, environmental-correction models, and software implementation provenance.
6. **HazRob source code** for project-specific fusion logic, UI risk mapping, experimental calibration workflow, and system integration.

## 13. Version-control provenance

For reproducibility, publications should record the exact HazRob release and, where relevant, the Git commit used for analysis. The reference set in this file was reviewed for HazRob **v0.4.0** on **August 28, 2026**. MQDataScience is explicitly pinned in HazRob source code to version **6.0.0**, commit **e88f876140587a708ee84a4d4d3acbd5720274ee**.
