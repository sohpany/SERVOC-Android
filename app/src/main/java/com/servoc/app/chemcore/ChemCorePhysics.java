package com.servoc.app.chemcore;

/** Deterministic physical/thermodynamic transforms used as transparent ChemCore constraints. */
public final class ChemCorePhysics {
    private ChemCorePhysics() {}

    public static final double R_J_PER_MOL_K = 8.31446261815324;
    public static final double AIR_MOLAR_MASS_G_PER_MOL = 28.96546;

    public static double kelvin(double temperatureC) { return temperatureC + 273.15; }

    /** Ideal-gas density in kg/m^3 from molar mass g/mol, temperature C and pressure kPa. */
    public static double idealGasDensityKgM3(double molarMassGPerMol, double temperatureC, double pressureKPa) {
        double tk=kelvin(temperatureC);
        if(!(molarMassGPerMol>0)||!Double.isFinite(molarMassGPerMol))return Double.NaN;
        if(!(tk>0)||!(pressureKPa>0)||!Double.isFinite(pressureKPa))return Double.NaN;
        return (pressureKPa*1000.0)*(molarMassGPerMol/1000.0)/(R_J_PER_MOL_K*tk);
    }

    /** Relative ideal-gas density to dry air at the same T/P. */
    public static double relativeVaporDensityToAir(double molarMassGPerMol) {
        return molarMassGPerMol>0&&Double.isFinite(molarMassGPerMol)?molarMassGPerMol/AIR_MOLAR_MASS_G_PER_MOL:Double.NaN;
    }

    /** ppm(v/v) -> mg/m^3 at current T/P under ideal-gas approximation. */
    public static double ppmToMgM3(double ppm, double molarMassGPerMol, double temperatureC, double pressureKPa) {
        double tk=kelvin(temperatureC);
        if(!(ppm>=0)||!(molarMassGPerMol>0)||!(pressureKPa>0)||!(tk>0))return Double.NaN;
        return ppm*molarMassGPerMol*pressureKPa/(R_J_PER_MOL_K*tk);
    }

    /** mg/m^3 -> ppm(v/v) at current T/P under ideal-gas approximation. */
    public static double mgM3ToPpm(double mgM3, double molarMassGPerMol, double temperatureC, double pressureKPa) {
        double tk=kelvin(temperatureC);
        if(!(mgM3>=0)||!(molarMassGPerMol>0)||!(pressureKPa>0)||!(tk>0))return Double.NaN;
        return mgM3*R_J_PER_MOL_K*tk/(molarMassGPerMol*pressureKPa);
    }

    /** Maximum equilibrium vapor fraction in ppm from saturation vapor pressure and total pressure. */
    public static double saturationPpm(double vaporPressureKPa, double totalPressureKPa) {
        if(!(vaporPressureKPa>=0)||!(totalPressureKPa>0)||!Double.isFinite(vaporPressureKPa)||!Double.isFinite(totalPressureKPa))return Double.NaN;
        return 1_000_000.0*Math.min(1.0,vaporPressureKPa/totalPressureKPa);
    }

    /** Antoine form log10(P) = A - B/(T_C + C); returns P in the coefficient's declared pressure unit. */
    public static double antoinePressure(double temperatureC, double a, double b, double c) {
        double den=temperatureC+c;
        if(!Double.isFinite(temperatureC)||!Double.isFinite(a)||!Double.isFinite(b)||!Double.isFinite(c)||Math.abs(den)<1e-12)return Double.NaN;
        return Math.pow(10.0,a-b/den);
    }
}
