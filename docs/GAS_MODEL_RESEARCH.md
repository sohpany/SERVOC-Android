# HazRob MQ-array gas-model research roadmap

## Scope

HazRob is intentionally optimized around the eight low-cost sensors already available in the project: MQ-2, MQ-3, MQ-4, MQ-5, MQ-7, MQ-8, MQ-9 and MQ-135. Expensive specialist sensing technologies are not required for the current development track. They may remain future optional extensions only.

The 144-entry chemical catalogue must not be confused with 144 validated quantitative sensor curves. HazRob therefore records model provenance and separates four evidence levels:

1. **Manufacturer / source-backed quantitative curve available** — a response curve exists for the sensor/analyte pair and can be digitized/fitted.
2. **Community digitization of a manufacturer curve** — coefficients such as `ppm = a * (Rs/R0)^b` are available but must be traced back and revalidated.
3. **Empirical HazRob model** — identity and/or concentration model learned from controlled exposures of the actual sensor array.
4. **Unsupported / weak response** — retained in the catalogue but not reported quantitatively.

## 1. Physical MQ conversion

For the conventional load-resistor circuit:

`Rs = (Vc / Vrl - 1) * RL`

The normalized response is:

`r = Rs / R0`

Many MQ sensitivity graphs are approximately linear in log-log coordinates over their stated range. A convenient fitted model is:

`log10(ppm) = log10(a) + b * log10(r)`

or equivalently:

`ppm = a * r^b`

This is the model already used by the migrated HazRob `GasEngine` and by the widely used open-source MQUnifiedsensor library. HazRob must store the usable ppm range and source with every coefficient and must not silently extrapolate beyond that range.

### Curve digitization workflow

For each manufacturer sensitivity plot:

1. identify sensor revision and test conditions;
2. digitize multiple points on the logarithmic graph, not only two points;
3. fit a robust linear regression in `(log10(r), log10(ppm))` space;
4. calculate residual error / R2 and retain the original digitized points;
5. reject extrapolation outside the documented concentration range;
6. validate against the actual purchased sensor using known calibration concentrations before enabling quantitative alarms.

This process can enlarge the current direct-curve library without purchasing new sensors, but only for analytes actually represented by credible response curves.

## 2. Treat cross-sensitivity as an electronic-nose fingerprint

The low selectivity of MQ/MOX sensors is not only a limitation. Research electronic noses deliberately combine overlapping sensor responses and learn a multidimensional signature. Published work has used the same or nearly the same MQ family, including arrays of MQ-2, MQ-3, MQ-4, MQ-5, MQ-7, MQ-8, MQ-9 and MQ-135.

HazRob should therefore maintain **two separate analytical paths**:

- **Direct ppm path:** use source-backed log-power curves where available.
- **Fingerprint path:** use the entire eight-sensor response vector to identify compounds for which no trustworthy individual curve exists.

Identity classification and ppm regression must remain separate tasks. A classifier may recognize a pattern without being able to determine concentration accurately.

## 3. Temporal fingerprinting with the existing hardware

Steady-state values discard useful information. During exposure and purge, record the complete `Rs/R0` time series for every sensor. The Android analytical core now includes a temporal feature extractor. Per sensor it derives:

- valid-sample fraction;
- baseline log response;
- late/steady response;
- response delta;
- minimum and maximum log response;
- response slope;
- normalized area under the response curve.

Further features can be added later: T10/T50/T90, peak derivative, recovery time, hysteresis, wavelet coefficients and frequency-domain components.

This requires no additional sensor type. It only requires that sampling continues before, during and after exposure. Pump/flow state, temperature and humidity should be stored with every capture because MOX response changes with environmental conditions.

## 4. Machine-learning / chemometric path

Research consistently uses multivariate algorithms to turn cross-reactive sensor arrays into electronic noses. HazRob will keep the algorithm layer modular so models can be compared rather than hard-coded.

Recommended progression for this project:

- PCA: visualization, drift inspection and dimensionality reduction;
- nearest-centroid / kNN: transparent baseline classifier;
- LDA / PLS-DA: compact supervised classification;
- PLSR or regularized regression: concentration estimation in correlated sensor arrays;
- SVM / SVR: nonlinear classification and regression when training data justify it;
- random forest / gradient boosting: robust structured-feature models;
- small neural network: only after a sufficiently large and balanced dataset exists.

