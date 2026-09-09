package com.servoc.app.chemcore;

import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a deterministic molecular descriptor matrix from the local provenance database.
 * Missing values are never silently imputed: rows are either complete for the requested
 * descriptor subset or retained as explicit exclusions for coverage reporting.
 */
public final class MolecularDescriptorMatrixBuilder {
    private MolecularDescriptorMatrixBuilder() {}

    public static final String SCHEMA = "chemcore-descriptor-matrix/1";
    public static final String QSPR_INELIGIBLE_IDENTITY = "QSPR_INELIGIBLE_IDENTITY";

    private static final class Identity {
        final long uid;final String cas,nameEn;
        Identity(long uid,String cas,String nameEn){this.uid=uid;this.cas=cas==null?"":cas;this.nameEn=nameEn==null?"":nameEn;}
    }

    public static final class Exclusion {
        public final long chemicalUid;
        public final List<String> missingDescriptors;
        Exclusion(long chemicalUid,List<String> missingDescriptors){
            this.chemicalUid=chemicalUid;
            this.missingDescriptors=Collections.unmodifiableList(missingDescriptors);
        }
    }

    public static final class Standardizer {
        public final String[] descriptorNames;
        public final double[] mean;
        public final double[] standardDeviation;

        Standardizer(String[] descriptorNames,double[] mean,double[] standardDeviation){
            this.descriptorNames=Arrays.copyOf(descriptorNames,descriptorNames.length);
            this.mean=Arrays.copyOf(mean,mean.length);
            this.standardDeviation=Arrays.copyOf(standardDeviation,standardDeviation.length);
        }

        public double[] transform(double[] raw){
            if(raw==null||raw.length!=mean.length)throw new IllegalArgumentException("descriptor dimension mismatch");
            double[] out=new double[raw.length];
            for(int i=0;i<raw.length;i++){
                if(!Double.isFinite(raw[i]))throw new IllegalArgumentException("non-finite descriptor at column "+i);
                out[i]=(raw[i]-mean[i])/standardDeviation[i];
            }
            return out;
        }
    }

    public static final class Matrix {
        public final String schema;
        public final String descriptorVersion;
        public final String[] descriptorNames;
        public final long[] chemicalUids;
        public final double[][] raw;
        public final double[][] standardized;
        public final Standardizer standardizer;
        public final List<Exclusion> exclusions;
        public final int candidateCount;

        Matrix(String descriptorVersion,String[] descriptorNames,long[] chemicalUids,double[][] raw,
               double[][] standardized,Standardizer standardizer,List<Exclusion> exclusions,int candidateCount){
            this.schema=SCHEMA;this.descriptorVersion=descriptorVersion;
            this.descriptorNames=Arrays.copyOf(descriptorNames,descriptorNames.length);
            this.chemicalUids=Arrays.copyOf(chemicalUids,chemicalUids.length);this.raw=raw;this.standardized=standardized;
            this.standardizer=standardizer;this.exclusions=Collections.unmodifiableList(exclusions);this.candidateCount=candidateCount;
        }

        public double completeCaseCoverage(){return candidateCount>0?(double)chemicalUids.length/candidateCount:0.0;}
    }

    public static Matrix build(ExperimentsDb db,String[] requestedDescriptors){
        if(db==null)throw new IllegalArgumentException("database required");
        String[] descriptors=normalize(requestedDescriptors);
        if(descriptors.length==0)throw new IllegalArgumentException("at least one descriptor required");
        MolecularIntelligenceRepository repo=new MolecularIntelligenceRepository(db);
        ChemCoreIdentityCurationRepository curation=new ChemCoreIdentityCurationRepository(db);
        List<Identity> identities=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid,cas,name_en FROM chemicals ORDER BY uid",null)){
            while(c.moveToNext())identities.add(new Identity(c.getLong(0),c.getString(1),c.getString(2)));
        }
        List<Long> completeIds=new ArrayList<>();List<double[]> rows=new ArrayList<>();List<Exclusion> exclusions=new ArrayList<>();
        for(Identity identity:identities){
            long uid=identity.uid;
            // Curated mixtures, materials, isomer groups, occupational categories, and unresolved
            // pure compounds are excluded explicitly even if future/manual properties are present.
            // This prevents a non-single-molecule identity from leaking into a molecule-QSPR matrix.
            if(!curation.molecularQsprEligible(identity.cas,identity.nameEn)){
                exclusions.add(new Exclusion(uid,Collections.singletonList(QSPR_INELIGIBLE_IDENTITY)));
                continue;
            }
            double[] row=new double[descriptors.length];List<String> missing=new ArrayList<>();
            for(int j=0;j<descriptors.length;j++){
                Double v=repo.latestNumeric(uid,descriptors[j]);
                row[j]=v==null?Double.NaN:v;
                if(v==null||!Double.isFinite(v))missing.add(descriptors[j]);
            }
            if(missing.isEmpty()){completeIds.add(uid);rows.add(row);}else exclusions.add(new Exclusion(uid,missing));
        }
        if(rows.size()<2)throw new IllegalStateException("fewer than two complete descriptor rows");
        double[][] raw=rows.toArray(new double[0][]);Standardizer std=fitStandardizer(descriptors,raw);
        double[][] standardized=new double[raw.length][];for(int i=0;i<raw.length;i++)standardized[i]=std.transform(raw[i]);
        long[] uids=new long[completeIds.size()];for(int i=0;i<uids.length;i++)uids[i]=completeIds.get(i);
        return new Matrix(MolecularDescriptorContract.VERSION,descriptors,uids,raw,standardized,std,exclusions,identities.size());
    }

    public static Standardizer fitStandardizer(String[] descriptorNames,double[][] rows){
        String[] names=normalize(descriptorNames);
        if(rows==null||rows.length<2)throw new IllegalArgumentException("at least two rows required");
        int p=names.length;if(p==0)throw new IllegalArgumentException("descriptor names required");
        double[] mean=new double[p],sd=new double[p];
        for(double[] row:rows){
            if(row==null||row.length!=p)throw new IllegalArgumentException("ragged descriptor matrix");
            for(int j=0;j<p;j++){if(!Double.isFinite(row[j]))throw new IllegalArgumentException("non-finite training descriptor");mean[j]+=row[j];}
        }
        for(int j=0;j<p;j++)mean[j]/=rows.length;
        for(double[] row:rows)for(int j=0;j<p;j++){double d=row[j]-mean[j];sd[j]+=d*d;}
        for(int j=0;j<p;j++){
            sd[j]=Math.sqrt(sd[j]/Math.max(1,rows.length-1));
            // A zero-variance descriptor conveys no scaling information. Keep the column numerically
            // stable at zero after centering; downstream feature selection may remove it explicitly.
            if(!Double.isFinite(sd[j])||sd[j]<1e-12)sd[j]=1.0;
        }
        return new Standardizer(names,mean,sd);
    }

    private static String[] normalize(String[] input){
        if(input==null)return new String[0];Map<String,String> ordered=new LinkedHashMap<>();
        for(String x:input){if(x==null)continue;String k=x.trim();if(k.isEmpty())continue;if(!MolecularDescriptorContract.recognized(k))throw new IllegalArgumentException("unrecognized descriptor: "+k);ordered.put(k,k);}
        return ordered.values().toArray(new String[0]);
    }
}
