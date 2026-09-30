package com.servoc.app.chemical;

import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.reference.ReferenceArtifactBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only material readiness projection used by the developer experiment matrix.
 *
 * The local database is the activated snapshot of the central HazRob scientific library plus
 * device-local approved experiments. Device-local R0 is deliberately excluded: R0 is sensor/device
 * calibration state and must never be promoted to a material-library status.
 *
 * READY quantitative status is deliberately stricter than "some ppm evidence": only an active
 * curve explicitly qualified as HR069_M4 may produce READY/M4. External or transferred
 * concentration evidence remains PARTIAL.
 */
public final class ChemicalExperimentStatusRepository {
    public enum Level { NONE, PARTIAL, READY }

    public static final class Row {
        public final ChemicalRepository.Chemical chemical;
        public final Level fingerprint, ppm;
        public final boolean m4, tested;
        public final String evidenceRevision;

        Row(ChemicalRepository.Chemical chemical, Level fingerprint, Level ppm,
            boolean m4, boolean tested, String evidenceRevision) {
            this.chemical=chemical;this.fingerprint=fingerprint;this.ppm=ppm;
            this.m4=m4;this.tested=tested;this.evidenceRevision=evidenceRevision==null?"":evidenceRevision;
        }

        public String fingerprintMark(){return mark(fingerprint);}
        public String ppmMark(){return mark(ppm);}
        public String m4Mark(){return m4?"✓":"—";}
        public int priority(){
            if(fingerprint==Level.NONE)return 0;
            if(ppm==Level.NONE)return 1;
            if(ppm==Level.PARTIAL&&!m4)return 2;
            if(!m4)return 3;
            return 4;
        }
        private static String mark(Level x){return x==Level.READY?"✓":x==Level.PARTIAL?"◐":"—";}
    }

    private final ExperimentsDb db;
    private final ChemicalRepository chemicals;

    public ChemicalExperimentStatusRepository(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        this.db=db;this.chemicals=new ChemicalRepository(db);
    }

    public List<Row> search(String query,int limit){
        List<ChemicalRepository.Chemical> source=chemicals.search(query,Math.max(1,Math.min(limit,5000)));
        List<Row> out=new ArrayList<>(source.size());
        String revision=libraryRevision();
        for(ChemicalRepository.Chemical c:source)out.add(status(c,revision));
        return out;
    }

    public Row status(ChemicalRepository.Chemical c){return status(c,libraryRevision());}

    private Row status(ChemicalRepository.Chemical c,String revision){
        if(c==null)return null;

        boolean localFingerprint=exists("SELECT 1 FROM reference_artifacts WHERE chemical_uid=? AND kind='fingerprint' AND active=1 LIMIT 1",c.uid);
        boolean acceptedCentralFingerprint=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' AND (lower(evidence_type) LIKE '%fingerprint%' OR lower(evidence_type) LIKE '%identity%') AND (upper(status) LIKE 'ACCEPTED_IDENTITY_FINGERPRINT%' OR upper(status) LIKE 'VERIFIED%FINGERPRINT%') LIMIT 1",c.uid);
        boolean transferFingerprint=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' AND (lower(evidence_type) LIKE '%fingerprint%' OR lower(evidence_type) LIKE '%identity%') LIMIT 1",c.uid);
        Level fingerprint=(localFingerprint||acceptedCentralFingerprint)?Level.READY:transferFingerprint?Level.PARTIAL:Level.NONE;

        boolean anyActiveCurve=false,strictM4=false;
        if(c.legacyId!=null){
            anyActiveCurve=exists("SELECT 1 FROM calibration_curves WHERE chemical_id=? AND active=1 LIMIT 1",c.legacyId.longValue());
            strictM4=exists("SELECT 1 FROM calibration_curves WHERE chemical_id=? AND active=1 AND source_note LIKE '"+ReferenceArtifactBuilder.HR069_M4_SOURCE_PREFIX+"%' LIMIT 1",c.legacyId.longValue());
        }
        boolean transferPpm=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' AND ((min_ppm IS NOT NULL AND min_ppm>=0) OR (max_ppm IS NOT NULL AND max_ppm>0)) LIMIT 1",c.uid);
        Level ppm=strictM4?Level.READY:(anyActiveCurve||transferPpm)?Level.PARTIAL:Level.NONE;

        boolean centralEvidence=exists("SELECT 1 FROM sensor_evidence WHERE chemical_uid=? AND status NOT LIKE 'REJECT%' LIMIT 1",c.uid);
        boolean tested=centralEvidence
                ||exists("SELECT 1 FROM experiments WHERE chemical_uid=? LIMIT 1",c.uid)
                ||exists("SELECT 1 FROM reference_artifacts WHERE chemical_uid=? LIMIT 1",c.uid);
        return new Row(c,fingerprint,ppm,strictM4,tested,revision);
    }

    /** Revision shown beside the matrix so the developer can verify which active central snapshot drives status. */
    public String libraryRevision(){
        String version=db.getMeta("chemcore_library_version","");
        String evidence=db.getMeta("chemcore_evidence_revision","");
        String r=(version==null?"":version.trim());
        String e=(evidence==null?"":evidence.trim());
        if(!r.isEmpty()&&!e.isEmpty())r=r+" / "+e;
        else if(r.isEmpty())r=e;
        if(r.isEmpty())r=db.getMeta("chemcore_molecular_seed_sha256","");
        if(r==null)r="";r=r.trim();return r.length()<=28?r:r.substring(0,28)+"…";
    }

    public int countFingerprintMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE uid IN (SELECT chemical_uid FROM reference_artifacts WHERE kind='fingerprint' AND active=1) OR uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%' AND (lower(evidence_type) LIKE '%fingerprint%' OR lower(evidence_type) LIKE '%identity%'))");}
    public int countPpmMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE legacy_id IN (SELECT chemical_id FROM calibration_curves WHERE active=1) OR uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%' AND ((min_ppm IS NOT NULL AND min_ppm>=0) OR (max_ppm IS NOT NULL AND max_ppm>0)))");}
    public int countM4Materials(){return scalar("SELECT COUNT(DISTINCT chemical_id) FROM calibration_curves WHERE active=1 AND source_note LIKE '"+ReferenceArtifactBuilder.HR069_M4_SOURCE_PREFIX+"%'");}
    public int countTestedMaterials(){return scalar("SELECT COUNT(DISTINCT uid) FROM chemicals WHERE uid IN (SELECT chemical_uid FROM experiments WHERE chemical_uid IS NOT NULL AND chemical_uid>0) OR uid IN (SELECT chemical_uid FROM sensor_evidence WHERE chemical_uid IS NOT NULL AND status NOT LIKE 'REJECT%')");}

    private boolean exists(String sql,long value){
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(value)})){return c.moveToFirst();}
        catch(Exception ignored){return false;}
    }
    private int scalar(String sql){
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,null)){return c.moveToFirst()?c.getInt(0):0;}
        catch(Exception ignored){return 0;}
    }

    public static String displayName(ChemicalRepository.Chemical c){
        if(c==null)return "";String name=c.arabicName!=null&&!c.arabicName.trim().isEmpty()?c.arabicName:c.name;
        StringBuilder s=new StringBuilder(name==null?"":name.trim());
        if(c.cas!=null&&!c.cas.trim().isEmpty())s.append(" • CAS ").append(c.cas.trim());
        return s.toString();
    }

    public static int compareText(String a,String b){
        return (a==null?"":a.toLowerCase(Locale.ROOT)).compareTo(b==null?"":b.toLowerCase(Locale.ROOT));
    }
}
