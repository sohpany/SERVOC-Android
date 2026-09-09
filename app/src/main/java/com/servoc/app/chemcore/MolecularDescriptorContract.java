package com.servoc.app.chemcore;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stable molecular descriptor vocabulary for ChemCore v1.
 *
 * The contract separates descriptors expected to have broad PubChem coverage from optional
 * descriptors that may improve a model but must not silently cause compounds to disappear.
 * Model artifacts must still record the exact descriptor subset they actually used.
 */
public final class MolecularDescriptorContract {
    private MolecularDescriptorContract() {}

    public static final String VERSION = "chemcore-molecular-descriptors/1";

    /** Broad-coverage, interpretable descriptors suitable for the initial model bench. */
    public static final String[] CORE = {
            "MolecularWeight",
            "TPSA",
            "HBondDonorCount",
            "HBondAcceptorCount",
            "RotatableBondCount",
            "HeavyAtomCount",
            "Charge",
            "Complexity"
    };

    /** Useful but not required for every compound in the first complete matrix. */
    public static final String[] OPTIONAL = {
            "XLogP",
            "ExactMass",
            "MonoisotopicMass",
            "CovalentUnitCount",
            "DefinedAtomStereoCount",
            "UndefinedAtomStereoCount",
            "DefinedBondStereoCount",
            "UndefinedBondStereoCount"
    };

    public static final class Vector {
        public final Map<String, Double> values;
        public final Set<String> missingCore;
        public final Set<String> missingOptional;
        public final double coreCoverage;
        public final double optionalCoverage;

        Vector(Map<String, Double> values, Set<String> missingCore, Set<String> missingOptional) {
            this.values = Collections.unmodifiableMap(values);
            this.missingCore = Collections.unmodifiableSet(missingCore);
            this.missingOptional = Collections.unmodifiableSet(missingOptional);
            this.coreCoverage = 1.0 - ((double) missingCore.size() / CORE.length);
            this.optionalCoverage = 1.0 - ((double) missingOptional.size() / OPTIONAL.length);
        }

        /** Complete-case eligibility for the default core descriptor model only. */
        public boolean coreComplete() { return missingCore.isEmpty(); }
    }

    public static Vector from(Map<String, Double> source) {
        Map<String, Double> values = new LinkedHashMap<>();
        Set<String> missingCore = new LinkedHashSet<>();
        Set<String> missingOptional = new LinkedHashSet<>();
        collect(source, CORE, values, missingCore);
        collect(source, OPTIONAL, values, missingOptional);
        return new Vector(values, missingCore, missingOptional);
    }

    public static String[] defaultQsprDescriptors() {
        return Arrays.copyOf(CORE, CORE.length);
    }

    public static boolean recognized(String name) {
        if (name == null) return false;
        for (String key : CORE) if (key.equals(name)) return true;
        for (String key : OPTIONAL) if (key.equals(name)) return true;
        return false;
    }

    private static void collect(Map<String, Double> source, String[] names,
                                Map<String, Double> values, Set<String> missing) {
        for (String name : names) {
            Double value = source == null ? null : source.get(name);
            if (value == null || !Double.isFinite(value)) {
                missing.add(name);
            } else {
                values.put(name, value);
            }
        }
    }
}
