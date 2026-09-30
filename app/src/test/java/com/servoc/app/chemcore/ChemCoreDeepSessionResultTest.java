package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public final class ChemCoreDeepSessionResultTest {
    @Test public void recoveryIsAuditEvidenceNotAutomaticIdentityOrPpmEvidence()throws Exception{
        ChemCoreResultEnvelope.CandidateSummary c=new ChemCoreResultEnvelope.CandidateSummary(7,"X",.8,42,.1,1,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                100,100,100,0,true,Collections.emptyMap(),45,1,2,Collections.emptyList());
        ChemCoreResultEnvelope analytical=new ChemCoreResultEnvelope("P",ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                7L,"X",Double.NaN,Double.NaN,Double.NaN,.1,.8,.2,"profile",ChemCoreFeaturePipeline.SCHEMA_VERSION,"model","screening",Collections.singletonList(c));
        double[] t={0,1,2,3,4,5};double[][] p=new double[6][8],r=new double[6][8];for(int i=0;i<6;i++)for(int j=0;j<8;j++){p[i][j]=1+.1*i;r[i][j]=1.5-.08*i;}
        ChemCoreSessionDynamics.Result dynamics=ChemCoreSessionDynamics.compute("P","R",t,p,null,t,r,null);
        ChemCoreDeepSessionResult deep=new ChemCoreDeepSessionResult(analytical,dynamics,123);
        JSONObject q=deep.toJson();JSONObject a=q.getJSONObject("analytical_result");
        assertEquals(45.0,a.getDouble("analytical_confidence_pct"),1e-9);assertFalse(a.getBoolean("ppm_reportable"));
        assertFalse(q.getBoolean("recovery_changes_identity_confidence"));assertFalse(q.getBoolean("recovery_unlocks_reportable_ppm"));assertEquals(123,q.getLong("analysis_elapsed_ms"));
    }
}