HazRob v0.3 introduces a pure-Java ridge concentration model using `log1p(ppm)` so zero-concentration mixture samples can participate in training. It is a baseline regressor, not a claim of laboratory-grade accuracy.

## 5. Mixtures

A mixture cannot in general be decomposed by independently applying pure-gas MQ curves because sensor responses interact and are nonlinear. The scientific route is supervised multi-output regression:

`X = [temporal + steady-state features from all sensors]`

`Y = [concentration gas1, concentration gas2, ...]`

Training must include pure gases, zero-gas baselines and representative mixtures. PLSR, SVR, relevance-vector methods, ridge baselines and neural networks are all candidates reported in the literature. HazRob should report uncertainty and avoid naming components outside the model's trained domain.

## 6. Heater information from the sensors already owned

Most of the current array is normally used with a constant heater. MQ-7/MQ-7B and MQ-9/MQ-9B are notable exceptions: manufacturer documentation specifies high/low heater phases for CO-related operation. If the actual module wiring exposes independent heater control, those manufacturer cycles can become additional time-domain information. This is an optional refinement using the same sensor, not a requirement to buy another sensor.

Do not apply arbitrary heater modulation to modules unless the heater circuit and exact sensor revision have been verified.

## 7. Temperature, humidity, ageing and drift

Manufacturer curves are collected under specified laboratory conditions. HazRob training records should therefore include:

- temperature and relative humidity;
- sensor serial/channel identity;
- R0 and calibration date;
- heater-on duration / warm-up state;
- exposure and purge duration;
- flow state if a sampling pump is used;
- model/library version.

Models should be tested with session-separated or date-separated validation, not random splits only, to reveal sensor drift. Periodic clean-air baselines can be used for normalization and drift tracking.

## 8. Practical 144-chemical target

The realistic goal is **144 catalogue entries with an explicit analytical status**, not fabricated equations for every compound.

For each entry HazRob will eventually store:

- formula and aliases;
- direct MQ curves and their provenance if available;
- documented sensor cross-responses;
- fingerprint-training status;
- concentration-regression status and validated range;
- number of training sessions and calibration points;
- temperature/RH domain;
- classification confusion statistics;
- regression RMSE / MAE / R2;
- exposure-limit data source;
- unsupported or incompatibility warnings.

Compounds with corrosive/oxidizing behavior or no measurable array response remain in the library but must not receive invented ppm results. Controlled work with hazardous gases must use certified standards and appropriate laboratory safety systems; HazRob should never instruct users to generate hazardous calibration gases themselves.

## Key sources

- Winsen MQ-2 manual: https://www.winsen-sensor.com/d/files/newpdf/mq-2-%28ver1_6%29---manual.pdf
- Winsen MQ-4: https://www.winsen-sensor.com/product/mq-4.html
- Winsen MQ-5: https://www.winsen-sensor.com/product/mq-5.html
- Winsen MQ-7B manual: https://www.winsen-sensor.com/d/files/PDF/Semiconductor%20Gas%20Sensor/MQ-7B%20%28Ver1.4%29%20-%20Manual.pdf
- Winsen MQ-8: https://www.winsen-sensor.com/product/mq-8.html
- Winsen MQ-9B manual: https://www.winsen-sensor.com/d/files/manual/mq-9b.pdf
- Winsen MQ135 manual: https://www.winsen-sensor.com/manual/mq135.html
- MQUnifiedsensor equations/examples: https://github.com/miguel5612/MQSensorsLib
- Gas mixture prediction using dynamic MOX response: https://pmc.ncbi.nlm.nih.gov/articles/PMC6780764/
- Mixed-gas recognition and concentration estimation with MOS sensor arrays: https://pmc.ncbi.nlm.nih.gov/articles/PMC6210432/
- ANN + least-squares wireless e-nose for CH4/CO: https://pmc.ncbi.nlm.nih.gov/articles/PMC5982671/
- Pattern recognition review for cross-sensitive chemiresistive arrays: https://pmc.ncbi.nlm.nih.gov/articles/PMC11324646/
- Exact eight-MQ-family electronic-nose example: https://www.sciencedirect.com/science/article/pii/S0168169918310366
