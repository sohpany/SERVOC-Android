package com.servoc.app.models;

/**
 * Exact ordered model catalogue from MQDataScience v6.0.0 PredictData.py.
 * Registration means HazRob knows/provenances the model. Execution availability is tracked separately.
 */
public final class MqDataScience76Catalog {
    private MqDataScience76Catalog() {}

    public static final String SOURCE="abcdaaaaaaaaa/MQDataScience";
    public static final String VERSION="6.0.0";
    public static final String PINNED_COMMIT="e88f876140587a708ee84a4d4d3acbd5720274ee";

    public static final String[] NAMES={
            "Multi-Variable Regression (Time + Temperature + Relative Humidity)",
            "Multi-Variable Regression (Temperature + Relative Humidity)",
            "Multi-Variable Regression (Time + Temperature)",
            "Multi-Variable Regression (Time + Relative Humidity)",
            "Full Quadratic Surface Regression",
            "Multi-Variable Quadratic Regression",
            "Temperature × Humidity Interaction Regression",
            "Temperature Linear Regression",
            "Humidity Linear Regression",
            "Temperature Inverse Regression",
            "Humidity Inverse Regression",
            "Linear Regression",
            "Weighted Linear Regression",
            "Robust Regression (Huber)",
            "Theil–Sen Regression",
            "RANSAC Regression",
            "Ridge Regression",
            "Lasso Regression",
            "Elastic Net Regression",
            "Bayesian Linear Regression",
            "Bayesian Polynomial Regression",
            "Orthogonal Distance Regression (ODR)",
            "Total Least Squares (TLS)",
            "Deming Regression",
            "2nd Degree Polynomial Regression",
            "3rd Degree Polynomial Regression",
            "4th Degree Polynomial Regression",
            "Exponential Regression",
            "Logarithmic Regression",
            "Power Regression",
            "Gaussian Regression",
            "Rational Function Regression",
            "Hyperbolic Regression",
            "Reciprocal Polynomial Regression",
            "Padé Approximation Regression",
            "Piecewise Regression",
            "Piecewise Linear Regression",
            "Piecewise Polynomial Regression",
            "LOESS (LOWESS) Regression",
            "Local Polynomial Regression",
            "Kernel Regression (Nadaraya–Watson)",
            "Generalized Additive Model (GAM)",
            "Quantile Regression",
            "Cubic Spline Regression",
            "Natural Cubic Spline Regression",
            "B-Spline Regression",
            "Smoothing Spline Regression",
            "Penalized Spline Regression (P-Spline)",
            "Thin Plate Spline Regression",
            "2nd Degree Fourier Series",
            "Sinusoidal Regression",
            "Moving Average Regression",
            "Exponential Smoothing",
            "Holt Linear Trend",
            "Holt-Winters Method",
            "Autoregressive (AR) Model",
            "Autoregressive Moving Average (ARMA)",
            "Autoregressive Integrated Moving Average (ARIMA)",
            "Seasonal ARIMA (SARIMA)",
            "Error–Trend–Seasonality (ETS)",
            "Dynamic Linear Model (DLM)",
            "State Space Regression",
            "Kalman Filter Regression",
            "Support Vector Regression (SVR)",
            "Decision Tree Regression",
            "Random Forest Regression",
            "Extra Trees Regression",
            "Gradient Boosting Regression",
            "AdaBoost Regression",
            "Extreme Gradient Boosting (XGBoost)",
            "Light Gradient Boosting Machine (LightGBM)",
            "CatBoost Regression",
            "k-Nearest Neighbors Regression (kNN)",
            "Gaussian Process Regression (GPR)",
            "Multi-Layer Perceptron (MLP)",
            "Radial Basis Function Neural Network (RBF Network)"
    };

    public static final int COUNT=NAMES.length;

    public enum Runtime { ANDROID_NATIVE, REFERENCE_REGISTERED }

    /**
     * Models with a current pure-Java HazRob implementation or direct equivalent.
     * The remaining source models stay registered and are never falsely reported as executed.
     */
    public static Runtime runtime(int index){
        switch(index){
            case 11: // Linear
            case 16: // Ridge
            case 24: // quadratic
            case 25: // cubic
            case 26: // quartic
            case 28: // logarithmic family support
            case 29: // power
            case 51: // moving average
            case 62: // Kalman-like state processing hooks
                return Runtime.ANDROID_NATIVE;
            default:return Runtime.REFERENCE_REGISTERED;
        }
    }

    public static int androidNativeCount(){
        int n=0;for(int i=0;i<COUNT;i++)if(runtime(i)==Runtime.ANDROID_NATIVE)n++;return n;
    }
}
